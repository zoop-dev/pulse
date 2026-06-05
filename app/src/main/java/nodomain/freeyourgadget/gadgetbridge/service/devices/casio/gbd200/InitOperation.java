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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.Casio2C2DSupport;

/**
 * Full init handshake for GBD-200, following the exact protocol sequence confirmed
 * from btsnoop HCI captures. The watch only shows "connection ok" after this completes.
 *
 * Sequence:
 *  1.  Request APP_INFO (0x22) → write back APP_INFO (announces capabilities, incl. time-sync support)
 *  2.  Request BLE_FEAT (0x10)
 *  3.  Write WATCH_NAME to ALL_FEAT (identity confirm)
 *  4.  Request MODULE_ID (0x26)
 *  5.  WATCH_COND + VER_INFO x 2 rounds, then one final WATCH_COND
 *  6.  Request DST_WATCH_STATE (0x1d) → echo back
 *  7.  Request DST_SETTING slot 0 → save; request slot 1 → echo both
 *  8.  Write GPS chunks (0x24)
 *  9.  Request WORLD_CITY slot 0 → save; request slot 1 → echo both
 *  10. Request FEAT_2F (0x2f) → echo back
 *  11. Request USER_PROF (0x45) → echo back
 *  12. Write CURRENT_TIME (0x09)
 *  13. Wait for 47 01 (INITIALIZED)
 *  14. Post-init reads: WATCH_COND, BASIC (0x13), VER_INFO, WATCH_COND
 */
