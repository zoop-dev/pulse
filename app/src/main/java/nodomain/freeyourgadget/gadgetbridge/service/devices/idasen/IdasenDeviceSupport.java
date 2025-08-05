/*  Copyright (C) 2024-2025 Linos Giannopoulos, Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.idasen;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.devices.idasen.IdasenConstants;

public class IdasenDeviceSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(IdasenDeviceSupport.class);
    private static final AtomicLong THREAD_COUNTER = new AtomicLong(0L);

    public static final String COMMAND_UP = "nodomain.freeyourgadget.gadgetbridge.idasen.command.UP";
    public static final String COMMAND_DOWN = "nodomain.freeyourgadget.gadgetbridge.idasen.command.DOWN";
    public static final String COMMAND_SET_HEIGHT = "nodomain.freeyourgadget.gadgetbridge.idasen.command.SET_HEIGHT";
    public static final String COMMAND_GET_DESK_VALUES = "nodomain.freeyourgadget.gadgetbridge.idasen.command.GET_DESK_VALUES";
    public static final String TARGET_HEIGHT = "TARGET_HEIGHT";
    public static final String TARGET_POS_SIT = "TARGET_POS_SIT";
    public static final String TARGET_POS_STAND = "TARGET_POS_STAND";
    public static final String TARGET_POS_MID = "TARGET_POS_MID";
    public float sit_height, stand_height, mid_height;
    public float deskSpeed, deskHeight;

    public IdasenDeviceSupport() {
        super(LOG);
        addSupportedService(IdasenConstants.CHARACTERISTIC_SVC_HEIGHT);
        addSupportedService(IdasenConstants.CHARACTERISTIC_SVC_COMMAND);
        addSupportedService(IdasenConstants.CHARACTERISTIC_SVC_REF_HEIGHT);
        addSupportedService(IdasenConstants.CHARACTERISTIC_SVC_DPG);
    }

    BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String act = intent.getAction();

            switch(Objects.requireNonNull(act)){
                case COMMAND_UP:
                    sendCommand("cmd", IdasenConstants.CHARACTERISTIC_COMMAND, IdasenConstants.CMD_UP);
                    break;
                case COMMAND_DOWN:
                    sendCommand("cmd", IdasenConstants.CHARACTERISTIC_COMMAND, IdasenConstants.CMD_DOWN);
                    break;
                case COMMAND_GET_DESK_VALUES:
                    readCharacteristic("IdasenGetHeight", IdasenConstants.CHARACTERISTIC_HEIGHT);
                    break;
                case COMMAND_SET_HEIGHT:
                    HashMap<String, Float> mPositionsMap = new HashMap<>();

                    SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                    stand_height = Float.parseFloat(prefs.getString(DeviceSettingsPreferenceConst.PREF_IDASEN_STAND_HEIGHT, "0.0")) / 100F;
                    mid_height = Float.parseFloat(prefs.getString(DeviceSettingsPreferenceConst.PREF_IDASEN_MID_HEIGHT, "0.0")) / 100F;
                    sit_height = Float.parseFloat(prefs.getString(DeviceSettingsPreferenceConst.PREF_IDASEN_SIT_HEIGHT, "0.0")) / 100F;
                    mPositionsMap.put(TARGET_POS_SIT, sit_height);
                    mPositionsMap.put(TARGET_POS_STAND, stand_height);
                    mPositionsMap.put(TARGET_POS_MID, mid_height);

                    sendCommand("cmd", IdasenConstants.CHARACTERISTIC_COMMAND, IdasenConstants.CMD_WAKEUP);
                    sendCommand("cmd", IdasenConstants.CHARACTERISTIC_COMMAND, IdasenConstants.CMD_STOP);

                    BluetoothGattCharacteristic characteristic = getCharacteristic(IdasenConstants.CHARACTERISTIC_REF_HEIGHT);
                    String targetHeightKey = intent.getStringExtra(TARGET_HEIGHT);
                    float targetHeight = mPositionsMap.get(targetHeightKey);

                    ByteBuffer setHeightBB = ByteBuffer.allocate(2);
                    setHeightBB.order(ByteOrder.LITTLE_ENDIAN);
                    setHeightBB.putShort(0, (short) ((targetHeight - IdasenConstants.MIN_HEIGHT) * 10000F));
                    byte[] setHeightRequest = setHeightBB.array();
                    new Thread(() -> {
                        // This acts as a fail-safe, in case deskSpeed never goes to 0
                        // for whatever reason. It's based on the time the desk controller
                        // needs to get from the lowest to the highest point.
                        int cutOff = 100;
                        do {
                            TransactionBuilder builder = createTransactionBuilder("height");

                            builder.write(characteristic, setHeightRequest);
                            builder.queue();
                            try {
                                Thread.sleep(300);
                            } catch (InterruptedException e) {
                                LOG.error("error", e);
                            }
                            cutOff--;
                        } while (deskSpeed != 0F && cutOff != 0);

                        if (cutOff == 0) {
                            LOG.warn("desk controller did not reach the desired height in time");
                        }
                    }, "IdasenDeviceSupport_" + THREAD_COUNTER.getAndIncrement()).start();
                    break;
            }
        }
    };

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            super.dispose();
            LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(commandReceiver);
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.notify(IdasenConstants.CHARACTERISTIC_HEIGHT, true);
        sendCommand("dpg", IdasenConstants.CHARACTERISTIC_DPG, IdasenConstants.CMD_DPG_WAKEUP_PREP);
        sendCommand("dpg", IdasenConstants.CHARACTERISTIC_DPG, IdasenConstants.CMD_DPG_WAKEUP);
        sendCommand("dpg", IdasenConstants.CHARACTERISTIC_COMMAND, IdasenConstants.CMD_WAKEUP);
        initBroadcast();
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, byte[] value,
                                        int status) {

        if (characteristic.getUuid().equals(IdasenConstants.CHARACTERISTIC_HEIGHT)) {
            getDeskValues(value);
            announceDeskValues();
            return true;
        }
        return super.onCharacteristicRead(gatt, characteristic, value, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {

        if (characteristic.getUuid().equals(IdasenConstants.CHARACTERISTIC_HEIGHT)) {
            getDeskValues(value);
            announceDeskValues();
            return true;
        }
        return super.onCharacteristicChanged(gatt, characteristic, value);
    }

    private void readCharacteristic(String taskName, UUID charac) {
        TransactionBuilder builder = createTransactionBuilder(taskName);
        builder.read(charac);
        builder.queue();
    }

    private void initBroadcast() {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_UP);
        filter.addAction(COMMAND_DOWN);
        filter.addAction(COMMAND_SET_HEIGHT);
        filter.addAction(COMMAND_GET_DESK_VALUES);

        broadcastManager.registerReceiver(commandReceiver, filter);
    }

    private void getDeskValues(byte[] value) {
        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        int hh = BLETypeConversions.toUnsigned(buf.getShort());
        deskHeight = (float) IdasenConstants.MIN_HEIGHT + hh / 10000F;
        deskSpeed = buf.getShort() / 10000F;
    }

    private void announceDeskValues() {
        final Intent intent = new Intent(IdasenConstants.ACTION_REALTIME_DESK_VALUES)
                .putExtra(IdasenConstants.EXTRA_DESK_HEIGHT, deskHeight)
                .putExtra(IdasenConstants.EXTRA_DESK_SPEED, deskSpeed);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    private void sendCommand(String taskName, UUID charac,  byte[] contents) {
        TransactionBuilder builder = createTransactionBuilder(taskName);
        BluetoothGattCharacteristic characteristic = getCharacteristic(charac);
        if (characteristic != null) {
            builder.write(characteristic, contents);
            builder.queue();
        }
    }
}
