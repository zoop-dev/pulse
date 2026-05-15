/*  Copyright (C) 2026 Davide Gessa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbd200;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryTableBuilder;
import nodomain.freeyourgadget.gadgetbridge.activities.workouts.entries.ActivitySummaryValue;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.ACTIVE_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CADENCE_AVG;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.CALORIES_BURNT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.PACE_AVG_SECONDS_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_KCAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SECONDS_PER_KM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_SPM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.GROUP_LAPS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.UNIT_NONE;

/**
 * Fetches all sport session summaries from a GBD-200 via the CONVOY handshake protocol.
 *
 * Protocol flow (h0011 = DATA_REQ_SP, h0014 = CONVOY):
 *  1.  Enable notifications on h0011 + h0014
 *  2.  Send CONVOY_INIT (00 1c …) to h0011, send ping (00 00 00) to h0014
 *  3.  Wait for ping echo on h0014 (00 00 04 = ready; 00 01 04 = BUSY)
 *  4.  Wait for h0011 echo (00 1c …) — watch loading flash (~8 s)
 *  5.  cap_query / cap_set / init_sig / version exchange on h0014
 *  6.  Request session list (feat 0x1d, addr 0x46a0) via h0011
 *  7.  Collect type-0x05 CONVOY packets; wait for 0x09 DATA_READY on h0011
 *  8.  For each session: request summary (feat 0x1e, addr), collect, parse, save
 *  9.  Close with cancel (03 1c …) on h0011
 */
