/*  Copyright (C) 2026 Ingvar Stepanyan

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import androidx.annotation.Nullable;

import com.google.protobuf.ByteString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.export.GPXExporter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.ExploreSyncService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.Line;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDataReadyRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDataReadyResponse;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDataRequestOp;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigestReadResponse;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigestWriteRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigestWriteResponse;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LinePart;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineReadResponse;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineType;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.ReadStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.StartSyncStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.SyncFinishedStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.WriteStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification;

/**
 * Handler for the GDI Smart {@code ExploreSyncService} extension (field 22).
 *
 * Walks the watch's historical activity catalog: each completed
 * {@code LINE_TYPE_ACTIVITY} entry is fetched as a polyline, written as GPX,
 * and persisted as a {@link BaseActivitySummary} — the same surface FIT imports
 * use, so downstream consumers need no ExploreSync awareness.
 */
class ExploreSyncHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExploreSyncHandler.class);

    private final GarminSupport deviceSupport;

    /**
     * Drop session state on BT disconnect; in-flight POINTS pages won't match
     * the watch's offset on reconnect.
     */
    void onDisconnected() {
        disposeSession();
        LOG.info("ExploreSync state cleared on BT disconnect");
    }

    private void disposeSession() {
        if (state != null) {
            state.dispose();
            state = null;
        }
    }

    /**
     * Returns false if the DB is unavailable — caller decides whether to skip
     * an outgoing or reject an incoming StartSync.
     */
    private boolean replaceSession() {
        disposeSession();
        try {
            state = new SyncSession();
            return true;
        } catch (final GBException e) {
            LOG.warn("ExploreSync: failed to (re)create session — DB unavailable", e);
            return false;
        }
    }

    // Per-field "no value" sentinels: the values the watch ships when
    // a metric is unmeasured. Each constant is field-specific because
    // FIT's per-type max (see {@link
    // nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType})
    // leaks through after the proto pipeline applies the field's unit
    // scaling — blanket filtering would mask unrelated real values.

    /** Speeds, altitudes, temperatures — protobuf-pipeline convention
     *  (FIT's float32 NaN doesn't survive the wire). */
    private static final float SENTINEL_FLOAT = 1.0e25f;
    /** Distance, meters. FIT uint32 max cm divided by 100. */
    private static final float SENTINEL_DISTANCE_M = 0xFFFFFFFFL / 100f;
    /** Total ascent and descent, meters. FIT uint16 max widened to float. */
    private static final float SENTINEL_ASCENT_DESCENT_M = 0xFFFF;
    /** Calories, kcal. FIT uint16 max in a uint32 proto field. */
    private static final int SENTINEL_CALORIES_KCAL = 0xFFFF;
    /** Per-point HR and cadence, plus avg HR. FIT uint8 max widened to int. */
    private static final int SENTINEL_HR_CADENCE = 0xFF;

    private static final int PROTOCOL_VERSION = 2;

    /**
     * Stable per-app identity. The watch keys its saved_app_transaction_id
     * on this; sending a different one (or none) gets START_SYNC_REJECTED
     * and resets the cursor on the watch side.
     */
    private static final ByteString APP_UUID = ByteString.copyFrom(
            "Gadgetbridge GB!".getBytes(StandardCharsets.US_ASCII));

    /**
     * Flipped on the first SyncFinished(OK) after a FULL. Stays unset
     * across interrupted FULLs so the next session retries from scratch.
     */
    private static final String PREF_BASELINE_ESTABLISHED
            = "garmin_explore_sync_baseline_established";

    // Pre-built stateless ack responses.
    private static final ExploreSyncService LINE_DIGEST_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setLineDigestReadResponse(LineDigestReadResponse.newBuilder()
                            .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService LINE_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setLineReadResponse(LineReadResponse.newBuilder()
                            .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();

    // Collection / Waypoint stubs: the watch stalls if we don't ack
    // these, but we don't surface any of them — pre-built SUCCESS.
    private static final ExploreSyncService COLLECTION_LIST_WRITE_ACK
            = ExploreSyncService.newBuilder()
                    .setCollectionListWriteResponse(
                            GdiExploreSyncService.CollectionListWriteResponse.newBuilder()
                                    .setStatus(WriteStatus.WRITE_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService COLLECTION_DIGEST_WRITE_ACK
            = ExploreSyncService.newBuilder()
                    .setCollectionDigestWriteResponse(
                            GdiExploreSyncService.CollectionDigestWriteResponse.newBuilder()
                                    .setStatus(WriteStatus.WRITE_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService COLLECTION_DIGEST_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setCollectionDigestReadResponse(
                            GdiExploreSyncService.CollectionDigestReadResponse.newBuilder()
                                    .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService COLLECTION_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setCollectionReadResponse(
                            GdiExploreSyncService.CollectionReadResponse.newBuilder()
                                    .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService WAYPOINT_DIGEST_WRITE_ACK
            = ExploreSyncService.newBuilder()
                    .setWaypointDigestWriteResponse(
                            GdiExploreSyncService.WaypointDigestWriteResponse.newBuilder()
                                    .setStatus(WriteStatus.WRITE_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService WAYPOINT_DIGEST_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setWaypointDigestReadResponse(
                            GdiExploreSyncService.WaypointDigestReadResponse.newBuilder()
                                    .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService WAYPOINT_READ_ACK
            = ExploreSyncService.newBuilder()
                    .setWaypointReadResponse(
                            GdiExploreSyncService.WaypointReadResponse.newBuilder()
                                    .setStatus(ReadStatus.READ_STATUS_SUCCESS))
                    .build();
    private static final ExploreSyncService ACTIVE_LINE_DIGEST_WRITE_ACK
            = ExploreSyncService.newBuilder()
                    .setActiveLineDigestWriteResponse(
                            GdiExploreSyncService.ActiveLineDigestWriteResponse.newBuilder()
                                    .setStatus(WriteStatus.WRITE_STATUS_SUCCESS))
                    .build();
    /** Read-only client; we never have line changes to report. */
    private static final ExploreSyncService CHANGE_SUMMARY_ACK
            = ExploreSyncService.newBuilder()
                    .setChangeSummaryResponse(
                            GdiExploreSyncService.ChangeSummaryResponse.getDefaultInstance())
                    .build();

    /**
     * One historical-sync run. Created by {@link #startSession} or by
     * a watch-initiated {@code StartSyncRequest}; disposed on
     * {@code SyncFinished} or BT disconnect.
     */
    private final class SyncSession {

        /**
         * Drained one entry at a time: pop → promote to currentBuf → fetch →
         * flush → pop next.
         */
        private final Deque<LineDigest.LineReference> pending = new ArrayDeque<>();
        @Nullable
        private HistoricalBuffer currentBuf;
        private final long deviceId;
        private final long userId;
        /** Drives FULL vs INCREMENTAL for the outgoing StartSyncRequest. */
        private final boolean needsFullSync;
        private final GBProgressNotification progress = new GBProgressNotification(
                GBApplication.getContext(), GB.NOTIFICATION_CHANNEL_ID_TRANSFER);
        /** Tracks whether we own the device's busy-task slot. GBDevice
         *  doesn't support nesting, so we can't rely on isBusy() to
         *  decide whether to unset on dispose — another subsystem may
         *  have taken the slot in the meantime. */
        private boolean weMarkedBusy = false;

        private SyncSession() throws GBException {
            // Resolve and cache the (deviceId, userId) ids up front so
            // per-op write paths can re-acquireDB() briefly instead of
            // holding the global write lock for the whole sync.
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                final DaoSession daoSession = dbHandler.getDaoSession();
                this.deviceId = DBHelper.getDevice(deviceSupport.getDevice(), daoSession).getId();
                this.userId = DBHelper.getUser(daoSession).getId();
            } catch (final Exception e) {
                throw new GBException("Failed to resolve device/user for ExploreSync session", e);
            }
            this.needsFullSync = !deviceSupport.getDevicePrefs().getBoolean(PREF_BASELINE_ESTABLISHED, false);
            progress.start(R.string.busy_task_processing_files, /* textRes */ 0, /* totalSize */ 0L);
            // WorkoutListActivity refreshes on the busy→idle edge;
            // dispose() clears the busy task to trigger that.
            final GBDevice device = deviceSupport.getDevice();
            device.setBusyTask(R.string.busy_task_fetch_activity_data, deviceSupport.getContext());
            weMarkedBusy = true;
            device.sendDeviceUpdateIntent(deviceSupport.getContext());
        }

        private void dispose() {
            progress.finish();
            if (weMarkedBusy) {
                final GBDevice device = deviceSupport.getDevice();
                device.unsetBusyTask();
                weMarkedBusy = false;
                device.sendDeviceUpdateIntent(deviceSupport.getContext());
            }
        }

        /**
         * Queue every non-deleted activity UUID from the digest and
         * reply with a {@code line_read_op} for the first one; an
         * empty-queue digest gets a bare ack.
         */
        private ExploreSyncService handleLineDigestWriteRequest(final LineDigestWriteRequest req) {
            // Routes, courses, tracking events aren't surfaced by GB.
            final List<LineDigest.LineReference> lines = new ArrayList<>(req.getDigest().getLinesList());
            lines.removeIf(line -> line.getLineType() != LineType.LINE_TYPE_ACTIVITY);
            progress.incrementTotalSize(lines.size());

            int queued = 0;
            for (final LineDigest.LineReference line : lines) {
                if (line.getDeleted()) {
                    // TODO: propagate watch-side deletes. Needs a
                    // uuid_hex column on BaseActivitySummary — the watch
                    // doesn't serve SUMMARY for a tombstoned UUID, so we
                    // can't learn the activity's start_time at delete
                    // time and have no other key to find the row.
                    continue;
                }
                pending.add(line);
                queued++;
            }
            if (queued > 0) {
                LOG.info("ExploreSync: queued {} activities for fetch", queued);
            }

            final LineDigestWriteResponse.Builder responseBuilder = LineDigestWriteResponse.newBuilder()
                    .setStatus(WriteStatus.WRITE_STATUS_SUCCESS);
            final LineDataRequestOp readOp = popNextRead();
            if (readOp != null) {
                responseBuilder.setLineReadOp(readOp);
                LOG.info("ExploreSync: requesting historical line {} (offset={})",
                        GB.hexdump(readOp.getUuid().toByteArray()), readOp.getPointOffset());
            }
            return ExploreSyncService.newBuilder().setLineDigestWriteResponse(responseBuilder).build();
        }

        /**
         * Promote next pending → currentBuf and return its first read op; null
         * when the queue is drained.
         */
        @Nullable
        private LineDataRequestOp popNextRead() {
            final LineDigest.LineReference line = pending.poll();
            if (line == null) {
                currentBuf = null;
                return null;
            }
            currentBuf = new HistoricalBuffer(line);
            return currentBuf.buildReadOp(0);
        }

        /**
         * Watch's reply to our outgoing {@code StartSyncRequest}. A
         * non-accepted status (e.g. capability mismatch) means no
         * digest is coming — tear down so the progress notification
         * doesn't hang on traffic that won't arrive.
         */
        @Nullable
        private ExploreSyncService handleStartSyncResponse(
                final GdiExploreSyncService.StartSyncResponse resp) {
            final StartSyncStatus status = resp.getStatus();
            if (status != StartSyncStatus.START_SYNC_ACCEPTED) {
                LOG.warn("ExploreSync: watch rejected our StartSyncRequest with {}, disposing session",
                        status);
                disposeSession();
            }
            return null;
        }

        /**
         * Watch's end-of-sync notification. On the first clean FULL we
         * flip the baseline pref so the next session can use
         * INCREMENTAL. Either way the session is done — dispose.
         */
        @Nullable
        private ExploreSyncService handleSyncFinishedNotification(
                final GdiExploreSyncService.SyncFinishedNotification notif) {
            final SyncFinishedStatus status = notif.getStatus();
            LOG.info("ExploreSync session ended: {}", status);
            if (status == SyncFinishedStatus.SYNC_FINISHED_OK && needsFullSync) {
                deviceSupport.getDevicePrefs().getPreferences().edit()
                        .putBoolean(PREF_BASELINE_ESTABLISHED, true).apply();
            }
            disposeSession();
            return null;
        }

        /**
         * Drive one {@code LineDataReady} into the in-flight buffer. The
         * next read op (chained on the response) is the same UUID for
         * in-line pagination (POINTS pages, then SUMMARY etc.), then the
         * next queued UUID once the current one drains; absent only when
         * the queue is also empty.
         */
        private ExploreSyncService handleLineDataReadyRequest(final LineDataReadyRequest req) {
            LineDataRequestOp next = currentBuf.absorb(req);
            if (next == null) {
                try {
                    currentBuf.flush();
                } catch (final Exception e) {
                    LOG.warn("Failed to write historical summary/GPX for {}", currentBuf.uuidHex, e);
                }
                progress.incrementTotalProgress(1);
                next = popNextRead();
            }
            final LineDataReadyResponse.Builder responseBuilder = LineDataReadyResponse.newBuilder();
            if (next != null) {
                responseBuilder.setNextDataRequest(next);
            }
            return ExploreSyncService.newBuilder()
                    .setLineDataReadyResponse(responseBuilder)
                    .build();
        }

        /**
         * Per-UUID in-flight import state. At most one alive at a time; the
         * rest of the digest sits in {@link #pending}.
         */
        private final class HistoricalBuffer {

            /** Wire byte form — needed on every read op. */
            private final ByteString uuid;
            /** Hex form for logging. */
            private final String uuidHex;
            /** Parts still owed by the watch, in fetch order. */
            private final LinkedHashSet<LinePart> partsToFetch = new LinkedHashSet<>(Arrays.asList(
                    LinePart.LINE_PART_POINTS,
                    LinePart.LINE_PART_SUMMARY,
                    LinePart.LINE_PART_STAT,
                    LinePart.LINE_PART_SPORT));
            private Line.StatPart statPart = Line.StatPart.getDefaultInstance();
            private Line.SportPart sportPart = Line.SportPart.getDefaultInstance();
            /** Generic fallback, overwritten by absorb when SUMMARY has a name. */
            private String summaryName = "Activity";
            private final List<ActivityPoint> points = new ArrayList<>();
            private boolean hasGpsPoints = false;
            private boolean hasHrPoints = false;

            private HistoricalBuffer(final LineDigest.LineReference line) {
                this.uuid = line.getUuid();
                this.uuidHex = GB.hexdump(uuid.toByteArray());
            }

            /**
             * True if any BaseActivitySummary exists at ({@code deviceId},
             * {@code startTimeSeconds}). Whether it came from us, a FIT
             * import, or another path, we treat it as authoritative and
             * skip the re-fetch. On DB error we also return true — skip
             * this attempt, retry on the next session — rather than
             * paying the POINTS round-trip for a flush() that's likely
             * to fail the same way.
             */
            private boolean alreadyImported(final long startTimeSeconds) {
                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    return ActivitySummaryParser.findBaseActivitySummary(dbHandler.getDaoSession(),
                            deviceSupport.getDevice(), startTimeSeconds) != null;
                } catch (final Exception e) {
                    LOG.warn("ExploreSync: skip-check failed for {}, deferring to next session",
                            uuidHex, e);
                    return true;
                }
            }

            /**
             * Reads the highest-priority still-needed part. Caller must ensure
             * partsToFetch is non-empty.
             */
            private LineDataRequestOp buildReadOp(final int pointOffset) {
                return LineDataRequestOp.newBuilder()
                        .setUuid(uuid)
                        .setPart(partsToFetch.iterator().next())
                        .setPointOffset(pointOffset)
                        .build();
            }

            /**
             * Absorb the watch's reply and return the next read op, or
             * null when the line is drained (caller then flushes).
             */
            @Nullable
            private LineDataRequestOp absorb(final LineDataReadyRequest req) {
                if (req.getStatus() == ReadStatus.READ_STATUS_ERROR) {
                    // Fatal for the line — discard any partial pagination;
                    // the catalog will list the UUID again next session.
                    LOG.warn("ExploreSync historical line {} returned READ_STATUS_ERROR — dropping",
                            uuidHex);
                    points.clear();
                    return null;
                }
                // Opportunistically absorb whichever parts the watch
                // volunteered. Each branch prunes its own part from
                // partsToFetch.
                final Line line = req.getLine();
                if (line.hasPointPart()) {
                    final Line.PointPart pointPart = line.getPointPart();
                    if (points.isEmpty() && alreadyImported(pointPart.getTimestamps(0))) {
                        // POINTS is fetched first so we can bail here:
                        // timestamps[0] is the row's startTime key, and
                        // SummaryPart.creation_time can't replace it
                        // (that's activity-end + ~10s, not start).
                        // flush() sees empty points and short-circuits.
                        return null;
                    }
                    appendPoints(pointPart);
                    if (req.getStatus() == ReadStatus.READ_STATUS_MORE) {
                        return buildReadOp(req.getNextPointOffset());
                    }
                    partsToFetch.remove(LinePart.LINE_PART_POINTS);
                }
                if (line.hasSummaryPart()) {
                    final Line.SummaryPart summaryPart = line.getSummaryPart();
                    if (summaryPart.hasName()) {
                        summaryName = summaryPart.getName();
                    }
                    partsToFetch.remove(LinePart.LINE_PART_SUMMARY);
                }
                if (line.hasStatPart()) {
                    statPart = line.getStatPart();
                    partsToFetch.remove(LinePart.LINE_PART_STAT);
                }
                if (line.hasSportPart()) {
                    sportPart = line.getSportPart();
                    partsToFetch.remove(LinePart.LINE_PART_SPORT);
                }

                if (partsToFetch.isEmpty()) {
                    return null;
                }
                return buildReadOp(0);
            }

            /**
             * Each per-point side array (altitudes, heart_rates, ...) is
             * either empty or parallel to generic_positions, so one
             * non-empty check up front gates the per-index reads.
             */
            private void appendPoints(final Line.PointPart pointPart) {
                final int pointCount = pointPart.getGenericPositionsCount();
                final boolean hasAltitude = pointPart.getAltitudesCount() > 0;
                final ByteString heartRates = pointPart.getHeartRates();
                final boolean hasHr = !heartRates.isEmpty();
                final ByteString cadences = pointPart.getCadences();
                final boolean hasCadence = !cadences.isEmpty();
                final boolean hasSpeed = pointPart.getSpeedsCount() > 0;
                final boolean hasTemp = pointPart.getTemperaturesCount() > 0;
                final boolean hasDepth = pointPart.getWaterDepthCount() > 0;
                for (int i = 0; i < pointCount; i++) {
                    final ActivityPoint point = new ActivityPoint(new Date(pointPart.getTimestamps(i) * 1000L));

                    // fixed64 packs two int32 semicircles: low=lat, high=lon.
                    final long encodedPosition = pointPart.getGenericPositions(i);
                    final int latSemi = (int) (encodedPosition & 0xFFFFFFFFL);
                    final int lonSemi = (int) (encodedPosition >>> 32);
                    // INT_MIN and INT_MAX in either half are the watch's
                    // invalid-coordinate sentinels.
                    final boolean validCoords = latSemi != Integer.MIN_VALUE
                            && latSemi != Integer.MAX_VALUE
                            && lonSemi != Integer.MIN_VALUE
                            && lonSemi != Integer.MAX_VALUE;
                    if (validCoords) {
                        final double altitude = hasAltitude && pointPart.getAltitudes(i) != SENTINEL_FLOAT
                                ? pointPart.getAltitudes(i) : GPSCoordinate.UNKNOWN_ALTITUDE;
                        point.setLocation(new GPSCoordinate(
                                GarminUtils.semicirclesToDegrees(lonSemi),
                                GarminUtils.semicirclesToDegrees(latSemi),
                                altitude));
                        hasGpsPoints = true;
                    }

                    if (hasHr) {
                        final int heartRate = heartRates.byteAt(i) & 0xFF;
                        if (heartRate != SENTINEL_HR_CADENCE) {
                            point.setHeartRate(heartRate);
                            hasHrPoints = true;
                        }
                    }
                    if (hasCadence) {
                        // Unmeasured cadence on e.g. XC ski would
                        // chart as a flat line at 255 spm; drop it.
                        final int cadence = cadences.byteAt(i) & 0xFF;
                        if (cadence != SENTINEL_HR_CADENCE) {
                            point.setCadence(cadence);
                        }
                    }
                    if (hasSpeed) {
                        final float speed = pointPart.getSpeeds(i);
                        if (speed != SENTINEL_FLOAT) {
                            point.setSpeed(speed);
                        }
                    }
                    if (hasTemp) {
                        final float temperature = pointPart.getTemperatures(i);
                        if (temperature != SENTINEL_FLOAT) {
                            point.setTemperature(temperature);
                        }
                    }
                    if (hasDepth) {
                        final float depth = pointPart.getWaterDepth(i);
                        if (depth != SENTINEL_FLOAT) {
                            point.setDepth(depth);
                        }
                    }
                    points.add(point);
                }
            }

            /**
             * StatPart → chips. Units match {@code GarminWorkoutParser}
             * so FIT and ExploreSync render identically.
             */
            private ActivitySummaryData buildSummaryData() {
                // Per-field sentinel filter: hasX() is useless because the
                // watch sets the field even when its value is the "no
                // value" marker — we have to compare to the sentinel.
                final ActivitySummaryData summaryData = new ActivitySummaryData();
                if (statPart.getDistance() != SENTINEL_DISTANCE_M) {
                    summaryData.add(ActivitySummaryEntries.DISTANCE_METERS, statPart.getDistance(),
                            ActivitySummaryEntries.UNIT_METERS);
                }
                if (statPart.getCalories() != SENTINEL_CALORIES_KCAL) {
                    summaryData.add(ActivitySummaryEntries.CALORIES_BURNT, statPart.getCalories(),
                            ActivitySummaryEntries.UNIT_KCAL);
                }
                if (statPart.getTotalAscent() != SENTINEL_ASCENT_DESCENT_M) {
                    summaryData.add(ActivitySummaryEntries.TOTAL_ASCENT, statPart.getTotalAscent(),
                            ActivitySummaryEntries.UNIT_METERS);
                }
                if (statPart.getTotalDescent() != SENTINEL_ASCENT_DESCENT_M) {
                    summaryData.add(ActivitySummaryEntries.TOTAL_DESCENT, statPart.getTotalDescent(),
                            ActivitySummaryEntries.UNIT_METERS);
                }
                if (statPart.getMaxElevation() != SENTINEL_FLOAT) {
                    summaryData.add(ActivitySummaryEntries.ALTITUDE_MAX, statPart.getMaxElevation(),
                            ActivitySummaryEntries.UNIT_METERS);
                }
                if (statPart.getMinElevation() != SENTINEL_FLOAT) {
                    summaryData.add(ActivitySummaryEntries.ALTITUDE_MIN, statPart.getMinElevation(),
                            ActivitySummaryEntries.UNIT_METERS);
                }
                // uint32 proto field but ships the uint8 sentinel —
                // observed as 255 bpm on activities with no HR data.
                if (statPart.getAvgHeartRate() != SENTINEL_HR_CADENCE) {
                    summaryData.add(ActivitySummaryEntries.HR_AVG, statPart.getAvgHeartRate(),
                            ActivitySummaryEntries.UNIT_BPM);
                }
                summaryData.add(ActivitySummaryEntries.ACTIVE_SECONDS, statPart.getTimerTime(),
                        ActivitySummaryEntries.UNIT_SECONDS);
                if (statPart.getMovingSpeed() != SENTINEL_FLOAT) {
                    summaryData.add(ActivitySummaryEntries.SPEED_AVG, statPart.getMovingSpeed(),
                            ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
                }
                if (statPart.getMaxSpeed() != SENTINEL_FLOAT) {
                    summaryData.add(ActivitySummaryEntries.SPEED_MAX, statPart.getMaxSpeed(),
                            ActivitySummaryEntries.UNIT_METERS_PER_SECOND);
                }
                return summaryData;
            }

            /**
             * Persist the buffered line: GPX file (if any GPS points), a
             * new BaseActivitySummary row keyed on the first-point
             * timestamp, plus per-point HR samples for indoor activities.
             */
            private void flush() throws Exception {
                if (points.isEmpty()) {
                    // Upfront skip-check matched in absorb().
                    LOG.info("Historical line {}: skipped (already imported)", uuidHex);
                    return;
                }
                // GPX before DB: a file-write failure aborts the line
                // without ever taking the DB write lock.
                final String gpxPath = hasGpsPoints ? writeGpx() : null;

                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    final DaoSession daoSession = dbHandler.getDaoSession();
                    final BaseActivitySummary summary = ActivitySummaryParser.createBaseActivitySummary(
                            daoSession, deviceId, points.get(0).getTime().getTime() / 1000L);
                    summary.setEndTime(points.get(points.size() - 1).getTime());
                    summary.setName(summaryName);
                    summary.setGpxTrack(gpxPath);
                    if (sportPart.hasSport()) {
                        // Set before persistActivitySamples — it reads
                        // summary.getActivityKind() into each sample.
                        GarminSport.fromCodes(sportPart.getSport(), sportPart.getSubSport())
                                .ifPresent(sport -> summary.setActivityKind(sport.getActivityKind().getCode()));
                    }
                    final ActivitySummaryData chips = buildSummaryData();
                    if (!chips.getKeys().isEmpty()) {
                        summary.setSummaryData(chips.toString());
                    }

                    if (!hasGpsPoints && hasHrPoints) {
                        // Indoor activity: no GPX polyline, so the
                        // workout chart falls back to ActivitySample.
                        persistActivitySamples(daoSession, summary);
                    }

                    // Insert last — the row appears in the workout list
                    // only after every field is set and the GPX exists.
                    daoSession.getBaseActivitySummaryDao().insertOrReplace(summary);
                    LOG.info("Historical line {} → {} pts, summary id={}",
                            uuidHex, points.size(), summary.getId());
                }
                GB.signalActivityDataFinish(deviceSupport.getDevice());
            }

            /**
             * Export the buffered points to a GPX under the device's
             * export dir, named by activity start + UUID prefix.
             */
            private String writeGpx() throws Exception {
                final Date startDate = points.get(0).getTime();
                final File gpxFile = new File(deviceSupport.getWritableExportDirectory(),
                        GarminUtils.buildExportPath(FileType.FILETYPE.ACTIVITY, startDate,
                                uuidHex.substring(0, 8), "gpx"));
                //noinspection ResultOfMethodCallIgnored
                gpxFile.getParentFile().mkdirs();
                final ActivityTrack track = new ActivityTrack();
                track.setBaseTime(startDate);
                track.setName(summaryName);
                track.addTrackPoints(points);
                new GPXExporter().performExport(track, gpxFile, /* summary */ null);
                return gpxFile.getAbsolutePath();
            }

            /**
             * Write one {@link GarminActivitySample} per buffered ActivityPoint
             * with a real HR reading — the workout details fragment reads these
             * via getAllSamples() when the activity track is empty (indoor
             * path).
             *
             * Distance / calories / intensity / steps stay at NOT_MEASURED so
             * readers can distinguish "ExploreSync didn't carry this" from
             * "watch recorded zero".
             */
            private void persistActivitySamples(final DaoSession daoSession,
                    final BaseActivitySummary summary) {
                final List<GarminActivitySample> samples = new ArrayList<>(points.size());
                for (final ActivityPoint point : points) {
                    final int heartRate = point.getHeartRate();
                    if (heartRate <= 0) {
                        continue;
                    }
                    samples.add(new GarminActivitySample(
                            (int) (point.getTime().getTime() / 1000L),
                            deviceId,
                            userId,
                            ActivitySample.NOT_MEASURED, // rawIntensity
                            ActivitySample.NOT_MEASURED, // steps
                            summary.getActivityKind(),
                            heartRate,
                            ActivitySample.NOT_MEASURED, // distanceCm
                            ActivitySample.NOT_MEASURED // activeCalories
                    ));
                }
                if (samples.isEmpty()) {
                    return;
                }
                final GarminActivitySampleProvider provider
                        = new GarminActivitySampleProvider(deviceSupport.getDevice(), daoSession);
                provider.addGBActivitySamples(samples);
                LOG.debug("ExploreSync line {}: persisted {} activity samples",
                        uuidHex, samples.size());
            }

        }
    }

    @Nullable
    private SyncSession state;

    ExploreSyncHandler(final GarminSupport deviceSupport) {
        this.deviceSupport = deviceSupport;
    }

    /**
     * Begin a historical-catalog sync: replace any prior session, send
     * a {@code StartSyncRequest} so the watch starts pushing digests.
     * Called by {@link GarminSupport} on initial connect and on user
     * pull-to-refresh.
     */
    void startSession() {
        if (!replaceSession()) {
            return;
        }
        final GdiExploreSyncService.SyncType syncType = state.needsFullSync
                ? GdiExploreSyncService.SyncType.SYNC_TYPE_FULL
                : GdiExploreSyncService.SyncType.SYNC_TYPE_INCREMENTAL;
        deviceSupport.sendProtobufRequest("explore-sync start",
                GdiSmartProto.Smart.newBuilder()
                        .setExploreSyncService(ExploreSyncService.newBuilder()
                                .setStartSyncRequest(
                                        GdiExploreSyncService.StartSyncRequest.newBuilder()
                                                .setSyncType(syncType)
                                                .setProtocolVersion(PROTOCOL_VERSION)
                                                .setAppUuid(APP_UUID)))
                        .build());
        LOG.info("ExploreSync StartSyncRequest sent (sync_type={})", syncType);
    }

    /**
     * Handle an incoming ExploreSync message and produce an ExploreSyncService
     * response (or null if no immediate response is needed). The caller wraps
     * the result in a {@code Smart} envelope.
     */
    @Nullable
    ExploreSyncService handle(final ExploreSyncService req) {
        if (req.hasStartSyncRequest()) {
            // Watch-initiated session. The watch rejects the response
            // (SYNC_FINISHED_FAILED) without an echoed app_uuid.
            return ExploreSyncService.newBuilder()
                    .setStartSyncResponse(GdiExploreSyncService.StartSyncResponse.newBuilder()
                            .setSupportedProtocolVersion(PROTOCOL_VERSION)
                            .setAppUuid(APP_UUID)
                            .setStatus(replaceSession()
                                    ? StartSyncStatus.START_SYNC_ACCEPTED
                                    : StartSyncStatus.START_SYNC_REJECTED))
                    .build();
        }
        // Stateless ACKs for parts we don't surface in GB — cached
        // empty-body responses returned verbatim.
        if (req.hasLineDigestReadRequest()) {
            return LINE_DIGEST_READ_ACK;
        }
        if (req.hasLineReadRequest()) {
            return LINE_READ_ACK;
        }
        if (req.hasCollectionListWriteRequest()) {
            return COLLECTION_LIST_WRITE_ACK;
        }
        if (req.hasCollectionDigestWriteRequest()) {
            return COLLECTION_DIGEST_WRITE_ACK;
        }
        if (req.hasCollectionDigestReadRequest()) {
            return COLLECTION_DIGEST_READ_ACK;
        }
        if (req.hasCollectionReadRequest()) {
            return COLLECTION_READ_ACK;
        }
        if (req.hasWaypointDigestWriteRequest()) {
            return WAYPOINT_DIGEST_WRITE_ACK;
        }
        if (req.hasWaypointDigestReadRequest()) {
            return WAYPOINT_DIGEST_READ_ACK;
        }
        if (req.hasWaypointReadRequest()) {
            return WAYPOINT_READ_ACK;
        }
        if (req.hasActiveLineDigestWriteRequest()) {
            return ACTIVE_LINE_DIGEST_WRITE_ACK;
        }
        if (req.hasChangeSummaryRequest()) {
            return CHANGE_SUMMARY_ACK;
        }

        // Everything below is intra-session and needs an active
        // StartSync; drop if state is gone (protocol violation or BT
        // race).
        if (state == null) {
            LOG.warn("ExploreSync intra-session message arrived without an active session, ignoring: {}", req);
            return null;
        }

        if (req.hasStartSyncResponse()) {
            return state.handleStartSyncResponse(req.getStartSyncResponse());
        }
        if (req.hasLineDataReadyRequest()) {
            return state.handleLineDataReadyRequest(req.getLineDataReadyRequest());
        }
        if (req.hasLineDigestWriteRequest()) {
            return state.handleLineDigestWriteRequest(req.getLineDigestWriteRequest());
        }
        if (req.hasSyncFinishedNotification()) {
            return state.handleSyncFinishedNotification(req.getSyncFinishedNotification());
        }

        LOG.debug("ExploreSync message not yet handled: {}", req);
        return null;
    }

}
