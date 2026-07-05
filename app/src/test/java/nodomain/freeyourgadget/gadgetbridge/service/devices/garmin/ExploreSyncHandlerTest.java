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

import android.bluetooth.BluetoothGattCharacteristic;

import com.google.protobuf.ByteString;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.ChangeSummaryRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.ExploreSyncService;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.Line;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDataReadyRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDataRequestOp;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineDigestWriteRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LinePart;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.LineType;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.ReadStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.StartSyncRequest;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.StartSyncResponse;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.StartSyncStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.SyncFinishedNotification;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.SyncFinishedStatus;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.SyncType;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiExploreSyncService.VersionStamp;
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

/**
 * Protocol-level tests for {@link ExploreSyncHandler}: drives the
 * dispatcher with synthetic requests and asserts the response shape
 * and any persisted side-effects.
 */
public class ExploreSyncHandlerTest extends TestBase {
    private static final String DEVICE_ADDRESS = "00:11:22:33:44:55";
    private static final String PREF_BASELINE_ESTABLISHED = "garmin_explore_sync_baseline_established";

    private RecordingGarminSupport support;
    private ExploreSyncHandler handler;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        support = new RecordingGarminSupport();
        support.setContext(
                new GBDevice(DEVICE_ADDRESS, "TestFenix", null, null, DeviceType.GARMIN_FENIX_7_PRO),
                /*btAdapter*/ null,
                org.robolectric.RuntimeEnvironment.getApplication());
        support.getDevicePrefs().getPreferences().edit()
                .putStringSet(GarminPreferences.PREF_GARMIN_CAPABILITIES,
                        Collections.singleton(GarminCapability.EXPLORE_SYNC.name()))
                .apply();
        handler = new ExploreSyncHandler(support);
    }

    // ---- StartSync / baseline ---------------------------------------------------

    @Test
    public void startSession_freshDevice_sendsFullSync() {
        handler.startSession();

        Assert.assertEquals(1, support.outgoing.size());
        final StartSyncRequest req = support.outgoing.get(0)
                .getExploreSyncService().getStartSyncRequest();
        Assert.assertEquals(SyncType.SYNC_TYPE_FULL, req.getSyncType());
        Assert.assertEquals(2, req.getProtocolVersion());
        // Historical-only; live-activity capability lives on a separate branch.
        Assert.assertEquals(0, req.getSyncCapabilities());
        // Stable per-app id keeps the watch's saved_app_transaction_id
        // across reinstalls — without it, the watch rejects us.
        Assert.assertTrue(req.hasAppUuid());
        Assert.assertEquals(16, req.getAppUuid().size());
    }

    @Test
    public void startSession_baselineEstablished_sendsIncrementalSync() {
        setBaselineEstablished(true);

        handler.startSession();

        Assert.assertEquals(SyncType.SYNC_TYPE_INCREMENTAL,
                support.outgoing.get(0).getExploreSyncService()
                        .getStartSyncRequest().getSyncType());
    }

    @Test
    public void syncFinishedOk_afterFullSync_flipsBaseline() {
        Assert.assertFalse(isBaselineEstablished());
        handler.startSession();
        handler.handle(syncFinished(SyncFinishedStatus.SYNC_FINISHED_OK));

        Assert.assertTrue(isBaselineEstablished());
    }

    @Test
    public void syncFinishedFailed_leavesBaselineUnset() {
        handler.startSession();
        handler.handle(syncFinished(SyncFinishedStatus.SYNC_FINISHED_FAILED_DATA_LIMIT));

        // Interrupted FULL must retry next time, so the pref must stay off.
        Assert.assertFalse(isBaselineEstablished());
    }

    @Test
    public void syncFinishedOk_afterIncremental_keepsBaselineTrue() {
        setBaselineEstablished(true);
        handler.startSession();
        handler.handle(syncFinished(SyncFinishedStatus.SYNC_FINISHED_OK));

        Assert.assertTrue(isBaselineEstablished());
    }

    @Test
    public void watchInitiatedStartSync_replacesExistingSession() {
        handler.startSession();

        final ExploreSyncService resp = handler.handle(ExploreSyncService.newBuilder()
                .setStartSyncRequest(StartSyncRequest.newBuilder()
                        .setSyncType(SyncType.SYNC_TYPE_FULL)
                        .setProtocolVersion(2))
                .build());

        // Echoed app_uuid is mandatory; watch rejects responses without it.
        final StartSyncResponse sr = resp.getStartSyncResponse();
        Assert.assertEquals(StartSyncStatus.START_SYNC_ACCEPTED, sr.getStatus());
        Assert.assertEquals(16, sr.getAppUuid().size());
    }

    @Test
    public void startSyncRejected_disposesSession() {
        handler.startSession();
        handler.handle(ExploreSyncService.newBuilder()
                .setStartSyncResponse(StartSyncResponse.newBuilder()
                        .setStatus(StartSyncStatus.START_SYNC_REJECTED))
                .build());

        // Intra-session message after disposal returns null (state == null).
        Assert.assertNull(handler.handle(emptyLineDigestWrite()));
    }

    @Test
    public void onDisconnected_disposesSession() {
        handler.startSession();
        handler.onDisconnected();

        Assert.assertNull(handler.handle(emptyLineDigestWrite()));
    }

    // ---- LineDigest enqueue -----------------------------------------------------

    @Test
    public void lineDigestWrite_emptyDigest_acksWithNoReadOp() {
        handler.startSession();
        final ExploreSyncService resp = handler.handle(emptyLineDigestWrite());

        // Nothing to fetch — response carries no nextDataRequest.
        Assert.assertEquals(GdiExploreSyncService.WriteStatus.WRITE_STATUS_SUCCESS,
                resp.getLineDigestWriteResponse().getStatus());
        Assert.assertFalse(resp.getLineDigestWriteResponse().hasLineReadOp());
    }

    @Test
    public void lineDigestWrite_nonDeletedActivity_returnsReadOpForFirstUuid() {
        handler.startSession();
        final ByteString uuid = uuid("activity-aaaa");
        final ExploreSyncService resp = handler.handle(lineDigestWrite(
                activityRef(uuid, /*deleted*/ false)));

        // Read op points at the queued UUID's POINTS part. POINTS is
        // fetched first (matching Garmin Explore's flow); the first
        // page's timestamps[0] is our skip-lookup key.
        final LineDataRequestOp op = resp.getLineDigestWriteResponse().getLineReadOp();
        Assert.assertEquals(uuid, op.getUuid());
        Assert.assertEquals(LinePart.LINE_PART_POINTS, op.getPart());
        Assert.assertEquals(0, op.getPointOffset());
    }

    @Test
    public void lineDigestWrite_deletedEntriesSkipped_noReadOp() {
        handler.startSession();
        final ExploreSyncService resp = handler.handle(lineDigestWrite(
                activityRef(uuid("dead-1"), /*deleted*/ true),
                activityRef(uuid("dead-2"), /*deleted*/ true)));

        // All deleted → queue stays empty → ack carries no nextDataRequest.
        // Future work: propagate watch-side deletes to BaseActivitySummary.
        Assert.assertFalse(resp.getLineDigestWriteResponse().hasLineReadOp());
    }

    @Test
    public void lineDigestWrite_nonActivityLineTypesIgnored() {
        handler.startSession();
        final ExploreSyncService resp = handler.handle(lineDigestWrite(
                LineDigest.LineReference.newBuilder()
                        .setUuid(uuid("route"))
                        .setLineType(LineType.LINE_TYPE_ROUTE)
                        .build()));

        // Only LINE_TYPE_ACTIVITY entries are surfaced as BaseActivitySummary.
        Assert.assertFalse(resp.getLineDigestWriteResponse().hasLineReadOp());
    }

    // ---- Per-line fetch + flush -------------------------------------------------

    /** Encode the all-INT_MIN sentinel position the watch sends for
     *  GPS-less laps (indoor / pool).*/
    private static long sentinelPosition() {
        final long lat = Integer.MIN_VALUE & 0xFFFFFFFFL;
        final long lon = ((long) Integer.MIN_VALUE) << 32;
        return lat | lon;
    }

    /** Pack two int32 semicircles into the watch's fixed64 position
     *  encoding: low=lat, high=lon.*/
    private static long packPosition(final int latSemi, final int lonSemi) {
        return (latSemi & 0xFFFFFFFFL) | (((long) lonSemi) << 32);
    }

    @Test
    public void pointsReply_statusMore_requestsNextOffset() {
        final ByteString uuid = uuid("paged");
        final int t0 = 1_700_000_500;
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));

        final ExploreSyncService resp = handler.handle(pointsReply(uuid,
                ReadStatus.READ_STATUS_MORE,
                /*nextOffset*/ 100,
                /*timestamps*/ new int[]{t0},
                /*positions*/ new long[]{sentinelPosition()},
                /*heartRates*/ new byte[]{50}));

        // Mid-pagination: re-request POINTS at the watch's offset.
        final LineDataRequestOp next = resp.getLineDataReadyResponse().getNextDataRequest();
        Assert.assertEquals(uuid, next.getUuid());
        Assert.assertEquals(LinePart.LINE_PART_POINTS, next.getPart());
        Assert.assertEquals(100, next.getPointOffset());
    }

    @Test
    public void pointsReply_statusSuccess_advancesToSummary() {
        final ByteString uuid = uuid("done");
        final int t0 = 1_700_000_750;
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));

        final ExploreSyncService resp = handler.handle(pointsReply(uuid,
                ReadStatus.READ_STATUS_SUCCESS,
                /*nextOffset*/ 0,
                new int[]{t0},
                new long[]{sentinelPosition()},
                new byte[]{50}));

        // POINTS drained → next part owed: SUMMARY.
        Assert.assertEquals(LinePart.LINE_PART_SUMMARY,
                resp.getLineDataReadyResponse().getNextDataRequest().getPart());
    }

    @Test
    public void pointsReply_firstPageMatchesFitImportedRow_skipsRemainingFetch() {
        // FIT importer ran first and populated rawDetailsPath; that
        // row counts as authoritative — the ExploreSync digest's same
        // activity must skip without re-fetching POINTS.
        final int t0 = 1_700_010_000;
        final BaseActivitySummary preexisting = ActivitySummaryParser
                .findOrCreateBaseActivitySummary(daoSession, support.getDevice(), (long) t0);
        preexisting.setRawDetailsPath("/sdcard/.../ACTIVITY_2024-12-27.fit");
        daoSession.getBaseActivitySummaryDao().insertOrReplace(preexisting);

        final ByteString uuid = uuid("already-have");
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));
        final ExploreSyncService resp = handler.handle(pointsReply(uuid,
                ReadStatus.READ_STATUS_MORE,
                /*nextOffset*/ 50,
                new int[]{t0},
                new long[]{sentinelPosition()},
                new byte[]{50}));

        // Line drained → response carries no nextDataRequest.
        Assert.assertFalse(resp.getLineDataReadyResponse().hasNextDataRequest());
    }

    @Test
    public void readStatusError_dropsCurrentLine_movesToNextQueuedUuid() {
        final ByteString first = uuid("err-1");
        final ByteString second = uuid("ok-2");
        handler.startSession();
        handler.handle(lineDigestWrite(
                activityRef(first, false),
                activityRef(second, false)));

        final ExploreSyncService resp = handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_ERROR)
                        .setLine(Line.newBuilder().setUuid(first).setLineType(LineType.LINE_TYPE_ACTIVITY)))
                .build());

        // Watch returned an error for the head — drop it, move on to the
        // next queued UUID. Next part for a fresh line is POINTS.
        final LineDataRequestOp next = resp.getLineDataReadyResponse().getNextDataRequest();
        Assert.assertEquals(second, next.getUuid());
        Assert.assertEquals(LinePart.LINE_PART_POINTS, next.getPart());
    }

    @Test
    public void indoorActivity_persistsSummaryWithoutGpx() {
        final ByteString uuid = uuid("indoor");
        final int t0 = 1_700_000_000;
        runOneLine(uuid, t0, /*indoor*/ true, "Indoor Run", /*sport*/ null);

        final BaseActivitySummary summary = findSummary(t0);
        Assert.assertNotNull(summary);
        // Sentinel coords give no polyline, so no GPX file.
        Assert.assertNull(summary.getGpxTrack());
        Assert.assertEquals("Indoor Run", summary.getName());
        // End time is the last point.
        Assert.assertEquals((t0 + 60) * 1000L, summary.getEndTime().getTime());
    }

    @Test
    public void outdoorActivity_writesGpxAndSetsActivityKind() {
        final ByteString uuid = uuid("outdoor");
        final int t0 = 1_700_001_000;
        // SportPart with running sport so flush() can map → ActivityKind.
        final Line.SportPart sport = Line.SportPart.newBuilder()
                .setSport(1).setSubSport(0).build();
        runOneLine(uuid, t0, /*indoor*/ false, "Morning Run", sport);

        final BaseActivitySummary summary = findSummary(t0);
        Assert.assertNotNull(summary);
        Assert.assertNotNull("outdoor line must produce a GPX path", summary.getGpxTrack());
        Assert.assertNotEquals(ActivityKind.UNKNOWN.getCode(), summary.getActivityKind());
    }

    @Test
    public void statPartWithRealValues_populatesSummaryDataChips() {
        // StatPart fields above their sentinels should land in
        // summaryData; sentinel-valued fields stay out.
        final ByteString uuid = uuid("chips");
        final int t0 = 1_700_003_000;
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));
        handler.handle(pointsReply(uuid, ReadStatus.READ_STATUS_SUCCESS, 0,
                new int[]{t0, t0 + 60}, new long[]{sentinelPosition(), sentinelPosition()},
                new byte[]{(byte) 100, (byte) 110}));
        handler.handle(summaryReply(uuid, "With Stats", t0 + 70));
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setStatPart(Line.StatPart.newBuilder()
                                        .setDistance(5000f)
                                        .setCalories(420)
                                        .setTimerTime(3600))))
                .build());
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setSportPart(Line.SportPart.getDefaultInstance())))
                .build());

        final String chips = findSummary(t0).getSummaryData();
        Assert.assertNotNull("StatPart values must produce summaryData chips", chips);
        Assert.assertTrue("distance chip", chips.contains("\"distanceMeters\""));
        Assert.assertTrue("calories chip", chips.contains("\"active_calories\""));
        Assert.assertTrue("timer chip", chips.contains("\"activeSeconds\""));
    }

    @Test
    public void summaryWithoutName_fallsBackToActivity() {
        // SUMMARY reply without a name leaves summaryName at its
        // default "Activity" fallback.
        final ByteString uuid = uuid("nameless");
        final int t0 = 1_700_004_000;
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));
        handler.handle(pointsReply(uuid, ReadStatus.READ_STATUS_SUCCESS, 0,
                new int[]{t0}, new long[]{sentinelPosition()}, new byte[]{(byte) 80}));
        // SUMMARY with creation_time but no name.
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setSummaryPart(Line.SummaryPart.newBuilder()
                                        .setCreationTime(t0 + 10))))
                .build());
        // STAT + SPORT defaults in one reply to drain the line.
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setStatPart(Line.StatPart.getDefaultInstance())
                                .setSportPart(Line.SportPart.getDefaultInstance())))
                .build());

        Assert.assertEquals("Activity", findSummary(t0).getName());
    }

    // ---- Stateless ACKs ---------------------------------------------------------

    @Test
    public void changeSummaryRequest_repliesWithEmptyResponse() {
        // Read-only ChangeSummaryResponse — we have nothing to report so
        // the watch shouldn't gate its sync on our ending_transaction_id.
        final ExploreSyncService resp = handler.handle(ExploreSyncService.newBuilder()
                .setChangeSummaryRequest(ChangeSummaryRequest.newBuilder()
                        .setStartingTransactionId(42))
                .build());

        Assert.assertEquals(0, resp.getChangeSummaryResponse().getEndingTransactionId());
    }

    @Test
    public void readOnlyStubs_returnSuccessStatus() {
        // The watch may probe any of these read-only services. We reply
        // SUCCESS with no body so it doesn't stall — no startSession needed.
        Assert.assertEquals(ReadStatus.READ_STATUS_SUCCESS,
                handler.handle(ExploreSyncService.newBuilder()
                        .setLineDigestReadRequest(GdiExploreSyncService.LineDigestReadRequest.getDefaultInstance())
                        .build())
                        .getLineDigestReadResponse().getStatus());
        Assert.assertEquals(ReadStatus.READ_STATUS_SUCCESS,
                handler.handle(ExploreSyncService.newBuilder()
                        .setLineReadRequest(GdiExploreSyncService.LineReadRequest.getDefaultInstance())
                        .build())
                        .getLineReadResponse().getStatus());
    }

    @Test
    public void writeOnlyStubs_returnSuccessStatus() {
        // The watch may push catalogue/collection writes regardless of
        // whether we're in a session. Stateless WRITE_STATUS_SUCCESS replies.
        Assert.assertEquals(GdiExploreSyncService.WriteStatus.WRITE_STATUS_SUCCESS,
                handler.handle(ExploreSyncService.newBuilder()
                        .setCollectionListWriteRequest(GdiExploreSyncService.CollectionListWriteRequest.getDefaultInstance())
                        .build())
                        .getCollectionListWriteResponse().getStatus());
        Assert.assertEquals(GdiExploreSyncService.WriteStatus.WRITE_STATUS_SUCCESS,
                handler.handle(ExploreSyncService.newBuilder()
                        .setActiveLineDigestWriteRequest(GdiExploreSyncService.ActiveLineDigestWriteRequest.getDefaultInstance())
                        .build())
                        .getActiveLineDigestWriteResponse().getStatus());
    }

    // ---- GarminSupport.finishFileSync / ExploreSync re-arm ----------------------
    //
    // Historical bug: the "no more files to download" fetch cycle in
    // GarminSupport has two exits — no FIT files were queued for parsing,
    // or FitAsyncProcessor just finished parsing what was queued. A prior
    // refactor (upstream "Only trigger ExploreSync after normal sync")
    // added the ExploreSync re-arm to only the first exit; the second —
    // the common case, since a watch almost always has at least one
    // pending FIT file — silently never restarted ExploreSync.
    // finishFileSync() is now the single shared tail for both exits, so
    // testing it directly (rather than via either caller) covers both.

    @Test
    public void finishFileSync_reArmsExploreSync() {
        support.finishFileSync();

        Assert.assertEquals(1, support.outgoing.size());
        Assert.assertTrue(support.outgoing.get(0).getExploreSyncService().hasStartSyncRequest());
    }

    // ---- helpers ----------------------------------------------------------------

    private void setBaselineEstablished(final boolean v) {
        support.getDevicePrefs().getPreferences().edit()
                .putBoolean(PREF_BASELINE_ESTABLISHED, v).apply();
    }

    private boolean isBaselineEstablished() {
        return support.getDevicePrefs().getBoolean(PREF_BASELINE_ESTABLISHED, false);
    }

    private static ByteString uuid(final String tag) {
        return ByteString.copyFromUtf8(tag);
    }

    private static LineDigest.LineReference activityRef(final ByteString uuid, final boolean deleted) {
        return LineDigest.LineReference.newBuilder()
                .setUuid(uuid)
                .setLineType(LineType.LINE_TYPE_ACTIVITY)
                .setDeleted(deleted)
                .setSummaryPartVersionStamp(VersionStamp.newBuilder().setEditTime(100))
                .setPointPartVersionStamp(VersionStamp.newBuilder().setEditTime(200))
                .build();
    }

    private static ExploreSyncService lineDigestWrite(final LineDigest.LineReference... lines) {
        final LineDigest.Builder digest = LineDigest.newBuilder();
        for (final LineDigest.LineReference line : lines) {
            digest.addLines(line);
        }
        return ExploreSyncService.newBuilder()
                .setLineDigestWriteRequest(LineDigestWriteRequest.newBuilder().setDigest(digest))
                .build();
    }

    private static ExploreSyncService emptyLineDigestWrite() {
        return ExploreSyncService.newBuilder()
                .setLineDigestWriteRequest(LineDigestWriteRequest.newBuilder()
                        .setDigest(LineDigest.getDefaultInstance()))
                .build();
    }

    private static ExploreSyncService syncFinished(final SyncFinishedStatus status) {
        return ExploreSyncService.newBuilder()
                .setSyncFinishedNotification(SyncFinishedNotification.newBuilder().setStatus(status))
                .build();
    }

    /** Build a SUMMARY reply for one UUID. {@code creationTimeSeconds}
     *  is the value the handler keys its skip-lookup off. */
    private static ExploreSyncService summaryReply(final ByteString uuid,
                                                   final String name,
                                                   final int creationTimeSeconds) {
        return ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid)
                                .setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setSummaryPart(Line.SummaryPart.newBuilder()
                                        .setName(name)
                                        .setCreationTime(creationTimeSeconds))))
                .build();
    }

    /** Build a POINTS reply for one UUID with parallel timestamp /
     *  position / HR arrays — all three must have the same length. */
    private static ExploreSyncService pointsReply(final ByteString uuid,
                                                  final ReadStatus status,
                                                  final int nextOffset,
                                                  final int[] timestamps,
                                                  final long[] positions,
                                                  final byte[] heartRates) {
        final Line.PointPart.Builder pp = Line.PointPart.newBuilder()
                .setHeartRates(ByteString.copyFrom(heartRates));
        for (final int t : timestamps) {
            pp.addTimestamps(t);
        }
        for (final long p : positions) {
            pp.addGenericPositions(p);
        }
        final LineDataReadyRequest.Builder req = LineDataReadyRequest.newBuilder()
                .setStatus(status)
                .setLine(Line.newBuilder()
                        .setUuid(uuid)
                        .setLineType(LineType.LINE_TYPE_ACTIVITY)
                        .setPointPart(pp));
        if (status == ReadStatus.READ_STATUS_MORE) {
            req.setNextPointOffset(nextOffset);
        }
        return ExploreSyncService.newBuilder().setLineDataReadyRequest(req).build();
    }

    /** Drive one complete UUID through the fetcher: digest → POINTS
     *  → SUMMARY → empty STAT/SPORT to drain the fetch list and
     *  trigger flush(). */
    private void runOneLine(final ByteString uuid,
                            final int t0,
                            final boolean indoor,
                            final String name,
                            final Line.SportPart sport) {
        handler.startSession();
        handler.handle(lineDigestWrite(activityRef(uuid, false)));

        // POINTS first.
        final long pos = indoor ? sentinelPosition() : packPosition(
                /*lat=45°*/ (int) (45.0 / (180.0 / 0x80000000L)),
                /*lon=10°*/ (int) (10.0 / (180.0 / 0x80000000L)));
        handler.handle(pointsReply(uuid,
                ReadStatus.READ_STATUS_SUCCESS, 0,
                new int[]{t0, t0 + 60},
                new long[]{pos, pos},
                new byte[]{(byte) 120, (byte) 125}));

        // SUMMARY with a creation_time at activity-end+10s to mimic
        // the real watch (which finalizes the SummaryPart after the
        // session ends, not at start).
        handler.handle(summaryReply(uuid, name, t0 + 60 + 10));

        // STAT reply — explicit default so hasStatPart() is true and
        // the handler advances past STAT.
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setStatPart(Line.StatPart.getDefaultInstance())))
                .build());

        // SPORT reply — caller's sport or explicit default.
        handler.handle(ExploreSyncService.newBuilder()
                .setLineDataReadyRequest(LineDataReadyRequest.newBuilder()
                        .setStatus(ReadStatus.READ_STATUS_SUCCESS)
                        .setLine(Line.newBuilder()
                                .setUuid(uuid).setLineType(LineType.LINE_TYPE_ACTIVITY)
                                .setSportPart(sport != null ? sport : Line.SportPart.getDefaultInstance())))
                .build());
    }

    private BaseActivitySummary findSummary(final int t0Seconds) {
        return ActivitySummaryParser.findBaseActivitySummary(
                daoSession, support.getDevice(), (long) t0Seconds);
    }

    /** Bare-bones GarminSupport that records outgoing protobuf requests
     *  instead of dispatching them over BLE. */
    private static class RecordingGarminSupport extends GarminSupport {
        final List<GdiSmartProto.Smart> outgoing = new ArrayList<>();

        @Override
        public BluetoothGattCharacteristic getCharacteristic(final UUID uuid) {
            return new BluetoothGattCharacteristic(null, 0, 0);
        }

        @Override
        void sendProtobufRequest(final String taskName, final GdiSmartProto.Smart payload) {
            outgoing.add(payload);
        }

        @Override
        public java.io.File getWritableExportDirectory() throws java.io.IOException {
            // Robolectric has no real coordinator setup; route GPX writes
            // through the JVM tmpdir so flush() can complete.
            final java.io.File dir = new java.io.File(
                    System.getProperty("java.io.tmpdir"), "gb-explore-test");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new java.io.IOException("mkdirs failed: " + dir);
            }
            return dir;
        }
    }
}
