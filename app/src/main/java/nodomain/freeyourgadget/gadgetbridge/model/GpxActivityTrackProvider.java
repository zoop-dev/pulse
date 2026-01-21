/*  Copyright (C) 2026 José Rebelo

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrack;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackSegment;

public class GpxActivityTrackProvider implements ActivityTrackProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GpxActivityTrackProvider.class);

    @Nullable
    @Override
    public ActivityTrack getActivityTrack(@NonNull final BaseActivitySummary summary) {
        final File file;

        final File gpxTrack = FileUtils.tryFixPath(summary.getGpxTrack());
        if (gpxTrack == null) {
            LOG.debug("Gpx file not found in {}", summary.getGpxTrack());

            // Just in case, check whether the raw details is a gpx
            final File rawDetails = FileUtils.tryFixPath(summary.getRawDetailsPath());
            if (rawDetails == null || !rawDetails.getAbsolutePath().endsWith(".gpx")) {
                return null;
            }

            file = rawDetails;
        } else {
            file = gpxTrack;
        }

        LOG.debug("Loading gpx file from {}", file);

        final GpxFile gpxFile;
        try (InputStream inputStream = new FileInputStream(file)) {
            final GpxParser gpxParser = new GpxParser(inputStream);
            gpxFile = gpxParser.getGpxFile();
        } catch (final IOException e) {
            LOG.error("Failed to read gpx file", e);
            return null;
        } catch (final GpxParseException e) {
            LOG.error("Failed to parse gpx file", e);
            return null;
        }

        final ActivityTrack activityTrack = new ActivityTrack();
        activityTrack.setName(gpxFile.getName());
        for (GpxTrack track : gpxFile.getTracks()) {
            for (GpxTrackSegment trackSegment : track.getTrackSegments()) {
                activityTrack.startNewSegment();
                for (GpxTrackPoint trackPoint : trackSegment.getTrackPoints()) {
                    activityTrack.addTrackPoint(trackPoint.toActivityPoint());
                }
            }
        }
        return activityTrack;
    }
}
