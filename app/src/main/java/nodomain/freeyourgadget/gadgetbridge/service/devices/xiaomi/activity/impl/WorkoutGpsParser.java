/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.export.AutoFitExporter;
import nodomain.freeyourgadget.gadgetbridge.export.AutoGpxExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileFetcher;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityTrackProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class WorkoutGpsParser extends XiaomiActivityParser {
    private static final Logger LOG = LoggerFactory.getLogger(WorkoutGpsParser.class);

    @Nullable
    public ActivityTrack getActivityTrack(final XiaomiActivityFileId fileId, final byte[] bytes) {
        final int version = fileId.getVersion();
        final int headerSize;
        final int sampleSize;
        // GPS sample wire format (band firmware versions, deduced from captured tracks):
        //   v1 (12B): time (4B int32 seconds) + longitude (4B float) + latitude (4B float)
        //   v2 (18B): v1 fields + accuracy (4B float, meters) + packed speed/source (2B u16:
        //             high 12 bits = 0.1 m/s, low 4 bits = source flag)
        //   v3 (26B): v2 fields + altitude (4B float) + hdop (4B float, dimensionless DOP)
        switch (version) {
            case 1:
                headerSize = 1;
                sampleSize = 12;
                break;
            case 2:
                headerSize = 1;
                sampleSize = 18;
                break;
            case 3:
                headerSize = 1;
                sampleSize = 26;
                break;
            default:
                LOG.warn("Unable to parse workout gps version {}", fileId.getVersion());
                return null;
        }

        final ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buf.limit(buf.limit() - 4); // discard crc at the end
        buf.get(new byte[7]); // skip fileId bytes
        final byte fileIdPadding = buf.get();
        if (fileIdPadding != 0) {
            LOG.warn("Expected 0 padding after fileId, got {} - parsing might fail", fileIdPadding);
        }
        final byte[] header = new byte[headerSize];
        buf.get(header);

        LOG.debug("Workout gps Header: {}", GB.hexdump(header));

        if ((buf.limit() - buf.position()) % sampleSize != 0) {
            LOG.warn("Remaining data in the buffer is not a multiple of {}", sampleSize);
        }

        final ActivityTrack activityTrack = new ActivityTrack();

        // GPS V1 contains no speed data, therefore second while loop to avoid too many ifs within the loop
        if (version == 1) {
            while (buf.position() < buf.limit()) {
                final int ts = buf.getInt();
                final float longitude = buf.getFloat();
                final float latitude = buf.getFloat();

                final ActivityPoint ap = new ActivityPoint(new Date(ts * 1000L));
                ap.setLocation(new GPSCoordinate(longitude, latitude, 0));
                activityTrack.addTrackPoint(ap);
                LOG.trace("ActivityPoint V1: ts={} lon={} lat={}", ts, longitude, latitude);
            }
        } else if (version == 2) {
            while (buf.position() < buf.limit()) {
                final int ts = buf.getInt();
                final float longitude = buf.getFloat();
                final float latitude = buf.getFloat();
                // The 4-byte float at this offset is GPS accuracy in meters, not HDOP.
                // We store it via setHdop because Gadgetbridge's GPSCoordinate hdop slot
                // is documented as UNIT_METERS in ActivityPoint.Builder (existing convention).
                // Previous code divided by 4.8 — that scale was unfounded.
                final float accuracyMeters = buf.getFloat();
                // Speed packed: high 12 bits = 0.1 m/s, low 4 bits = source flag
                // (0 = phone GPS; other values = device-derived, not enumerated).
                final int speedRaw = buf.getShort() & 0xFFFF;
                final float speed = ((speedRaw & 0xFFF0) >> 4) / 10.0f;

                final ActivityPoint ap = new ActivityPoint(new Date(ts * 1000L));
                final GPSCoordinate gpsc = new GPSCoordinate(longitude, latitude);
                gpsc.setHdop(accuracyMeters);
                ap.setLocation(gpsc);
                ap.setSpeed(speed);

                activityTrack.addTrackPoint(ap);
                LOG.trace("ActivityPoint V2: ts={} lon={} lat={} acc(m)={} speed(m/s)={}",
                        ts, longitude, latitude, accuracyMeters, speed);
            }
        } else { // version == 3
            while (buf.position() < buf.limit()) {
                final int ts = buf.getInt();
                final float longitude = buf.getFloat();
                final float latitude = buf.getFloat();
                final float accuracyMeters = buf.getFloat(); // unused for now; v3 also has true hdop
                final int speedRaw = buf.getShort() & 0xFFFF;
                final float speed = ((speedRaw & 0xFFF0) >> 4) / 10.0f;
                final float altitude = buf.getFloat();
                final float hdop = buf.getFloat();

                final ActivityPoint ap = new ActivityPoint(new Date(ts * 1000L));
                final GPSCoordinate gpsc = new GPSCoordinate(longitude, latitude, altitude);
                gpsc.setHdop(hdop);
                ap.setLocation(gpsc);
                ap.setSpeed(speed);

                activityTrack.addTrackPoint(ap);
                LOG.trace("ActivityPoint V3: ts={} lon={} lat={} alt={} acc(m)={} hdop={} speed(m/s)={}",
                        ts, longitude, latitude, altitude, accuracyMeters, hdop, speed);
            }
        }

        return activityTrack;
    }

    @Override
    public boolean parse(final Context context, final GBDevice gbDevice, final XiaomiActivityFileId fileId, final byte[] bytes) {
        final ActivityTrack activityTrack = getActivityTrack(fileId, bytes);
        final boolean trackEmpty = activityTrack == null || activityTrack.getAllPoints().isEmpty();
        final boolean trackUnusable = trackEmpty
                || !XiaomiActivityTrackProvider.hasAnyNonNullIslandLocation(activityTrack);
        if (trackUnusable) {
            // No usable GPS track. 13-byte files are placeholders for indoor / no-fix workouts
            // (fileId + padding + 1 header byte + CRC); walking / outdoor running with no fix
            // instead emit records but with all coords 0,0 (Null Island). Either way log at INFO.
            if (bytes != null && bytes.length <= 16) {
                LOG.info("GPS_TRACK placeholder ({} bytes) for {} — likely indoor / no GPS fix",
                        bytes.length, fileId);
            } else if (!trackEmpty) {
                LOG.info("GPS_TRACK for {} contains only Null Island coords — treating as no fix",
                        fileId);
            }
            // Heal any summary previously flagged hasGps=true by a pre-fix parse of this same
            // GPS_TRACK: clear rawDetailsPath (only WorkoutGpsParser ever sets it for Xiaomi) and
            // null the cached summaryData JSON so XiaomiSimpleActivityParser re-derives hasGps=false.
            clearStaleGpsFlag(gbDevice, fileId);
            return false;
        }

        final BaseActivitySummary summary;
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            // Find the matching summary
            summary = findOrCreateBaseActivitySummary(session, device, user, fileId);

            // Set the info on the activity track
            activityTrack.setUser(user);
            activityTrack.setDevice(device);
            activityTrack.setName(ActivityKind.fromCode(summary.getActivityKind()).getLabel(context));

            // The file was already persisted by XiaomiActivityFileFetcher - just use the existing file
            final File rawBytesFile = XiaomiActivityFileFetcher.getRawFile(gbDevice, fileId);

            // Save the gpx file
            if (rawBytesFile != null) {
                summary.setRawDetailsPath(rawBytesFile.getAbsolutePath());
            }

            // Mark hasGps=true so the generic UI shows the GPS canvas. The summary parser
            // (XiaomiSimpleActivityParser) preserves this flag if it runs after this parser.
            final String prevSummaryDataJson = summary.getSummaryData();
            final ActivitySummaryData summaryData = prevSummaryDataJson != null
                    ? ActivitySummaryData.fromJson(prevSummaryDataJson)
                    : new ActivitySummaryData();
            if (summaryData != null) {
                summaryData.setHasGps(true);
                summary.setSummaryData(summaryData.toString());
            }

            session.getBaseActivitySummaryDao().insertOrReplace(summary);
        } catch (final Exception e) {
            GB.toast(context, "Error saving workout gps", Toast.LENGTH_LONG, GB.ERROR, e);
            return false;
        }

        // Patch HR / cadence / speed from the DETAILS file onto the GPS track before
        // auto-export. WorkoutGpsParser builds the track from GPS samples only; without this
        // the auto-exported GPX/FIT carry position + speed but no HR. The manual export path
        // (XiaomiActivityTrackProvider.getActivityTrack) already merges — this brings the
        // auto-export to parity. DETAILS is fetched before GPS_TRACK, so it is already persisted.
        XiaomiActivityTrackProvider.mergeDetailsForExport(
                gbDevice, activityTrack, fileId.getTimestamp().getTime() / 1000L);

        AutoGpxExporter.doExport(context, gbDevice, summary, activityTrack);
        AutoFitExporter.doExport(context, gbDevice, summary, activityTrack);

        return true;
    }

    private void clearStaleGpsFlag(final GBDevice gbDevice, final XiaomiActivityFileId fileId) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);
            final BaseActivitySummary summary = findOrCreateBaseActivitySummary(session, device, user, fileId);
            if (summary.getId() == null) {
                // findOrCreateBaseActivitySummary returned a fresh summary (no prior parse).
                // Nothing to heal — and we don't want to persist a placeholder row.
                return;
            }
            final String existingPath = summary.getRawDetailsPath();
            final String existingSummaryDataJson = summary.getSummaryData();
            if (existingPath == null && existingSummaryDataJson == null) {
                return;
            }
            // Only WorkoutGpsParser ever sets rawDetailsPath for Xiaomi, so any value here came
            // from a prior parse of this same GPS_TRACK.
            summary.setRawDetailsPath(null);
            // Null the cached summaryData JSON; XiaomiSimpleActivityParser rebuilds it from
            // rawSummaryData on demand and will now derive hasGps=false.
            summary.setSummaryData(null);
            session.getBaseActivitySummaryDao().update(summary);
            LOG.info("Cleared stale rawDetailsPath for {}", fileId);
        } catch (final Exception e) {
            LOG.warn("Failed to clear stale GPS flag for {}", fileId, e);
        }
    }
}
