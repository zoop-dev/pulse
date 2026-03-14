/*
    Copyright (C) 2026 Christian Breiteneder

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package nodomain.freeyourgadget.gadgetbridge.service.devices.braun;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.sbm_67.GenericBloodPressureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.GenericBloodPressureSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BraunBPW4500DeviceSupport extends AbstractBTLESingleDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(BraunBPW4500DeviceSupport.class);

    // -------------------------------------------------------------------------
    // BLE Services
    // -------------------------------------------------------------------------

    private static final UUID SERVICE_MEASUREMENT =
            UUID.fromString("56484aae-a8eb-4a97-ac19-a8ea6373e05a");

    private static final UUID SERVICE_CONTROL =
            UUID.fromString("bb647f01-d352-48de-9015-d055b1355d7b");

    // -------------------------------------------------------------------------
    // Characteristics
    // -------------------------------------------------------------------------

    /**
     * Live measurement notification (19 bytes, little-endian):
     *   [0]      0x1E  flags
     *   [1..2]         systolic (uint16 LE, mmHg)
     *   [3..4]         diastolic (uint16 LE, mmHg)
     *   [5..6]   0x5A  constant (protocol ID)
     *   [7..10]        constant (unknown)
     *   [11]           hour
     *   [12]           minute
     *   [13]           second
     *   [14..15]       pulse rate (uint16 LE, bpm)
     *   [16..18] 0x00  padding
     */
    private static final UUID CHAR_MEASUREMENT =
            UUID.fromString("2db34480-bce5-4bb7-9f56-55bd202317c5");

    /**
     * Sync command: 0x01 = start sync | 0x02 = power off device (!)
     */
    private static final UUID CHAR_COMMAND =
            UUID.fromString("53ebc2ce-344c-4c5d-9d65-4740aa4660cd");

    /**
     * RTC register: [year_lo, year_hi, month(1-12), day, hour, minute, second, 0x00]
     */
    private static final UUID CHAR_RTC =
            UUID.fromString("6114ac81-2a71-4acc-ada5-555b63e8c5e1");

    // -------------------------------------------------------------------------

    private final List<GenericBloodPressureSample> pendingSamples = new ArrayList<>();

    private final BroadcastReceiver disconnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (GBDevice.ACTION_DEVICE_CHANGED.equals(intent.getAction())) {
                final GBDevice.DeviceUpdateSubject subject =
                        (GBDevice.DeviceUpdateSubject) intent.getSerializableExtra(GBDevice.EXTRA_UPDATE_SUBJECT);
                if (GBDevice.DeviceUpdateSubject.CONNECTION_STATE.equals(subject) &&
                        GBDevice.State.NOT_CONNECTED.equals(gbDevice.getState()) &&
                        !pendingSamples.isEmpty()) {
                    persistSamples();
                    pendingSamples.clear();
                }
            }
        }
    };

    public BraunBPW4500DeviceSupport() {
        super(LOG);
        addSupportedService(SERVICE_MEASUREMENT);
        addSupportedService(SERVICE_CONTROL);

        final IntentFilter filter = new IntentFilter(GBDevice.ACTION_DEVICE_CHANGED);
        //noinspection deprecation
        LocalBroadcastManager.getInstance(GBApplication.getContext())
                .registerReceiver(disconnectReceiver, filter);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        LOG.info("Initializing Braun BPW4500");
        pendingSamples.clear();

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // Enable notifications for live measurements
        final BluetoothGattCharacteristic measureChar = getCharacteristic(CHAR_MEASUREMENT);
        if (measureChar != null) {
            builder.notify(measureChar, true);
        } else {
            LOG.warn("Measurement characteristic not found!");
        }

        // Sync device clock
        writeRTC(builder);

        // Report firmware version
        final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.fwVersion = "N/A";
        versionCmd.hwVersion = "N/A";
        handleGBDeviceEvent(versionCmd);

        // Send sync command to trigger data transfer
        final BluetoothGattCharacteristic cmdChar = getCharacteristic(CHAR_COMMAND);
        if (cmdChar != null) {
            builder.write(cmdChar, new byte[]{0x01});
        }

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    /**
     * Writes the current system time to the device RTC register.
     */
    private void writeRTC(final TransactionBuilder builder) {
        final BluetoothGattCharacteristic rtcChar = getCharacteristic(CHAR_RTC);
        if (rtcChar == null) {
            LOG.warn("RTC characteristic not found");
            return;
        }
        final Calendar now = Calendar.getInstance();
        final int year = now.get(Calendar.YEAR);
        final byte[] rtc = new byte[]{
                (byte) (year & 0xFF),
                (byte) ((year >> 8) & 0xFF),
                (byte) (now.get(Calendar.MONTH) + 1),
                (byte) now.get(Calendar.DAY_OF_MONTH),
                (byte) now.get(Calendar.HOUR_OF_DAY),
                (byte) now.get(Calendar.MINUTE),
                (byte) now.get(Calendar.SECOND),
                0x00
        };
        builder.write(rtcChar, rtc);
        LOG.info("RTC written: {}-{}-{} {}:{}:{}",
                year,
                now.get(Calendar.MONTH) + 1,
                now.get(Calendar.DAY_OF_MONTH),
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                now.get(Calendar.SECOND));
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        if (CHAR_MEASUREMENT.equals(characteristic.getUuid())) {
            handleMeasurement(value);
            return true;
        }

        LOG.debug("Unhandled characteristic: {} = {}", characteristic.getUuid(), GB.hexdump(value));
        return false;
    }

    /**
     * Parses a 19-byte measurement packet from the BPW4500.
     */
    private void handleMeasurement(final byte[] data) {
        if (data == null || data.length < 19) {
            LOG.warn("Invalid measurement packet length: {}", data == null ? 0 : data.length);
            return;
        }
        if ((data[0] & 0xFF) != 0x1E) {
            LOG.warn("Unknown packet type: 0x{}", Integer.toHexString(data[0] & 0xFF));
            return;
        }

        final int systolic  = (data[1] & 0xFF) | ((data[2] & 0xFF) << 8);
        final int diastolic = (data[3] & 0xFF) | ((data[4] & 0xFF) << 8);
        final int hour      = data[11] & 0xFF;
        final int minute    = data[12] & 0xFF;
        final int second    = data[13] & 0xFF;
        final int pulse     = (data[14] & 0xFF) | ((data[15] & 0xFF) << 8);

        LOG.info("Measurement: {}/{} mmHg, pulse: {} bpm, time: {}:{}:{}",
                systolic, diastolic, pulse, hour, minute, second);

        // Use system date with time from device
        final Calendar ts = Calendar.getInstance();
        ts.set(Calendar.HOUR_OF_DAY, hour);
        ts.set(Calendar.MINUTE, minute);
        ts.set(Calendar.SECOND, second);
        ts.set(Calendar.MILLISECOND, 0);

        final GenericBloodPressureSample sample = new GenericBloodPressureSample();
        sample.setTimestamp(ts.getTimeInMillis());
        sample.setBpSystolic(systolic);
        sample.setBpDiastolic(diastolic);
        sample.setPulseRate(pulse);
        sample.setMeanArterialPressure(0);
        sample.setUserIndex(0);
        sample.setMeasurementStatus(0);

        pendingSamples.add(sample);

        // Persist immediately as the device disconnects after transmission
        persistSamples();
        pendingSamples.clear();

        GB.signalActivityDataFinish(getDevice());
    }

    private void persistSamples() {
        if (pendingSamples.isEmpty()) return;

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GenericBloodPressureSampleProvider provider =
                    new GenericBloodPressureSampleProvider(getDevice(), session);
            provider.persistForDevice(getContext(), getDevice(), pendingSamples);
            LOG.info("Persisted {} blood pressure samples", pendingSamples.size());
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving blood pressure data", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void dispose() {
        try {
            //noinspection deprecation
            LocalBroadcastManager.getInstance(GBApplication.getContext())
                    .unregisterReceiver(disconnectReceiver);
        } catch (final Exception e) {
            LOG.error("Failed to unregister receiver", e);
        }
        super.dispose();
    }
}