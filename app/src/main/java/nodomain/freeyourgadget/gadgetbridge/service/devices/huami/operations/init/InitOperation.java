/*  Copyright (C) 2018-2024 Andreas Shimokawa, Carsten Pfeiffer, Damien
    Gaignon, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.init;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEOperation;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation extends AbstractBTLEOperation<HuamiSupport> {
    private static final Logger LOG = LoggerFactory.getLogger(InitOperation.class);

    protected final TransactionBuilder builder;
    private final boolean needsAuth;
    private final byte authFlags;
    private final byte cryptFlags;
    protected final HuamiSupport huamiSupport;

    public InitOperation(boolean needsAuth, byte authFlags, byte cryptFlags, HuamiSupport support, TransactionBuilder builder) {
        super(support);
        this.huamiSupport = support;
        this.needsAuth = needsAuth;
        this.authFlags = authFlags;
        this.cryptFlags = cryptFlags;
        this.builder = builder;
        builder.setCallback(this);
    }

    @Override
    protected void doPerform() {
        huamiSupport.enableNotifications(builder, true);
        if (needsAuth) {
            builder.setDeviceState(GBDevice.State.AUTHENTICATING);
            // write key to device
            byte[] sendKey = org.apache.commons.lang3.ArrayUtils.addAll(new byte[]{HuamiService.AUTH_SEND_KEY, authFlags}, getSecretKey());
            builder.write(HuamiService.UUID_CHARACTERISTIC_AUTH, sendKey);
        } else {
            builder.setDeviceState(GBDevice.State.INITIALIZING);
            // get random auth number
            builder.write(HuamiService.UUID_CHARACTERISTIC_AUTH, requestAuthNumber());
        }
    }

    private byte[] requestAuthNumber() {
        if (cryptFlags == 0x00) {
            return new byte[]{HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER, authFlags};
        } else {
            return new byte[]{(byte) (cryptFlags | HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER), authFlags, 0x02, 0x01, 0x00};
        }
    }

    protected byte[] getSecretKey() {
        final byte[] authKeyBytes = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};

        final SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        final String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.substring(2));
            }
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }

    @Override
    public TransactionBuilder performInitialized(String taskName) {
        throw new UnsupportedOperationException("This IS the initialization class, you cannot call this method");
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           byte[] value) {
        UUID characteristicUUID = characteristic.getUuid();
        if (!HuamiService.UUID_CHARACTERISTIC_AUTH.equals(characteristicUUID)) {
            LOG.info("Unhandled characteristic changed: {}", characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic, value);
        }

        try {
            huamiSupport.logMessageContent(value);
            if (value[0] != HuamiService.AUTH_RESPONSE) {
                LOG.warn("Got a non-response: {}", GB.hexdump(value));
                return super.onCharacteristicChanged(gatt, characteristic, value);
            }

            if (value[1] == HuamiService.AUTH_SEND_KEY && value[2] == HuamiService.AUTH_SUCCESS) {
                TransactionBuilder builder = createTransactionBuilder("Sending the secret key to the device");
                builder.write(characteristic, requestAuthNumber());
                builder.queueImmediately();
            } else if ((value[1] & 0x0f) == HuamiService.AUTH_REQUEST_RANDOM_AUTH_NUMBER && value[2] == HuamiService.AUTH_SUCCESS) {
                byte[] eValue = handleAESAuth(value, getSecretKey());
                byte[] responseValue = org.apache.commons.lang3.ArrayUtils.addAll(
                        new byte[]{(byte) (HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER | cryptFlags), authFlags}, eValue);

                TransactionBuilder builder = createTransactionBuilder("Sending the encrypted random key to the device");
                builder.write(characteristic, responseValue);
                huamiSupport.setCurrentTime(builder);
                builder.queueImmediately();
            } else if ((value[1] & 0x0f) == HuamiService.AUTH_SEND_ENCRYPTED_AUTH_NUMBER) {
                if (value[2] == HuamiService.AUTH_SUCCESS) {
                    TransactionBuilder builder = createTransactionBuilder("Authenticated, now initialize phase 2");
                    builder.setDeviceState(GBDevice.State.INITIALIZING);
                    builder.setCallback(null); // remove init operation as the callback
                    huamiSupport.enableFurtherNotifications(builder, true);
                    huamiSupport.requestDeviceInfo(builder);
                    huamiSupport.phase2Initialize(builder);
                    huamiSupport.phase3Initialize(builder);
                    huamiSupport.setInitialized(builder);
                    builder.queueImmediately();
                } else if (value[2] == HuamiService.AUTH_FAIL) {
                    LOG.error("Authentication failed, disconnecting");
                    GB.toast(getContext(), R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN);
                    final GBDevice device = getDevice();
                    if (device != null) {
                        GBApplication.deviceService(device).disconnect();
                    }
                } else {
                    return super.onCharacteristicChanged(gatt, characteristic, value);
                }
            } else {
                return super.onCharacteristicChanged(gatt, characteristic, value);
            }
        } catch (Exception e) {
            GB.toast(getContext(), "Error authenticating Huami device", Toast.LENGTH_LONG, GB.ERROR, e);
        }
        return true;
    }

    private byte[] handleAESAuth(byte[] value, byte[] secretKey) throws InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException, BadPaddingException, IllegalBlockSizeException {
        byte[] mValue = Arrays.copyOfRange(value, 3, 19);
        @SuppressLint("GetInstance") Cipher ecipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec newKey = new SecretKeySpec(secretKey, "AES");
        ecipher.init(Cipher.ENCRYPT_MODE, newKey);
        return ecipher.doFinal(mValue);
    }
}