public class InitOperation extends AbstractBTLEOperation<CasioGBD200DeviceSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    // Linear init state machine
    private static final int S_APP_INFO       = 0;
    private static final int S_BLE_FEAT       = 1;
    private static final int S_MODULE_ID      = 2;
    private static final int S_WATCH_COND_1   = 3;
    private static final int S_VER_INFO_1     = 4;
    private static final int S_WATCH_COND_2   = 5;
    private static final int S_VER_INFO_2     = 6;
    private static final int S_WATCH_COND_3   = 7;
    private static final int S_DST_WATCH      = 8;
    private static final int S_DST_SETTING_0  = 9;
    private static final int S_DST_SETTING_1  = 10;
    private static final int S_WORLD_CITY_0   = 11;
    private static final int S_WORLD_CITY_1   = 12;
    private static final int S_FEAT_2F        = 13;
    private static final int S_USER_PROF      = 14;
    private static final int S_WAIT_INIT      = 15;
    private static final int S_POST_COND_1    = 16;
    private static final int S_POST_BASIC     = 17;
    private static final int S_POST_VER_INFO  = 18;
    private static final int S_POST_COND_2    = 19;

    private int mState = S_APP_INFO;
    private byte[] mDstSetting0 = null;
    private byte[] mWorldCity0 = null;

    private final TransactionBuilder mBuilder;
    private final CasioGBD200DeviceSupport mSupport;
    private final boolean mFirstConnect;
    private final boolean mNeedsGetConfiguration;

    public InitOperation(CasioGBD200DeviceSupport support, TransactionBuilder builder,
                         boolean firstConnect, boolean needsGetConfiguration) {
        super(support);
        this.mBuilder = builder;
        this.mSupport = support;
        this.mFirstConnect = firstConnect;
        this.mNeedsGetConfiguration = needsGetConfiguration;
        builder.setCallback(this);
    }

    // ── Write helpers ─────────────────────────────────────────────────────────

    private void req(byte feat) {
        try {
            TransactionBuilder b = createTransactionBuilder("req_" + String.format("%02x", feat));
            b.setCallback(this);
            mSupport.writeAllFeaturesRequest(b, new byte[]{feat});
            b.queueImmediately();
        } catch (IOException e) {
            LOG.error("req failed: {}", e.getMessage());
        }
    }

    private void req(byte feat, byte slot) {
        try {
            TransactionBuilder b = createTransactionBuilder("req_" + String.format("%02x", feat));
            b.setCallback(this);
            mSupport.writeAllFeaturesRequest(b, new byte[]{feat, slot});
            b.queueImmediately();
        } catch (IOException e) {
            LOG.error("req slot failed: {}", e.getMessage());
        }
    }

    private void write(byte[] data) {
        try {
            TransactionBuilder b = createTransactionBuilder("write_" + String.format("%02x", data[0]));
            b.setCallback(this);
            mSupport.writeAllFeatures(b, data);
            b.queueImmediately();
        } catch (IOException e) {
            LOG.error("write failed: {}", e.getMessage());
        }
    }

    // ── App information ──────────────────────────────────────────────────────

    private void writeAppInformation() {
        // Announce this app's capabilities to the watch. arr[11] = 2 is required for
        // the watch to show "Bluetooth time adjustment: supported" in its menu.
        byte[] arr = new byte[12];
        arr[0] = Casio2C2DSupport.FEATURE_APP_INFORMATION;
        for (int i = 0; i < 10; i++) arr[i + 1] = (byte) (i & 0xff);
        arr[11] = 2;
        write(arr);
    }

    // ── GPS encoding ─────────────────────────────────────────────────────────

    private byte[] makeGpsChunk0(double lat, double lon) {
        ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 0x24);
        buf.put((byte) 0x00);
        buf.put((byte) 0x01);
        buf.putDouble(lat);
        buf.putDouble(lon);
        buf.put((byte) 0x04);
        return buf.array();
    }

    private byte[] makeGpsChunk1(double alt) {
        ByteBuffer buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) 0x24);
        buf.put((byte) 0x01);
        buf.put((byte) 0x01);
        buf.putDouble(alt);
        // remaining 9 bytes are 0x00 (ByteBuffer zero-initialized)
        return buf.array();
    }

    // ── Time encoding (DOW: Sunday=0, Monday=1 … Saturday=6) ────────────────

    private void writeCurrentTime() {
        ZonedDateTime now = ZonedDateTime.now();
        // DOW: Sunday=0 … Saturday=6; Java DayOfWeek: Mon=1…Sun=7
        int dow = now.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : now.getDayOfWeek().getValue();
        byte[] arr = new byte[11];
        arr[0]  = Casio2C2DSupport.FEATURE_CURRENT_TIME;
        arr[1]  = (byte) (now.getYear() & 0xff);
        arr[2]  = (byte) ((now.getYear() >> 8) & 0xff);
        arr[3]  = (byte) now.getMonthValue();
        arr[4]  = (byte) now.getDayOfMonth();
        arr[5]  = (byte) now.getHour();
        arr[6]  = (byte) now.getMinute();
        arr[7]  = (byte) now.getSecond();
        arr[8]  = (byte) dow;
        arr[9]  = 0x00; // fractions256
        arr[10] = 0x01; // reason = manual sync
        write(arr);
    }

    // ── AbstractBTLEOperation ────────────────────────────────────────────────

    @Override
    protected void doPerform() throws IOException {
        mBuilder.setDeviceState(GBDevice.State.INITIALIZING);
        mBuilder.notify(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID, true);
        mBuilder.writeLegacy(
                getCharacteristic(CasioConstants.CASIO_READ_REQUEST_FOR_ALL_FEATURES_CHARACTERISTIC_UUID),
                new byte[]{Casio2C2DSupport.FEATURE_APP_INFORMATION});
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) throws IOException {
        throw new UnsupportedOperationException("This IS the initialization class");
    }

    // ── Main state machine ───────────────────────────────────────────────────

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] data) {
        UUID uuid = characteristic.getUuid();

        if (data == null || data.length == 0) return true;

        if (!uuid.equals(CasioConstants.CASIO_ALL_FEATURES_CHARACTERISTIC_UUID)) {
            return super.onCharacteristicChanged(gatt, characteristic, data);
        }

        byte feat = data[0];

        switch (mState) {
            case S_APP_INFO:
                if (feat == Casio2C2DSupport.FEATURE_APP_INFORMATION) {
                    LOG.debug("Init[APP_INFO] → writing APP_INFO (capabilities), requesting BLE_FEAT");
                    // Write back our app information so the watch knows this app supports
                    // Bluetooth time adjustment (arr[11] = 2 is the capabilities byte).
                    writeAppInformation();
                    mState = S_BLE_FEAT;
                    req(Casio2C2DSupport.FEATURE_BLE_FEATURES);
                }
                break;

            case S_BLE_FEAT:
                if (feat == Casio2C2DSupport.FEATURE_BLE_FEATURES) {
                    LOG.debug("Init[BLE_FEAT] → writing WATCH_NAME, requesting MODULE_ID");
                    // Identity confirm: write device name to ALL_FEAT
                    byte[] nameBytes = "CASIO GBD-200\0\0\0\0\0\0".getBytes(StandardCharsets.US_ASCII);
                    byte[] namePacket = new byte[1 + nameBytes.length];
                    namePacket[0] = Casio2C2DSupport.FEATURE_WATCH_NAME;
                    System.arraycopy(nameBytes, 0, namePacket, 1, nameBytes.length);
                    write(namePacket);
                    mState = S_MODULE_ID;
                    req(Casio2C2DSupport.FEATURE_MODULE_ID);
                }
                break;

            case S_MODULE_ID:
                if (feat == Casio2C2DSupport.FEATURE_MODULE_ID) {
                    LOG.debug("Init[MODULE_ID] → requesting WATCH_COND (round 1)");
                    mState = S_WATCH_COND_1;
                    req(Casio2C2DSupport.FEATURE_WATCH_CONDITION);
                }
                break;

            case S_WATCH_COND_1:
                if (feat == Casio2C2DSupport.FEATURE_WATCH_CONDITION) {
                    mState = S_VER_INFO_1;
                    req(Casio2C2DSupport.FEATURE_VERSION_INFORMATION);
                }
                break;

            case S_VER_INFO_1:
                if (feat == Casio2C2DSupport.FEATURE_VERSION_INFORMATION) {
                    mState = S_WATCH_COND_2;
                    req(Casio2C2DSupport.FEATURE_WATCH_CONDITION);
                }
                break;

            case S_WATCH_COND_2:
                if (feat == Casio2C2DSupport.FEATURE_WATCH_CONDITION) {
                    mState = S_VER_INFO_2;
                    req(Casio2C2DSupport.FEATURE_VERSION_INFORMATION);
                }
                break;

            case S_VER_INFO_2:
                if (feat == Casio2C2DSupport.FEATURE_VERSION_INFORMATION) {
                    LOG.debug("Init[VER_INFO_2] → requesting final WATCH_COND");
                    mState = S_WATCH_COND_3;
                    req(Casio2C2DSupport.FEATURE_WATCH_CONDITION);
                }
                break;

            case S_WATCH_COND_3:
                if (feat == Casio2C2DSupport.FEATURE_WATCH_CONDITION) {
                    LOG.debug("Init[WATCH_COND_3] → requesting DST_WATCH_STATE");
                    mState = S_DST_WATCH;
                    req(Casio2C2DSupport.FEATURE_DST_WATCH_STATE);
                }
                break;

            case S_DST_WATCH:
                if (feat == Casio2C2DSupport.FEATURE_DST_WATCH_STATE) {
                    LOG.debug("Init[DST_WATCH] → echo + request DST_SETTING slot 0");
                    write(data.clone()); // echo back
                    mState = S_DST_SETTING_0;
                    req(Casio2C2DSupport.FEATURE_DST_SETTING, (byte) 0x00);
                }
                break;

            case S_DST_SETTING_0:
                if (feat == Casio2C2DSupport.FEATURE_DST_SETTING) {
                    LOG.debug("Init[DST_SETTING_0] → saved, requesting slot 1");
                    mDstSetting0 = data.clone();
                    mState = S_DST_SETTING_1;
                    req(Casio2C2DSupport.FEATURE_DST_SETTING, (byte) 0x01);
                }
                break;

            case S_DST_SETTING_1:
                if (feat == Casio2C2DSupport.FEATURE_DST_SETTING) {
                    LOG.debug("Init[DST_SETTING_1] → echo both + GPS + request WORLD_CITY slot 0");
                    write(mDstSetting0);      // echo slot 0
                    write(data.clone());       // echo slot 1
                    // GPS location
                    double[] gps = mSupport.getLastKnownLocation();
                    write(makeGpsChunk0(gps[0], gps[1]));
                    write(makeGpsChunk1(gps[2]));
                    mState = S_WORLD_CITY_0;
                    req(Casio2C2DSupport.FEATURE_WORLD_CITY, (byte) 0x00);
                }
                break;

            case S_WORLD_CITY_0:
                if (feat == Casio2C2DSupport.FEATURE_WORLD_CITY) {
                    LOG.debug("Init[WORLD_CITY_0] → saved, requesting slot 1");
                    mWorldCity0 = data.clone();
                    mState = S_WORLD_CITY_1;
                    req(Casio2C2DSupport.FEATURE_WORLD_CITY, (byte) 0x01);
                }
                break;

            case S_WORLD_CITY_1:
                if (feat == Casio2C2DSupport.FEATURE_WORLD_CITY) {
                    LOG.debug("Init[WORLD_CITY_1] → echo both + request FEAT_2F");
                    write(mWorldCity0);   // echo slot 0
                    write(data.clone());  // echo slot 1
                    mState = S_FEAT_2F;
                    req(Casio2C2DSupport.FEATURE_FEAT_2F);
                }
                break;

            case S_FEAT_2F:
                if (feat == Casio2C2DSupport.FEATURE_FEAT_2F) {
                    LOG.debug("Init[FEAT_2F] → echo + request USER_PROF");
                    write(data.clone());
                    mState = S_USER_PROF;
                    req(Casio2C2DSupport.FEATURE_SETTING_FOR_USER_PROFILE);
                }
                break;

            case S_USER_PROF:
                if (feat == Casio2C2DSupport.FEATURE_SETTING_FOR_USER_PROFILE) {
                    LOG.debug("Init[USER_PROF] → echo + write CURRENT_TIME, waiting for 47 01");
                    write(data.clone());
                    writeCurrentTime();
                    mState = S_WAIT_INIT;
                }
                break;

            case S_WAIT_INIT:
                if (feat == Casio2C2DSupport.FEATURE_SERVICE_DISCOVERY_MANAGER
                        && data.length > 1 && data[1] == 0x01) {
                    LOG.info("Init[INITIALIZED] 47 01 received → post-init reads");
                    mSupport.setInitialized();
                    mState = S_POST_COND_1;
                    req(Casio2C2DSupport.FEATURE_WATCH_CONDITION);
                } else if (feat == Casio2C2DSupport.FEATURE_SERVICE_DISCOVERY_MANAGER
                        && data.length > 1 && data[1] == 0x02) {
                    // First-time pairing bonding request
                    LOG.debug("Init[SVC_DISC 0x02] first-time bond request → write time + init");
                    writeCurrentTime();
                    byte[] initPkt = {0x00, 0x01};
                    write(initPkt);
                }
                break;

            case S_POST_COND_1:
                if (feat == Casio2C2DSupport.FEATURE_WATCH_CONDITION) {
                    mState = S_POST_BASIC;
                    req(Casio2C2DSupport.FEATURE_SETTING_FOR_BASIC);
                }
                break;

            case S_POST_BASIC:
                if (feat == Casio2C2DSupport.FEATURE_SETTING_FOR_BASIC) {
                    mState = S_POST_VER_INFO;
                    req(Casio2C2DSupport.FEATURE_VERSION_INFORMATION);
                }
                break;

            case S_POST_VER_INFO:
                if (feat == Casio2C2DSupport.FEATURE_VERSION_INFORMATION) {
                    mState = S_POST_COND_2;
                    req(Casio2C2DSupport.FEATURE_WATCH_CONDITION);
                }
                break;

            case S_POST_COND_2:
                if (feat == Casio2C2DSupport.FEATURE_WATCH_CONDITION) {
                    LOG.info("Init complete.");
                    try {
                        TransactionBuilder b = createTransactionBuilder("init_done");
                        b.setCallback(null);
                        b.sleep(0);
                        b.queueImmediately();
                    } catch (Exception e) {
                        LOG.error("Failed to release GATT callback after init: {}", e.getMessage());
                    }
                    if (mFirstConnect) {
                        mSupport.disconnect();
                        mSupport.reconnectDelayed();
                    } else if (mNeedsGetConfiguration) {
                        // First reconnect after pairing: read config from watch, then
                        // GetConfigurationOperation will call syncProfile() when done.
                        mSupport.onReadConfiguration(null);
                    }
                    // Regular reconnects: skip syncProfile() here so auto-fetch is not
                    // blocked by a "Configuring" busy state. Settings changed while
                    // disconnected are pushed via onSharedPreferenceChanged.
                }
                break;

            default:
                break;
        }

        return true;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic,
                                        byte[] value, int status) {
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }
}
