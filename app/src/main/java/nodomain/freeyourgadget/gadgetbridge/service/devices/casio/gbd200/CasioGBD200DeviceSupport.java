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

import android.Manifest;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.gbx100.CasioGBX100SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CasioGBX100ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOLIGHT;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_AUTOREMOVE_MESSAGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_CALENDAR;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_CALL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_EMAIL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_OTHER;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_CASIO_ALERT_SMS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FAKE_RING_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_FIND_PHONE_DURATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_KEY_VIBRATION;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OPERATING_SOUNDS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_PREVIEW_MESSAGE_IN_TITLE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_TIMEFORMAT;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_ACTIVETIME_MINUTES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DATE_OF_BIRTH;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_DISTANCE_METERS;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_GENDER;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_HEIGHT_CM;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_STEPS_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivityUser.PREF_USER_WEIGHT_KG;

public class CasioGBD200DeviceSupport extends Casio2C2DSupport
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(CasioGBD200DeviceSupport.class);

    private boolean mGetConfigurationPending = false;
    private boolean mRingNotificationPending = false;
    private boolean mNeedsGetConfiguration = false;
    private boolean mResyncPending = false;
    private final ArrayList<Integer> mSyncedNotificationIDs = new ArrayList<>();
    private int mLastCallId = (int) (System.currentTimeMillis() / 1000) + 1;
    private int mFakeRingDurationCounter = 0;
    private final Handler mFindPhoneHandler = new Handler(Looper.getMainLooper());
    private final Handler mFakeRingDurationHandler = new Handler(Looper.getMainLooper());
    private final Handler mAutoRemoveMessageHandler = new Handler(Looper.getMainLooper());
    private final Handler mReconnectHandler = new Handler(Looper.getMainLooper());

    public CasioGBD200DeviceSupport() {
        super(LOG);
    }

    // ── GPS ───────────────────────────────────────────────────────────────────

    /** Returns [lat, lon, alt] from the last known location, or [0, 0, 0]. */
    public double[] getLastKnownLocation() {
        try {
            LocationManager lm = (LocationManager)
                    getContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            if (lm == null) return new double[]{0, 0, 0};
            if (ActivityCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                LOG.warn("No location permission; using 0,0,0 for GPS");
                return new double[]{0, 0, 0};
            }
            Location loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) return new double[]{0, 0, 0};
            return new double[]{loc.getLatitude(), loc.getLongitude(), loc.getAltitude()};
        } catch (Exception e) {
            LOG.warn("getLastKnownLocation failed: {}", e.getMessage());
            return new double[]{0, 0, 0};
        }
    }

    // ── Initialization ────────────────────────────────────────────────────────

    private void resetState() {
        mFindPhoneHandler.removeCallbacksAndMessages(null);
        mFakeRingDurationHandler.removeCallbacksAndMessages(null);
        mAutoRemoveMessageHandler.removeCallbacksAndMessages(null);
        mReconnectHandler.removeCallbacksAndMessages(null);

        mGetConfigurationPending = false;
        mRingNotificationPending = false;
        mResyncPending = false;
        mFakeRingDurationCounter = 0;
        mSyncedNotificationIDs.clear();
    }

    @Override
    public void dispose() {
        resetState();
        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        resetState();

        try {
            new InitOperation(this, builder, mFirstConnect, mNeedsGetConfiguration).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Casio GBD-200 failed",
                    Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        return builder;
    }

    // ── Time sync ─────────────────────────────────────────────────────────────

    /**
     * Override to use the correct DOW encoding: Sunday=0, Monday=1 … Saturday=6.
     * The standard BLETypeConversions uses DayOfWeek.getValue() (Mon=1…Sun=7) which
     * is wrong for this protocol.
     */
    @Override
    public void writeCurrentTime(TransactionBuilder builder, ZonedDateTime time) {
        int dow = time.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : time.getDayOfWeek().getValue();
        byte[] arr = new byte[11];
        arr[0]  = FEATURE_CURRENT_TIME;
        arr[1]  = (byte) (time.getYear() & 0xff);
        arr[2]  = (byte) ((time.getYear() >> 8) & 0xff);
        arr[3]  = (byte) time.getMonthValue();
        arr[4]  = (byte) time.getDayOfMonth();
        arr[5]  = (byte) time.getHour();
        arr[6]  = (byte) time.getMinute();
        arr[7]  = (byte) time.getSecond();
        arr[8]  = (byte) dow;
        arr[9]  = 0x00; // fractions256
        arr[10] = 0x01; // reason = manual sync
        writeAllFeatures(builder, arr);
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("onSetTime");
            writeCurrentTime(builder, ZonedDateTime.now());
            builder.queue();
        } catch (IOException e) {
            LOG.warn("onSetTime failed: {}", e.getMessage());
        }
    }

    // ── Config sync ───────────────────────────────────────────────────────────

    @Override
    public void syncProfile() {
        try {
            new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                    .SetConfigurationOperation(this,
                    CasioConstants.ConfigurationOption.OPTION_ALL).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Sending Casio configuration failed",
                    Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    // ── Main-menu resync (3d 01) ─────────────────────────────────────────────

    /**
     * When the user navigates to the watch main menu, it sends BLE_PARAM (3d 01) and resets
     * its CONVOY state. We must toggle CCCDs and run a dummy steps fetch to warm the state
     * machine before the next sport or steps command.
     */
    private void handleMainMenuResync() {
        LOG.info("Main-menu resync triggered (3d 01)");
        mResyncPending = true;
        try {
            TransactionBuilder b1 = performInitialized("resync_cccd_off");
            b1.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, false);
            b1.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, false);
            b1.queue();

            TransactionBuilder b2 = performInitialized("resync_cccd_on");
            b2.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, true);
            b2.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, true);
            b2.writeLegacy(
                    getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID),
                    new byte[]{0x00, FEATURE_SETTING_FOR_BLE, 0x00, 0x00, 0x00});
            b2.queue();
        } catch (IOException e) {
            mResyncPending = false;
            LOG.error("handleMainMenuResync failed: {}", e.getMessage());
        }
    }

    // ── Characteristic change handler ─────────────────────────────────────────

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        UUID uuid = characteristic.getUuid();
        if (data == null || data.length == 0) return true;

        if (uuid.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            byte feat = data[0];

            if (feat == FEATURE_ALERT_LEVEL) {
                // Phone finder: 0x02 = start, other = stop
                onReverseFindDevice(data.length > 1 && data[1] == 0x02);
                return true;
            }

            if (feat == FEATURE_CURRENT_TIME_MANAGER && data.length > 1 && data[1] == 0x00) {
                // Watch requests time resync
                try {
                    TransactionBuilder b = performInitialized("writeCurrentTime");
                    writeCurrentTime(b, ZonedDateTime.now());
                    b.queue();
                } catch (IOException e) {
                    LOG.warn("Time resync failed: {}", e.getMessage());
                }
                return true;
            }

            if (feat == FEATURE_BLE_PARAM && data.length > 1 && data[1] == 0x01) {
                // Watch navigated to main menu → must resync CONVOY state
                handleMainMenuResync();
                return true;
            }

            if (feat == FEATURE_WATCH_CONDITION) {
                if (data.length >= 9) {
                    if (data[7] == 0x01) {
                        LOG.info("Session saved on watch");
                    } else {
                        LOG.info("Session discarded on watch — OpenTracks recording already stopped");
                    }
                }
                return true;
            }

            if (feat == FEATURE_SESSION_EVENT && data.length > 1) {
                if (data[1] == 0x00) {
                    // Running session started on watch
                    LOG.info("Running session started on watch (0x48 0x00)");
                    try {
                        TransactionBuilder b = performInitialized("session_event_ack");
                        writeAllFeatures(b, new byte[]{
                                FEATURE_SESSION_EVENT, 0x03, 0x00,
                                (byte) 0xc8, 0x00,          // GPS rate 200
                                0x14, 0x0a,                 // thresholds
                                0x00, 0x00,                 // elapsed_s (0 at start)
                                0x34, 0x01,                 // target pace (308 s/km)
                                0x40, 0x01,                 // target step count (320)
                                0x01, 0x00,
                                (byte) 0xdc, 0x05           // target distance (1500 m)
                        });
                        b.queue();
                    } catch (IOException e) {
                        LOG.warn("session_event_ack failed: {}", e.getMessage());
                    }
                    OpenTracksController.startRecording(getContext(), ActivityKind.RUNNING);
                } else if (data[1] == 0x01) {
                    LOG.info("Running session ended on watch (0x48 0x01)");
                    OpenTracksController.stopRecording(getContext());
                }
                return true;
            }
        }

        // Warm-up steps ACK arrives on DATA_REQ_SP (h0011), not ALL_FEATURES.
        // Only handle when a resync was explicitly triggered — not during normal step fetches.
        if (mResyncPending
                && uuid.equals(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID)
                && data.length >= 2 && data[0] == 0x00 && data[1] == FEATURE_SETTING_FOR_BLE) {
            mResyncPending = false;
            try {
                TransactionBuilder b = performInitialized("resync_steps_ack");
                b.writeLegacy(
                        getCharacteristic(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID),
                        new byte[]{0x04, FEATURE_SETTING_FOR_BLE, 0x00, 0x00, 0x00});
                b.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, false);
                b.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, false);
                b.notify(CasioConstants.CASIO_DATA_REQUEST_SP_CHARACTERISTIC_UUID, true);
                b.notify(CasioConstants.CASIO_CONVOY_CHARACTERISTIC_UUID, true);
                b.queue();
            } catch (IOException e) {
                LOG.warn("resync_steps_ack failed: {}", e.getMessage());
            }
            return true;
        }

        LOG.info("Unhandled characteristic change: {} feat=0x{}", uuid,
                String.format("%02x", data[0]));
        return super.onCharacteristicChanged(gatt, characteristic, data);
    }

    // ── Data fetch ────────────────────────────────────────────────────────────

    @Override
    public void onFetchRecordedData(int dataTypes) {
        try {
            new FetchStepCountDataOperation(this).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching step data",
                    Toast.LENGTH_SHORT, GB.ERROR, e);
            // Still attempt sport fetch even if step count fails
            startFetchSportData();
        }
    }

    /** Called by FetchStepCountDataOperation when it finishes, to chain the sport fetch. */
    public void onStepCountFetchFinished() {
        startFetchSportData();
    }

    private void startFetchSportData() {
        try {
            new FetchSportDataOperation(this).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Error fetching sport data",
                    Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    // ── Activity DB helpers (used by FetchStepCountDataOperation) ─────────────

    public CasioGBX100ActivitySample getSumWithinRange(int ts_from, int ts_to) {
        int steps = 0, calories = 0;
        try (DBHandler db = GBApplication.acquireDB()) {
            User user     = DBHelper.getUser(db.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), db.getDaoSession());
            CasioGBX100SampleProvider provider =
                    new CasioGBX100SampleProvider(this.getDevice(), db.getDaoSession());
            List<CasioGBX100ActivitySample> samples =
                    provider.getActivitySamples(ts_from, ts_to);
            for (CasioGBX100ActivitySample s : samples) {
                if (s.getDevice().equals(device) && s.getUser().equals(user)) {
                    steps    += s.getSteps();
                    calories += s.getCalories();
                }
            }
        } catch (Exception e) {
            LOG.error("Error fetching step sum: {}", e.getMessage());
        }
        CasioGBX100ActivitySample ret = new CasioGBX100ActivitySample();
        ret.setSteps(steps);
        ret.setCalories(calories);
        return ret;
    }

    public void stepCountDataFetched(int totalSteps, int totalCalories,
                                     ArrayList<CasioGBX100ActivitySample> data) {
        LOG.info("Steps fetched: total={} kcal={}", totalSteps, totalCalories);
        try (DBHandler db = GBApplication.acquireDB()) {
            User user     = DBHelper.getUser(db.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), db.getDaoSession());
            CasioGBX100SampleProvider provider =
                    new CasioGBX100SampleProvider(this.getDevice(), db.getDaoSession());
            for (CasioGBX100ActivitySample s : data) {
                s.setDevice(device);
                s.setUser(user);
                s.setProvider(provider);
                provider.addGBActivitySample(s);
            }
        } catch (Exception e) {
            GB.toast(getContext(), "Error saving steps: " + e.getLocalizedMessage(),
                    Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private void showNotification(byte icon, String sender, String title,
                                  String subtitle, String message, int id, boolean delete) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(
                getDevice().getAddress());
        boolean showPreview = prefs.getBoolean(PREF_PREVIEW_MESSAGE_IN_TITLE, true);

        boolean shouldAlert = true;
        switch (icon) {
            case CasioConstants.CATEGORY_EMAIL:
                shouldAlert = prefs.getBoolean(PREF_CASIO_ALERT_EMAIL, true); break;
            case CasioConstants.CATEGORY_OTHER:
                shouldAlert = prefs.getBoolean(PREF_CASIO_ALERT_OTHER, true); break;
            case CasioConstants.CATEGORY_SNS:
                shouldAlert = prefs.getBoolean(PREF_CASIO_ALERT_SMS, true); break;
            case CasioConstants.CATEGORY_INCOMING_CALL:
                shouldAlert = prefs.getBoolean(PREF_CASIO_ALERT_CALL, true); break;
            case CasioConstants.CATEGORY_SCHEDULE_AND_ALARM:
                shouldAlert = prefs.getBoolean(PREF_CASIO_ALERT_CALENDAR, true); break;
        }

        if (showPreview && icon != CasioConstants.CATEGORY_INCOMING_CALL
                && icon != CasioConstants.CATEGORY_EMAIL) {
            if (StringUtils.isNullOrEmpty(sender)) sender = title;
            if (!StringUtils.isNullOrEmpty(message))
                title = message.substring(0, Math.min(message.length(), 18)) + "..";
        }

        byte[] titleBytes = StringUtils.isNullOrEmpty(title) ? new byte[0]
                : truncate(title, 32).getBytes(StandardCharsets.UTF_8);
        byte[] senderBytes = StringUtils.isNullOrEmpty(sender) ? new byte[0]
                : truncate(sender, 32).getBytes(StandardCharsets.UTF_8);
        byte[] subtitleBytes = StringUtils.isNullOrEmpty(subtitle) ? new byte[0]
                : StringUtils.truncateToBytes(subtitle, 32);
        byte[] messageBytes = StringUtils.isNullOrEmpty(message) ? new byte[0]
                : StringUtils.truncateToBytes(message, 250);

        byte[] hdr = new byte[22];
        hdr[0] = (byte) (id & 0xff);
        hdr[1] = (byte) ((id >> 8) & 0xff);
        hdr[2] = (byte) ((id >> 16) & 0xff);
        hdr[3] = (byte) ((id >> 24) & 0xff);
        hdr[4] = delete ? (byte) 0x02 : (byte) 0x00;
        hdr[5] = shouldAlert ? (byte) 0x01 : (byte) 0x00;
        hdr[6] = icon;
        String ts = new SimpleDateFormat("yyyyMMdd'T'HHmmss").format(new Date());
        System.arraycopy(ts.getBytes(), 0, hdr, 7, 15);

        byte[] pkt = tlvConcat(hdr, senderBytes, titleBytes, subtitleBytes, messageBytes);
        // XOR all bytes with 0xFF
        for (int i = 0; i < pkt.length; i++) pkt[i] = (byte) (~pkt[i]);

        try {
            TransactionBuilder b = performInitialized("showNotification");
            b.writeLegacy(getCharacteristic(CasioConstants.CASIO_NOTIFICATION_CHARACTERISTIC_UUID),
                    pkt);
            b.queue();
        } catch (IOException e) {
            LOG.error("showNotification failed: {}", e.getMessage());
        }
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 2) + ".." : s;
    }

    /** Build header + TLV fields (LE16 length + bytes) for each string. */
    private byte[] tlvConcat(byte[] hdr, byte[]... fields) {
        int total = hdr.length;
        for (byte[] f : fields) total += 2 + f.length;
        byte[] out = Arrays.copyOf(hdr, total);
        int pos = hdr.length;
        for (byte[] f : fields) {
            out[pos]     = (byte) (f.length & 0xff);
            out[pos + 1] = (byte) ((f.length >> 8) & 0xff);
            System.arraycopy(f, 0, out, pos + 2, f.length);
            pos += 2 + f.length;
        }
        return out;
    }

    private void showNotification(byte icon, String sender, String title, String message,
                                  int id, boolean delete) {
        showNotification(icon, sender, title, null, message, id, delete);
    }

    @Override
    public void onNotification(final NotificationSpec spec) {
        byte icon;
        boolean autoremove = false;
        switch (spec.type) {
            case GENERIC_CALENDAR:
                icon = CasioConstants.CATEGORY_SCHEDULE_AND_ALARM; break;
            case GENERIC_EMAIL:
            case GMAIL:
            case GOOGLE_INBOX:
                icon = CasioConstants.CATEGORY_EMAIL; break;
            case GENERIC_SMS:
                icon = CasioConstants.CATEGORY_SNS;
                autoremove = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress())
                        .getBoolean(PREF_AUTOREMOVE_MESSAGE, false);
                break;
            case GENERIC_PHONE:
                icon = CasioConstants.CATEGORY_INCOMING_CALL; break;
            default:
                icon = CasioConstants.CATEGORY_OTHER; break;
        }
        showNotification(icon, spec.sender, spec.title, spec.body, spec.getId(), false);
        mSyncedNotificationIDs.add(spec.getId());
        if (autoremove) {
            mAutoRemoveMessageHandler.postDelayed(
                    () -> onDeleteNotification(spec.getId()),
                    CasioConstants.CASIO_AUTOREMOVE_MESSAGE_DELAY);
        }
        if (mSyncedNotificationIDs.size() > 100) mSyncedNotificationIDs.remove(0);
    }

    @Override
    public void onDeleteNotification(int id) {
        if (mSyncedNotificationIDs.contains(id)) {
            showNotification(CasioConstants.CATEGORY_OTHER, null, null, null, id, true);
            mSyncedNotificationIDs.remove(Integer.valueOf(id));
        }
    }

    // ── Calls ─────────────────────────────────────────────────────────────────

    @Override
    public void onSetCallState(final CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(CasioConstants.CATEGORY_INCOMING_CALL,
                        callSpec.name, callSpec.number, "Phone Call", mLastCallId, false);
                SharedPreferences p = GBApplication.getDeviceSpecificSharedPrefs(
                        getDevice().getAddress());
                if (p.getBoolean(PREF_FAKE_RING_DURATION, false)
                        && mFakeRingDurationCounter < CasioConstants.CASIO_FAKE_RING_RETRIES) {
                    mFakeRingDurationCounter++;
                    mFakeRingDurationHandler.postDelayed(() -> {
                        showNotification(CasioConstants.CATEGORY_INCOMING_CALL,
                                null, null, null, mLastCallId, true);
                        onSetCallState(callSpec);
                    }, CasioConstants.CASIO_FAKE_RING_SLEEP_DURATION);
                } else {
                    mFakeRingDurationCounter = 0;
                }
                mRingNotificationPending = true;
                break;
            default:
                if (mRingNotificationPending) {
                    mFakeRingDurationHandler.removeCallbacksAndMessages(null);
                    mFakeRingDurationCounter = 0;
                    showNotification(CasioConstants.CATEGORY_INCOMING_CALL,
                            null, null, null, mLastCallId, true);
                    mLastCallId = (int) (System.currentTimeMillis() / 1000) + 1;
                    mRingNotificationPending = false;
                }
        }
    }

    // ── Phone finder ──────────────────────────────────────────────────────────

    private void onReverseFindDevice(boolean start) {
        if (start) {
            String findPhone = getDevicePrefs().getString(PREF_FIND_PHONE,
                    getContext().getString(R.string.p_off));
            if (findPhone.equals(getContext().getString(R.string.p_off))) return;

            GBDeviceEventFindPhone ev = new GBDeviceEventFindPhone();
            ev.event = GBDeviceEventFindPhone.Event.START;
            evaluateGBDeviceEvent(ev);

            if (findPhone.equals(getContext().getString(R.string.p_on))) {
                int duration = getDevicePrefs().getInt(PREF_FIND_PHONE_DURATION, 0);
                if (duration > 0) {
                    mFindPhoneHandler.postDelayed(() -> {
                        GBDeviceEventFindPhone stop = new GBDeviceEventFindPhone();
                        stop.event = GBDeviceEventFindPhone.Event.STOP;
                        evaluateGBDeviceEvent(stop);
                    }, duration * 1000L);
                }
            }
        } else {
            GBDeviceEventFindPhone ev = new GBDeviceEventFindPhone();
            ev.event = GBDeviceEventFindPhone.Event.STOP;
            evaluateGBDeviceEvent(ev);
        }
    }

    // ── Alarms ────────────────────────────────────────────────────────────────

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        if (!isConnected()) return;

        byte[] data1 = new byte[5];
        byte[] data2 = new byte[17];
        data1[0] = FEATURE_SETTING_FOR_ALM;
        data2[0] = FEATURE_SETTING_FOR_ALM2;

        for (int i = 0; i < alarms.size(); i++) {
            Alarm a = alarms.get(i);
            byte[] s = new byte[4];
            s[0] = a.getEnabled() ? (a.getSnooze() ? (byte) 0x50 : (byte) 0x40) : 0x00;
            s[1] = 0x40;
            s[2] = (byte) a.getHour();
            s[3] = (byte) a.getMinute();
            if (i == 0) System.arraycopy(s, 0, data1, 1, 4);
            else        System.arraycopy(s, 0, data2, 1 + (i - 1) * 4, 4);
        }
        try {
            TransactionBuilder b = performInitialized("setAlarm");
            writeAllFeatures(b, data1);
            writeAllFeatures(b, data2);
            b.queue();
        } catch (IOException e) {
            LOG.error("setAlarm failed: {}", e.getMessage());
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────────

    @Override
    public void onSendConfiguration(String config) {
        onSharedPreferenceChanged(null, config);
    }

    @Override
    public void onGetConfigurationFinished() {
        mGetConfigurationPending = false;
    }

    @Override
    public void onReadConfiguration(String config) {
        if (config == null) {
            try {
                mGetConfigurationPending = true;
                mNeedsGetConfiguration = false;
                new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                        .GetConfigurationOperation(this, true).perform();
            } catch (IOException e) {
                mGetConfigurationPending = false;
                GB.toast(getContext(), "Reading Casio config failed",
                        Toast.LENGTH_SHORT, GB.ERROR, e);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (!isConnected() || mGetConfigurationPending) return;
        if (key == null) return;

        try {
            switch (key) {
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_WRIST).perform(); break;
                case PREF_USER_STEPS_GOAL:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_STEP_GOAL).perform(); break;
                case PREF_USER_ACTIVETIME_MINUTES:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_ACTIVITY_GOAL).perform(); break;
                case PREF_USER_DISTANCE_METERS:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_DISTANCE_GOAL).perform(); break;
                case PREF_USER_GENDER:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_GENDER).perform(); break;
                case PREF_USER_HEIGHT_CM:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_HEIGHT).perform(); break;
                case PREF_USER_WEIGHT_KG:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_WEIGHT).perform(); break;
                case PREF_USER_DATE_OF_BIRTH:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_BIRTHDAY).perform(); break;
                case PREF_TIMEFORMAT:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_TIMEFORMAT).perform(); break;
                case PREF_KEY_VIBRATION:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_KEY_VIBRATION).perform(); break;
                case PREF_AUTOLIGHT:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_AUTOLIGHT).perform(); break;
                case PREF_OPERATING_SOUNDS:
                    new nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gbx100
                            .SetConfigurationOperation(this, CasioConstants.ConfigurationOption.OPTION_OPERATING_SOUNDS).perform(); break;
                default:
                    break;
            }
        } catch (IOException e) {
            LOG.info("onSharedPreferenceChanged: config send failed for {}", key);
        }
    }

    // ── Reconnect helper ──────────────────────────────────────────────────────

    public void reconnectDelayed() {
        setAutoReconnect(true);
        mNeedsGetConfiguration = true;
        mReconnectHandler.postDelayed(this::connect, CasioConstants.CASIO_FAKE_RING_SLEEP_DURATION);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public DevicePreference[] supportedDevicePreferences() {
        return new DevicePreference[]{};
    }
}
