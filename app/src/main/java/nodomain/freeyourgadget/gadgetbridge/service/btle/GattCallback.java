/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Callback interface handling gatt events.
 * Pretty much the same as {@link BluetoothGattCallback}, except it's an interface
 * instead of an abstract class. Some handlers commented out, because not used (yet).
 * <p>
 * Note: the boolean return values indicate whether this callback "consumed" this event
 * or not. True means, the event was consumed by this instance and no further instances
 * shall be notified. False means, this instance could not handle the event.
 */
public interface GattCallback {

    /**
     * @see BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)
     */
    void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

    /**
     * @see BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int)
     */
    void onServicesDiscovered(BluetoothGatt gatt);

    /**
     * @see BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, byte[], int)
     */
    boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                 byte[] value, int status);

    /**
     * @see BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int)
     */
    boolean onCharacteristicWrite(BluetoothGatt gatt,
                               BluetoothGattCharacteristic characteristic, int status);

    /**
     * @see BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic, byte[])
     */
    boolean onCharacteristicChanged(BluetoothGatt gatt,
                                 BluetoothGattCharacteristic characteristic, byte[] value);

    /**
     * @see BluetoothGattCallback#onDescriptorRead(BluetoothGatt, BluetoothGattDescriptor, int, byte[])
     */
    boolean onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                             int status, byte[] value);

    /**
     * @see BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int)
     */
    boolean onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor,
                           int status);

    /**
     * @see BluetoothGattCallback#onReadRemoteRssi(BluetoothGatt, int, int)
     */
    void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);

    /// @see BluetoothGattCallback#onMtuChanged(BluetoothGatt, int, int)
    void onMtuChanged(BluetoothGatt gatt, int mtu, int status);

    ///  @see BluetoothGattCallback#onReliableWriteCompleted(BluetoothGatt, int)
    void onReliableWriteCompleted (BluetoothGatt gatt, int status);

    ///  @see BluetoothGattCallback#onPhyRead(BluetoothGatt, int, int, int)
    @RequiresApi(Build.VERSION_CODES.O)
    void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status);

    ///  @see BluetoothGattCallback#onPhyUpdate(BluetoothGatt, int, int, int)
    @RequiresApi(Build.VERSION_CODES.O)
    void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status);

    ///  @see BluetoothGattCallback#onServiceChanged(BluetoothGatt)
    @RequiresApi(Build.VERSION_CODES.S)
    void onServiceChanged(@NonNull BluetoothGatt gatt);
}