public class FetchSportDataOperation extends AbstractBTLEOperation<CasioGBD200DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(FetchSportDataOperation.class);

    private static final int SESSION_LIST_BASE = 0x46a0;
    private static final int META_BLOCK_HDR    = 15;
    private static final int META_LAP_STRIDE   = 19;
    private static final int TIMEOUT_MS        = 45_000;

    private final Handler mTimeoutHandler = new Handler(Looper.getMainLooper());

    // State machine
    private enum State {
        WAIT_PING_ECHO,
        WAIT_INIT_ECHO,
        WAIT_CAP_RESPONSE,
        WAIT_CAP_CONFIRM,
        WAIT_VERSION,
        COLLECTING_LIST,
        COLLECTING_SESSION,
        COLLECTING_META
    }

    private State mState = State.WAIT_PING_ECHO;
    private final ArrayList<Byte> mConvoyBuf = new ArrayList<>();
    private int mTotalSessions = 0;
    private int mCurrentSession = 0;   // 1-based
    private int mMetaAddress  = 0;
    private int mSegCount     = 0;
    private BaseActivitySummary mCurrentSummary = null;

    private final CasioGBD200DeviceSupport mSupport;

    public FetchSportDataOperation(CasioGBD200DeviceSupport support) {
        super(support);
        this.mSupport = support;
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    /** Write to DATA_REQUEST_SP (h0011) with WRITE_REQ. */
    private void writeH0011(byte[] data, String label) {
        try {
            TransactionBuilder b = performInitialized(label);
            b.setCallback(this);
            b.writeLegacy(
                    getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID),
                    data);
            b.queue();
        } catch (IOException e) {
            LOG.error("writeH0011 [{}] failed: {}", label, e.getMessage());
        }
    }

    /** Write to CONVOY (h0014) with WRITE_CMD (no response). */
    private void writeH0014(byte[] data, String label) {
        try {
            TransactionBuilder b = performInitialized(label);
            b.setCallback(this);
            b.writeLegacy(
                    getCharacteristic(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID),
                    data);
            b.queue();
        } catch (IOException e) {
            LOG.error("writeH0014 [{}] failed: {}", label, e.getMessage());
        }
    }

    /** feat_req: [op=0x00] [feat] 0x00 [off_lo] [off_hi] 0x00 0x00 [param] 0x00 0x00 */
    private byte[] featReq(int feat, int offset, int param) {
        return new byte[]{
                0x00, (byte) feat, 0x00,
                (byte) (offset & 0xff), (byte) ((offset >> 8) & 0xff),
                0x00, 0x00, (byte) param, 0x00, 0x00
        };
    }

    /** ack: [op=0x04] [feat] 0x00 … (10 bytes total) */
    private byte[] ackReq(int feat) {
        byte[] a = new byte[10];
        a[0] = 0x04;
        a[1] = (byte) feat;
        return a;
    }

    /** Echo first 10 bytes of a DATA_READY (0x09) packet. */
    private byte[] echo10(byte[] pkt) {
        byte[] out = new byte[10];
        System.arraycopy(pkt, 0, out, 0, Math.min(pkt.length, 10));
        return out;
    }

    private void enableNotifications(boolean enable) {
        try {
            TransactionBuilder b = performInitialized("enableSportNotif");
            b.setCallback(this);
            b.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, enable);
            b.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, enable);
            b.queue();
        } catch (IOException e) {
            LOG.error("enableNotifications failed: {}", e.getMessage());
        }
    }

    // ── CONVOY data decoding ─────────────────────────────────────────────────

    /**
     * Decode a type-0x05 CONVOY packet and append the payload to mConvoyBuf.
     * byte[0] (type) is unchanged; bytes[1:] are XOR'd with 0xFF.
     * The first 3 bytes of the decoded packet are the CONVOY header and are skipped.
     */
    private void appendConvoyData(byte[] raw) {
        if (raw.length < 4) return;
        // raw[0] = 0x05 (type, unchanged); raw[1:] XOR'd with 0xFF.
        // The first 3 decoded bytes are a CONVOY header — skip them, accumulate payload only.
        for (int i = 3; i < raw.length; i++) {
            mConvoyBuf.add((byte) (~raw[i] & 0xff));
        }
    }

    private byte[] getConvoyPayload() {
        byte[] out = new byte[mConvoyBuf.size()];
        for (int i = 0; i < out.length; i++) out[i] = mConvoyBuf.get(i);
        return out;
    }

    // ── Session list parsing ──────────────────────────────────────────────────

    private int parseSessionCount(byte[] payload) {
        if (payload.length <= 6) return 0;
        int raw = payload[6] & 0xff;
        int inv = (~raw) & 0xff;
        return Integer.bitCount(inv);
    }

    // ── Session summary parsing and DB save ──────────────────────────────────

    private int bcd(byte b) {
        return ((b >> 4) & 0x0f) * 10 + (b & 0x0f);
    }

    /** Parse 7-byte BCD timestamp starting at payload[offset]. */
    private Date parseBcdTimestamp(byte[] p, int offset) {
        if (offset + 7 > p.length) return new Date(0);
        int yearLo = bcd(p[offset]);
        int yearHi = bcd(p[offset + 1]);
        int year   = yearHi * 100 + yearLo;
        int month  = bcd(p[offset + 2]) - 1; // 0-based
        int day    = bcd(p[offset + 3]);
        int hour   = bcd(p[offset + 4]);
        int min    = bcd(p[offset + 5]);
        int sec    = bcd(p[offset + 6]);
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, min, sec);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    enum SaveResult { NEW, EXISTING, ERROR }

    private SaveResult parseAndSaveSession(byte[] payload, int sessionIndex) {
        mCurrentSummary = null;
        if (payload.length < 186) {
            LOG.warn("Session {} payload too short: {} bytes", sessionIndex, payload.length);
            mMetaAddress = 0;
            mSegCount    = 0;
            return SaveResult.ERROR;
        }

        // All offsets are direct indices into payload[] (0-based)
        int durationMin = payload[174] & 0xff;
        int durationSec = payload[175] & 0xff;
        int avgPaceMin  = payload[176] & 0xff;
        int avgPaceSec  = payload[177] & 0xff;
        int kcal        = payload[178] & 0xff;
        int cadence     = payload[182] & 0xff;
        int segCount    = payload[142] & 0xff;
        // Meta block address for per-lap data (feat 0x20)
        mMetaAddress = (payload[166] & 0xff) | ((payload[167] & 0xff) << 8);
        mSegCount    = segCount;

        float distKm = ByteBuffer.wrap(payload, 169, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getFloat();

        Date startTime = parseBcdTimestamp(payload, 147);
        Date endTime   = parseBcdTimestamp(payload, 154);

        int totalSeconds = durationMin * 60 + durationSec;
        int avgPaceSeconds = avgPaceMin * 60 + avgPaceSec;

        LOG.info("Session {}: duration={}m{}s dist={}km kcal={} pace={}'{} cad={}",
                sessionIndex, durationMin, durationSec, distKm, kcal, avgPaceMin, avgPaceSec, cadence);

        ActivitySummaryData sd = new ActivitySummaryData();
        sd.add(ACTIVE_SECONDS,       totalSeconds,           UNIT_SECONDS);
        sd.add(DISTANCE_METERS,      distKm * 1000.0f,       UNIT_METERS);
        sd.add(CALORIES_BURNT,       kcal,                   UNIT_KCAL);
        sd.add(CADENCE_AVG,          cadence,                UNIT_SPM);
        if (avgPaceSeconds > 0) {
            sd.add(PACE_AVG_SECONDS_KM, avgPaceSeconds,      UNIT_SECONDS_PER_KM);
        }

        BaseActivitySummary summary = new BaseActivitySummary();
        summary.setName(ActivityKind.RUNNING.getLabel(getContext()));
        summary.setStartTime(startTime.getTime() == 0 ? new Date() : startTime);
        summary.setEndTime(endTime.getTime() == 0 ? new Date(startTime.getTime() + totalSeconds * 1000L) : endTime);
        summary.setActivityKind(ActivityKind.RUNNING.getCode());
        summary.setSummaryData(sd.toString());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);
            summary.setDevice(device);
            summary.setUser(user);

            // Avoid duplicates: reuse the existing row's ID if the same session was already saved
            java.util.List<BaseActivitySummary> existing = session.getBaseActivitySummaryDao()
                    .queryBuilder()
                    .where(
                            BaseActivitySummaryDao.Properties.DeviceId.eq(device.getId()),
                            BaseActivitySummaryDao.Properties.StartTime.eq(summary.getStartTime()))
                    .list();
            if (!existing.isEmpty()) {
                // Preserve the existing row as-is (it already has lap data in summaryData)
                mCurrentSummary = existing.get(0);
                LOG.info("Sport session {} already in database — skipping overwrite", sessionIndex);
                return SaveResult.EXISTING;
            }

            session.getBaseActivitySummaryDao().insertOrReplace(summary);
            mCurrentSummary = summary;
            LOG.info("Saved sport session {} to database (new)", sessionIndex);
            return SaveResult.NEW;
        } catch (Exception e) {
            GB.toast(getContext(), "Error saving sport session: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG, GB.ERROR, e);
        }
        return SaveResult.ERROR;
    }

    private void parseAndSaveMetaLaps(byte[] metaPayload) {
        if (mCurrentSummary == null || mSegCount <= 0) return;

        // appendConvoyData already strips the 3-byte CONVOY header; laps start at META_BLOCK_HDR
        int lapBase = META_BLOCK_HDR;

        ActivitySummaryTableBuilder tableBuilder = new ActivitySummaryTableBuilder(
                GROUP_LAPS, "laps_header",
                Arrays.asList("workout_lap", "distanceMeters", "lap_time", "averagePace",
                        "caloriesBurnt", "averageCadence"));

        for (int s = 0; s < mSegCount; s++) {
            int base = lapBase + s * META_LAP_STRIDE;
            if (base + META_LAP_STRIDE > metaPayload.length) break;

            float distKm  = ByteBuffer.wrap(metaPayload, base, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
            int durSec    = (metaPayload[base + 8] & 0xff) * 60 + (metaPayload[base + 9] & 0xff);
            int paceSec   = (metaPayload[base + 10] & 0xff) * 60 + (metaPayload[base + 11] & 0xff);
            int lapKcal   = metaPayload[base + 12] & 0xff;
            int lapCad    = metaPayload[base + 16] & 0xff;

            tableBuilder.addRow("lap_" + (s + 1), Arrays.asList(
                    new ActivitySummaryValue(s + 1,             UNIT_NONE),
                    new ActivitySummaryValue(distKm * 1000f,    UNIT_METERS),
                    new ActivitySummaryValue(durSec,            UNIT_SECONDS),
                    new ActivitySummaryValue(paceSec > 0 ? paceSec : null, UNIT_SECONDS_PER_KM),
                    new ActivitySummaryValue(lapKcal > 0 ? lapKcal : null, UNIT_KCAL),
                    new ActivitySummaryValue(lapCad  > 0 ? lapCad  : null, UNIT_SPM)
            ));
        }

        if (!tableBuilder.hasRows()) return;

        ActivitySummaryData sd = ActivitySummaryData.fromJson(mCurrentSummary.getSummaryData());
        tableBuilder.addToSummaryData(sd);
        mCurrentSummary.setSummaryData(sd.toString());

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Device device = DBHelper.getDevice(getDevice(), session);
            User user = DBHelper.getUser(session);
            mCurrentSummary.setDevice(device);
            mCurrentSummary.setUser(user);
            session.getBaseActivitySummaryDao().insertOrReplace(mCurrentSummary);
            LOG.info("Updated session {} with {} laps", mCurrentSession, mSegCount);
        } catch (Exception e) {
            LOG.error("Error saving lap data: {}", e.getLocalizedMessage());
        }
    }

    // ── Operation lifecycle ────────────────────────────────────────────────────

    @Override
    protected void prePerform() throws IOException {
        super.prePerform();
        getDevice().setBusyTask(R.string.busy_task_fetch_activity_data, getContext());
        GB.updateTransferNotification(null,
                getContext().getString(R.string.busy_task_fetch_activity_data), true, 0,
                getContext());
    }

    @Override
    protected void doPerform() throws IOException {
        enableNotifications(true);
        mState = State.WAIT_PING_ECHO;
        mConvoyBuf.clear();

        mTimeoutHandler.postDelayed(this::onTimeout, TIMEOUT_MS);

        // CONVOY_INIT request to h0011
        writeH0011(featReq(0x1c, 0, 0), "convoy_init");
        // ping to h0014
        writeH0014(new byte[]{0x00, 0x00, 0x00}, "ping");
    }

    private void onTimeout() {
        LOG.warn("FetchSportDataOperation timed out in state {}", mState);
        GB.toast(getContext(), getContext().getString(R.string.busy_task_fetch_activity_data)
                + ": timeout", Toast.LENGTH_SHORT, GB.WARN);
        enableNotifications(false);
        operationFinished();
    }

    @Override
    protected void operationFinished() {
        if (operationStatus == OperationStatus.FINISHED) return;
        mTimeoutHandler.removeCallbacksAndMessages(null);
        LOG.info("FetchSportDataOperation finished");
        unsetBusy();
        GB.updateTransferNotification(null,
                getContext().getString(R.string.busy_task_fetch_activity_data), false, 100,
                getContext());
        GB.signalActivityDataFinish(getDevice());

        operationStatus = OperationStatus.FINISHED;
        if (getDevice() != null) {
            try {
                TransactionBuilder b = performInitialized("finished");
                b.setCallback(null);
                b.wait(0);
                b.queue();
            } catch (IOException ex) {
                LOG.error("Error resetting Gatt callback", ex);
            }
        }
        // Push any pending settings changes to the watch now that data fetch is done.
        // This replaces the syncProfile() call that used to run during init (which blocked
        // auto-fetch by marking the device "Configuring" before fetch could start).
        mSupport.syncProfile();
    }

    private void proceedToNextSessionOrClose() {
        mCurrentSession++;
        if (mCurrentSession > mTotalSessions) {
            LOG.info("All sessions fetched");
            closeSession();
        } else {
            mConvoyBuf.clear();
            mState = State.COLLECTING_SESSION;
            int addr = SESSION_LIST_BASE + 0x40 + mCurrentSession;
            LOG.debug("Requesting session {}/{} @ 0x{}", mCurrentSession, mTotalSessions,
                    Integer.toHexString(addr));
            int progress = 40 + (int) ((float) mCurrentSession / mTotalSessions * 55);
            GB.updateTransferNotification(null,
                    getContext().getString(R.string.busy_task_fetch_activity_data),
                    true, progress, getContext());
            writeH0011(featReq(0x1e, addr, 0x01), "session_request_" + mCurrentSession);
        }
    }

    private void closeSession() {
        LOG.debug("Closing sport session");
        writeH0011(new byte[]{0x03, 0x1c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                "close_convoy");
        enableNotifications(false);
        operationFinished();
    }

    // ── Characteristic callbacks ────────────────────────────────────────────────

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        UUID uuid = characteristic.getUuid();
        if (data == null || data.length == 0) return true;

        if (uuid.equals(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID)) {
            return onConvoyChanged(data);
        } else if (uuid.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)) {
            return onH0011Changed(data);
        } else if (uuid.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            // BUSY recovery: watch sends WATCH_COND with byte[7]==0x01 when ready after BUSY
            if (mState == State.WAIT_PING_ECHO && data[0] == 0x28
                    && data.length >= 8 && data[7] == 0x01) {
                LOG.debug("BUSY recovery: WATCH_COND ready");
                enableNotifications(false);
                enableNotifications(true);
                mConvoyBuf.clear();
                mState = State.WAIT_PING_ECHO;
                writeH0011(featReq(0x1c, 0, 0), "convoy_init_retry");
                writeH0014(new byte[]{0x00, 0x00, 0x00}, "ping_retry");
            }
            return true;
        }

        return super.onCharacteristicChanged(gatt, characteristic, data);
    }

    /** Handle CONVOY (h0014) notifications. */
    private boolean onConvoyChanged(byte[] data) {
        byte type = data[0];

        switch (mState) {
            case WAIT_PING_ECHO:
                if (type == 0x00) {
                    if (data.length >= 2 && data[1] == 0x01) {
                        // Watch is BUSY — cancel and wait for WATCH_COND ready
                        LOG.warn("Watch BUSY — cancelling, waiting for WATCH_COND ready");
                        writeH0014(new byte[]{0x03, 0x00}, "convoy_cancel_busy");
                        writeH0011(new byte[]{0x03, 0x1c, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                                "cancel_init_busy");
                        // Stay in WAIT_PING_ECHO, handle WATCH_COND in ALL_FEATURES handler
                    } else {
                        // Ping echo received (00 00 04), now wait for h0011 init echo
                        LOG.debug("Ping echo received, waiting for h0011 init echo");
                        mState = State.WAIT_INIT_ECHO;
                    }
                }
                break;

            case WAIT_CAP_RESPONSE:
                if (type == 0x04) {
                    LOG.debug("Cap response received, sending cap_set");
                    mState = State.WAIT_CAP_CONFIRM;
                    writeH0014(
                            new byte[]{0x04, 0x01, 0x18, 0x00, 0x18, 0x00, 0x00, 0x00, (byte) 0xdc, 0x05},
                            "cap_set");
                }
                break;

            case WAIT_CAP_CONFIRM:
                if (type == 0x04) {
                    LOG.debug("Cap confirm received, sending init_sig");
                    mState = State.WAIT_VERSION;
                    writeH0014(new byte[]{0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                            "init_sig");
                }
                break;

            case WAIT_VERSION:
                if (type == 0x06) {
                    LOG.debug("Version received, echoing back");
                    writeH0014(data.clone(), "version_echo");
                    // Immediately request session list
                    mState = State.COLLECTING_LIST;
                    mConvoyBuf.clear();
                    GB.updateTransferNotification(null,
                            getContext().getString(R.string.busy_task_fetch_activity_data),
                            true, 20, getContext());
                    writeH0011(featReq(0x1d, SESSION_LIST_BASE, 0x01), "list_request");
                }
                break;

            case COLLECTING_LIST:
                if (type == 0x05) {
                    appendConvoyData(data);
                }
                break;

            case COLLECTING_SESSION:
            case COLLECTING_META:
                if (type == 0x05) {
                    appendConvoyData(data);
                }
                break;

            default:
                break;
        }
        return true;
    }

    /** Handle DATA_REQ_SP (h0011) notifications. */
    private boolean onH0011Changed(byte[] data) {
        if (data.length < 2) return true;

        switch (mState) {
            case WAIT_INIT_ECHO:
                // Watch finished loading flash; now proceed with cap_query
                if (data[0] == 0x00 && data[1] == 0x1c) {
                    LOG.debug("h0011 init echo received, sending cap_query");
                    mState = State.WAIT_CAP_RESPONSE;
                    writeH0014(new byte[]{0x04, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00},
                            "cap_query");
                }
                break;

            case COLLECTING_LIST:
                // DATA_READY signal (0x09)
                if (data[0] == 0x09) {
                    byte[] payload = getConvoyPayload();
                    mTotalSessions = parseSessionCount(payload);
                    LOG.info("Session list received: {} sessions", mTotalSessions);

                    writeH0011(echo10(data), "echo_list_signal");
                    writeH0011(ackReq(0x1d), "ack_list");

                    if (mTotalSessions == 0) {
                        LOG.info("No sessions to fetch");
                        closeSession();
                    } else {
                        mCurrentSession = 1;
                        mConvoyBuf.clear();
                        mState = State.COLLECTING_SESSION;
                        int addr = SESSION_LIST_BASE + 0x40 + mCurrentSession;
                        LOG.debug("Requesting session {}/{} @ 0x{}", mCurrentSession, mTotalSessions,
                                Integer.toHexString(addr));
                        GB.updateTransferNotification(null,
                                getContext().getString(R.string.busy_task_fetch_activity_data),
                                true, 40, getContext());
                        writeH0011(featReq(0x1e, addr, 0x01), "session_request_1");
                    }
                }
                break;

            case COLLECTING_SESSION:
                if (data[0] == 0x09) {
                    byte[] payload = getConvoyPayload();
                    LOG.debug("Session {}/{} payload: {} bytes", mCurrentSession, mTotalSessions,
                            payload.length);
                    SaveResult result = parseAndSaveSession(payload, mCurrentSession);

                    writeH0011(echo10(data), "echo_session_signal");
                    writeH0011(ackReq(0x1e), "ack_session");

                    int progress = 40 + (int) ((float) mCurrentSession / mTotalSessions * 55);
                    GB.updateTransferNotification(null,
                            getContext().getString(R.string.busy_task_fetch_activity_data),
                            true, progress, getContext());

                    if (result == SaveResult.EXISTING) {
                        // Session already in DB — lap data is already saved, skip meta download.
                        proceedToNextSessionOrClose();
                    } else if (result == SaveResult.NEW) {
                        if (mMetaAddress != 0 && mMetaAddress != 0xffff && mSegCount > 0) {
                            mConvoyBuf.clear();
                            mState = State.COLLECTING_META;
                            LOG.debug("Requesting meta @ 0x{} for session {}", Integer.toHexString(mMetaAddress), mCurrentSession);
                            writeH0011(featReq(0x20, mMetaAddress, 0x01), "meta_request_" + mCurrentSession);
                        } else {
                            proceedToNextSessionOrClose();
                        }
                    } else {
                        proceedToNextSessionOrClose();
                    }
                }
                break;

            case COLLECTING_META:
                if (data[0] == 0x09 || data[0] == 0x07) {
                    parseAndSaveMetaLaps(getConvoyPayload());
                    writeH0011(echo10(data), "echo_meta_signal");
                    writeH0011(ackReq(0x20), "ack_meta");
                    proceedToNextSessionOrClose();
                }
                break;

            default:
                break;
        }
        return true;
    }
}
