/*  Copyright (C) 2019-2024 Andreas Shimokawa, José Rebelo

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

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFetcher;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FetchDebugLogsOperation extends AbstractFetchOperation {
    private static final Logger LOG = LoggerFactory.getLogger(FetchDebugLogsOperation.class);

    private FileOutputStream logOutputStream;

    public FetchDebugLogsOperation(final HuamiFetcher fetcher) {
        super(fetcher, HuamiFetchDataType.DEBUG_LOGS);
    }

    @StringRes
    @Override
    public int taskDescription() {
        return R.string.busy_task_fetch_debug_logs;
    }

    @Override
    protected void startFetching() {
        File dir;
        try {
            dir = FileUtils.getExternalFilesDir();
        } catch (IOException e) {
            return;
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        final String filename = "huamidebug_" + dateFormat.format(new Date()) + ".log";

        File outputFile = new File(dir, filename);
        try {
            logOutputStream = new FileOutputStream(outputFile);
        } catch (IOException e) {
            LOG.warn("could not create file " + outputFile, e);
            return;
        }
        final GregorianCalendar sinceWhen = getLastSuccessfulSyncTime();
        startFetching(HuamiFetchDataType.DEBUG_LOGS.getCode(), sinceWhen);
    }

    @Override
    protected String getLastSyncTimeKey() {
        return "lastDebugTimeMillis";
    }

    @Override
    protected boolean processBufferedData() {
        LOG.info("{} data has finished", getName());
        try {
            logOutputStream.close();
            logOutputStream = null;
        } catch (final IOException e) {
            LOG.error("could not close output stream", e);
            return false;
        }

        return true;
    }

    @Override
    protected boolean validChecksum(int crc32) {
        // TODO actually check it?
        LOG.warn("Checksum not implemented for debug logs, assuming it's valid");
        return true;
    }

    @Override
    protected void bufferActivityData(@NonNull byte[] value) {
        try {
            logOutputStream.write(value, 1, value.length - 1);
        } catch (final IOException e) {
            LOG.error("could not write to output stream", e);
        }
    }

    @Override
    protected GregorianCalendar getLastSuccessfulSyncTime() {
        // Avoid going too further back - some devices have so many logs that it will take an
        // incredibly long time to sync
        final GregorianCalendar lastHour = BLETypeConversions.createCalendar();
        lastHour.add(Calendar.HOUR, -1);

        final long timeStampMillis = GBApplication.getDeviceSpecificSharedPrefs(fetcher.getDevice().getAddress()).getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0 && timeStampMillis > lastHour.getTimeInMillis()) {
            GregorianCalendar calendar = BLETypeConversions.createCalendar();
            calendar.setTimeInMillis(timeStampMillis);
            return calendar;
        }

        return lastHour;
    }
}
