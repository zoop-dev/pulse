/*  Copyright (C) 2026 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.exception.FitParseException;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class FitActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(FitActivityTrackProvider.class);

    @Nullable
    @Override
    public ActivityTrack getActivityTrack(@NonNull final BaseActivitySummary summary) {
        final File file = FileUtils.tryFixPath(summary.getRawDetailsPath());
        if (file == null) {
            LOG.debug("Fit file no found in {}", summary.getRawDetailsPath());
            return null;
        }

        LOG.debug("Loading activity track from {}", file);

        final FitFile fitFile;
        try {
            fitFile = FitFile.parseIncoming(file);
        } catch (final IOException e) {
            LOG.error("Failed to read fit file", e);
            return null;
        } catch (final FitParseException e) {
            LOG.error("Failed to parse fit file", e);
            return null;
        }

        final ActivityTrack activityTrack = new ActivityTrack();
        activityTrack.setName(summary.getName());

        final Iterator<FitRecord> records = fitFile.getRecords().stream()
                .filter(r -> r instanceof FitRecord)
                .map(r -> (FitRecord) r)
                .iterator();

        final Iterator<Long> lapStarts = fitFile.getRecords().stream()
                .filter(record -> record instanceof FitLap)
                .map(record -> (FitLap) record)
                .filter(lap -> {
                    Integer event = lap.getEvent();
                    if (event != null && event != 9) {
                        return false;
                    }
                    Integer eventType = lap.getEventType();
                    return (eventType == null || eventType == 1);
                })
                .map(FitLap::getStartTime)
                .filter(Objects::nonNull)
                .iterator();

        // skip first lap start
        if(lapStarts.hasNext()){
            lapStarts.next();
        }

        long nextLapStart = (lapStarts.hasNext() ? lapStarts.next() : Long.MAX_VALUE);
        while (records.hasNext()) {
            FitRecord record = records.next();
            if (record.getComputedTimestamp() >= nextLapStart) {
                activityTrack.startNewSegment();
                nextLapStart = (lapStarts.hasNext() ? lapStarts.next() : Long.MAX_VALUE);
            }
            activityTrack.addTrackPoint(record.toActivityPoint());
        }

        return activityTrack;
    }
}
