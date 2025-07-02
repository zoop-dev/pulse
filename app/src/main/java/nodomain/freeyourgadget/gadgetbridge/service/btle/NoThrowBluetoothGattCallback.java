/*  Copyright (C) 2025 Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A {@link BluetoothGattCallback} that ensures no {@link Exception} can kill the current {@link Thread}.
/// All processing and logging must be done in the {@link #Delegate} and not here.
public class NoThrowBluetoothGattCallback<T extends BluetoothGattCallback> extends BluetoothGattCallback {
    private static final Logger LOG = LoggerFactory.getLogger(NoThrowBluetoothGattCallback.class);
    public final T Delegate;

    public NoThrowBluetoothGattCallback(T delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate is null");
        }
        Delegate = delegate;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        try {
            Delegate.onPhyUpdate(gatt, txPhy, rxPhy, status);
        } catch (Exception ex) {
            LOG.error("onPhyUpdate", ex);
        } catch (Throwable t) {
            LOG.error("onPhyUpdate", t);
            throw t;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
        try {
            Delegate.onPhyRead(gatt, txPhy, rxPhy, status);
        } catch (Exception ex) {
            LOG.error("onPhyRead", ex);
        } catch (Throwable t) {
            LOG.error("onPhyRead", t);
            throw t;
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        try {
            Delegate.onConnectionStateChange(gatt, status, newState);
        } catch (Exception ex) {
            LOG.error("onConnectionStateChange", ex);
        } catch (Throwable t) {
            LOG.error("onConnectionStateChange", t);
            throw t;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        try {
            Delegate.onServicesDiscovered(gatt, status);
        } catch (Exception ex) {
            LOG.error("onServicesDiscovered", ex);
        } catch (Throwable t) {
            LOG.error("onServicesDiscovered", t);
            throw t;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        try {
            Delegate.onCharacteristicRead(gatt, characteristic, status);
        } catch (Exception ex) {
            LOG.error("onCharacteristicRead-old", ex);
        } catch (Throwable t) {
            LOG.error("onCharacteristicRead-old", t);
            throw t;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
        try {
            Delegate.onCharacteristicRead(gatt, characteristic, value, status);
        } catch (Exception ex) {
            LOG.error("onCharacteristicRead", ex);
        } catch (Throwable t) {
            LOG.error("onCharacteristicRead", t);
            throw t;
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        try {
            Delegate.onCharacteristicWrite(gatt, characteristic, status);
        } catch (Exception ex) {
            LOG.error("onCharacteristicWrite", ex);
        } catch (Throwable t) {
            LOG.error("onCharacteristicWrite", t);
            throw t;
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        try {
            Delegate.onCharacteristicChanged(gatt, characteristic);
        } catch (Exception ex) {
            LOG.error("onCharacteristicChanged-old", ex);
        } catch (Throwable t) {
            LOG.error("onCharacteristicChanged-old", t);
            throw t;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        try {
            Delegate.onCharacteristicChanged(gatt, characteristic, value);
        } catch (Exception ex) {
            LOG.error("onCharacteristicChanged", ex);
        } catch (Throwable t) {
            LOG.error("onCharacteristicChanged", t);
            throw t;
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        try {
            Delegate.onDescriptorRead(gatt, descriptor, status);
        } catch (Exception ex) {
            LOG.error("onDescriptorRead-old", ex);
        } catch (Throwable t) {
            LOG.error("onDescriptorRead-old", t);
            throw t;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
        try {
            Delegate.onDescriptorRead(gatt, descriptor, status, value);
        } catch (Exception ex) {
            LOG.error("onDescriptorRead", ex);
        } catch (Throwable t) {
            LOG.error("onDescriptorRead", t);
            throw t;
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        try {
            Delegate.onDescriptorWrite(gatt, descriptor, status);
        } catch (Exception ex) {
            LOG.error("onDescriptorWrite", ex);
        } catch (Throwable t) {
            LOG.error("onDescriptorWrite", t);
            throw t;
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        try {
            Delegate.onReliableWriteCompleted(gatt, status);
        } catch (Exception ex) {
            LOG.error("onReliableWriteCompleted", ex);
        } catch (Throwable t) {
            LOG.error("onReliableWriteCompleted", t);
            throw t;
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        try {
            Delegate.onReadRemoteRssi(gatt, rssi, status);
        } catch (Exception ex) {
            LOG.error("onReadRemoteRssi", ex);
        } catch (Throwable t) {
            LOG.error("onReadRemoteRssi", t);
            throw t;
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        try {
            Delegate.onMtuChanged(gatt, mtu, status);
        } catch (Exception ex) {
            LOG.error("onMtuChanged", ex);
        } catch (Throwable t) {
            LOG.error("onMtuChanged", t);
            throw t;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onServiceChanged(@NonNull BluetoothGatt gatt) {
        try {
            Delegate.onServiceChanged(gatt);
        } catch (Exception ex) {
            LOG.error("onServiceChanged", ex);
        } catch (Throwable t) {
            LOG.error("onServiceChanged", t);
            throw t;
        }
    }
}
