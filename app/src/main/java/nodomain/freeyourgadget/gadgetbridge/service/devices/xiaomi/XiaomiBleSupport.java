/*  Copyright (C) 2023 José Rebelo, Yoran Vulker

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiBleSupport extends XiaomiConnectionSupport {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBleSupport.class);

    private final XiaomiSupport mXiaomiSupport;
    private AbstractXiaomiBleProtocol bleProtocol;

    private final AbstractBTLESingleDeviceSupport commsSupport = new AbstractBTLESingleDeviceSupport(LOG) {
        @Override
        public boolean useAutoConnect() {
            return mXiaomiSupport.useAutoConnect();
        }

        @Override
        protected Set<UUID> getSupportedServices() {
            // This actually includes V2 too
            return XiaomiUuids.BLE_V1_UUIDS.keySet();
        }

        @Override
        public boolean getAutoReconnect() {
            return mXiaomiSupport.getAutoReconnect();
        }

        @Override
        protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
            final XiaomiBleProtocolV1 protocolV1 = new XiaomiBleProtocolV1(XiaomiBleSupport.this);
            if (protocolV1.initializeDevice(builder)) {
                bleProtocol = protocolV1;
            } else {
                final XiaomiBleProtocolV2 protocolV2 = new XiaomiBleProtocolV2(XiaomiBleSupport.this);
                if (!protocolV2.initializeDevice(builder)) {
                    GB.toast(getContext(), "Failed to find a known Xiaomi BLE protocol", Toast.LENGTH_LONG, GB.ERROR);
                    LOG.warn("Failed to find a known Xiaomi BLE protocol");
                    builder.setDeviceState(GBDevice.State.NOT_CONNECTED);
                    return builder;
                }

                bleProtocol = protocolV2;
            }

            return builder;
        }

        @Override
        public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final byte[] value) {
            if (super.onCharacteristicChanged(gatt, characteristic, value)) {
                return true;
            }

            if (bleProtocol != null && bleProtocol.onCharacteristicChanged(gatt, characteristic, value)) {
                return true;
            }

            LOG.warn("Unhandled characteristic changed: {} {}", characteristic.getUuid(), GB.hexdump(value));
            return false;
        }

        @Override
        public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
            super.onMtuChanged(gatt, mtu, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }
            if (bleProtocol != null) {
                bleProtocol.onMtuChanged(gatt, mtu, status);
            }
        }

        @Override
        public void dispose() {
            synchronized (ConnectionMonitor) {
                mXiaomiSupport.onDisconnect();
                super.dispose();
            }
        }
    };

    XiaomiSupport getXiaomiSupport() {
        return mXiaomiSupport;
    }

    AbstractBTLESingleDeviceSupport getCommsSupport() {
        return commsSupport;
    }

    public XiaomiBleSupport(final XiaomiSupport xiaomiSupport) {
        this.mXiaomiSupport = xiaomiSupport;
    }

    @Override
    public void onAuthSuccess() {
        if (bleProtocol != null) {
            bleProtocol.onAuthSuccess();
        }
    }

    @Override
    public void setContext(GBDevice device, BluetoothAdapter adapter, Context context) {
        this.commsSupport.setContext(device, adapter, context);
    }

    @Override
    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        if (bleProtocol != null) {
            bleProtocol.sendCommand(taskName, command);
        }
    }

    @Override
    public void sendDataChunk(String taskName, byte[] chunk, @Nullable XiaomiSendCallback callback) {
        if (bleProtocol != null) {
            bleProtocol.sendDataChunk(taskName, chunk, callback);
        }
    }

    @Override
    public void setAutoReconnect(boolean enabled) {
        this.commsSupport.setAutoReconnect(enabled);
    }

    /**
     * Realistically, this function should only be used during auth, as we must schedule the command after
     * notifications were enabled on the characteristics, and for that we need the builder to guarantee the
     * order.
     */
    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        if (bleProtocol != null) {
            bleProtocol.sendCommand(builder, command);
        }
    }

    public TransactionBuilder createTransactionBuilder(String taskName) {
        return commsSupport.createTransactionBuilder(taskName);
    }

    public BtLEQueue getQueue() {
        return commsSupport.getQueue();
    }

    @Override
    public void onUploadProgress(int textRsrc, int progressPercent, boolean ongoing) {
        try {
            final TransactionBuilder builder = commsSupport.createTransactionBuilder("send data upload progress");
            builder.setProgress(
                    textRsrc,
                    ongoing,
                    progressPercent
            );
            builder.queue();
        } catch (final Exception e) {
            LOG.error("Failed to update progress notification", e);
        }
    }

    @Override
    public boolean connect() {
        return commsSupport.connect();
    }

    @Override
    public void runOnQueue(String taskName, Runnable runnable) {
        final TransactionBuilder b = commsSupport.createTransactionBuilder("run task " + taskName + " on queue");
        b.run(runnable);
        b.queue();
    }

    @Override
    public void dispose() {
        commsSupport.dispose();
        if (bleProtocol != null) {
            bleProtocol.dispose();
        }
    }
}
