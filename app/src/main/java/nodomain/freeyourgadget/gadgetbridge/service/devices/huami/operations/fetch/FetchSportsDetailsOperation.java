/*  Copyright (C) 2018-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Oleg Vasilev, Sebastian Krey, Your Name

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.fetch;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.export.AutoGpxExporter;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFetcher;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiActivityDetailsParser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

/**
 * An operation that fetches activity data. For every fetch, a new operation must
 * be created, i.e. an operation may not be reused for multiple fetches.
 */
public class FetchSportsDetailsOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportsDetailsOperation.class);
    private final AbstractHuamiActivityDetailsParser detailsParser;
    private final BaseActivitySummary summary;
    private final String lastSyncTimeKey;

    FetchSportsDetailsOperation(@NonNull final BaseActivitySummary summary,
                                @NonNull final AbstractHuamiActivityDetailsParser detailsParser,
                                @NonNull final HuamiFetcher fetcher,
                                @NonNull final String lastSyncTimeKey,
                                int fetchCount) {
        super(fetcher, HuamiFetchDataType.SPORTS_DETAILS);
        this.summary = summary;
        this.detailsParser = detailsParser;
        this.lastSyncTimeKey = lastSyncTimeKey;
        this.fetchCount = fetchCount;
    }

    @StringRes
    @Override
    public int taskDescription() {
        return R.string.busy_task_fetch_sports_details;
    }

    @Override
    protected void startFetching() {
        LOG.info("start {}", getName());
        final GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(HuamiFetchDataType.SPORTS_DETAILS.getCode(), sinceWhen);
    }

    @Override
    protected boolean processBufferedData() {
        LOG.info("{} has finished round {}", getName(), fetchCount);

        if (buffer.size() == 0) {
            LOG.warn("Buffer is empty");
            return false;
        }

        if (detailsParser instanceof HuamiActivityDetailsParser) {
            ((HuamiActivityDetailsParser) detailsParser).setSkipCounterByte(false); // is already stripped
        }

        // Start by persisting the raw bytes right away - they can always be re-processed later if needed
        try {
            final String rawBytesPath = saveRawBytes();
            if (rawBytesPath != null) {
                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    summary.setRawDetailsPath(rawBytesPath);
                    dbHandler.getDaoSession().getBaseActivitySummaryDao().update(summary);
                }
            }
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving raw bytes: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        try {
            final ActivityTrack track = detailsParser.parse(buffer.toByteArray());
            AutoGpxExporter.doExport(getContext(), getDevice(), summary, track);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving activity details: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
            // #4549 - we do not return false here, since this might cause the same activity to be fetched over and over again
            // the raw details are persisted above, we can always re-process if needed
        }

        // Always increment the sync timestamp on success, even if we did not get data
        final GregorianCalendar endTime = BLETypeConversions.createCalendar();
        endTime.setTime(summary.getEndTime());
        saveLastSyncTimestamp(endTime);

        if (needsAnotherFetch(endTime)) {
            final FetchSportsSummaryOperation nextOperation = new FetchSportsSummaryOperation(fetcher, fetchCount);
            fetcher.getFetchOperationQueue().add(0, nextOperation);
        }

        return true;
    }

    private boolean needsAnotherFetch(GregorianCalendar lastSyncTimestamp) {
        // We have 2 operations per fetch round: summary + details
        if (fetchCount > 20) {
            LOG.warn("Already have {} fetch rounds, not doing another one.", fetchCount/ 2);
            return false;
        }

        if (lastSyncTimestamp.getTimeInMillis() > System.currentTimeMillis()) {
            LOG.warn("Not doing another fetch since last synced timestamp is in the future: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
            return false;
        }

        LOG.info("Doing another fetch since last sync timestamp is still too old: {}", DateTimeUtils.formatDateTime(lastSyncTimestamp.getTime()));
        return true;
    }

    @Override
    protected String getLastSyncTimeKey() {
        return lastSyncTimeKey;
    }

    @Override
    protected GregorianCalendar getLastSuccessfulSyncTime() {
        final GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.setTime(summary.getStartTime());
        return calendar;
    }

    private String saveRawBytes() {
        final SimpleDateFormat SDF_YEAR = new SimpleDateFormat("yyyy", Locale.ROOT);

        final StringBuilder sb = new StringBuilder();

        sb.append("rawDetails");
        sb.append(File.separator);
        sb.append(SDF_YEAR.format(summary.getStartTime()));
        sb.append(File.separator);
        sb.append(FileUtils.makeValidFileName(String.format("%s.bin", DateTimeUtils.formatIso8601(summary.getStartTime()))));

        try {
            final File writableExportDirectory = getDevice().getDeviceCoordinator().getWritableExportDirectory(getDevice(), true);
            final File targetFile = new File(writableExportDirectory, sb.toString());
            final File parent = targetFile.getParentFile();
            if (parent != null) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            final FileOutputStream outputStream = new FileOutputStream(targetFile);
            outputStream.write(buffer.toByteArray());
            outputStream.close();
            return targetFile.getAbsolutePath();
        } catch (final IOException e) {
            LOG.error("Failed to save raw bytes", e);
        }

        return null;
    }
}
