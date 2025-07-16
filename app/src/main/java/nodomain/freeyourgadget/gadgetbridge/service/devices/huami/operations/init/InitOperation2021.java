/*  Copyright (C) 2021-2024 Andreas Shimokawa, José Rebelo

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

import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.RESPONSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService.SUCCESS;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021ChunkedDecoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021ChunkedEncoder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.Huami2021Handler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ECDH_B163;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class InitOperation2021 extends InitOperation implements Huami2021Handler {
    private final byte[] privateEC = new byte[24];
    private byte[] publicEC;

    public static final short CHUNKED2021_ENDPOINT_AUTH = 0x0082;

    private final BluetoothGattCharacteristic characteristicChunked2021Write;
    private final Huami2021ChunkedEncoder huami2021ChunkedEncoder;
    private final Huami2021ChunkedDecoder huami2021ChunkedDecoder;

    private static final Logger LOG = LoggerFactory.getLogger(InitOperation2021.class);

    public InitOperation2021(final boolean needsAuth,
                             final byte authFlags,
                             final byte cryptFlags,
                             final HuamiSupport support,
                             final TransactionBuilder builder,
                             final BluetoothGattCharacteristic characteristicChunked2021Write,
                             final Huami2021ChunkedEncoder huami2021ChunkedEncoder,
                             final Huami2021ChunkedDecoder huami2021ChunkedDecoder) {
        super(needsAuth, authFlags, cryptFlags, support, builder);
        this.characteristicChunked2021Write = characteristicChunked2021Write;
        this.huami2021ChunkedEncoder = huami2021ChunkedEncoder;
        this.huami2021ChunkedDecoder = huami2021ChunkedDecoder;
        this.huami2021ChunkedDecoder.setHuami2021Handler(this);
    }

    @Override
    protected void doPerform() {
        huamiSupport.enableNotifications(builder, true);
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        // get random auth number
        generateKeyPair();
        final byte[] sendPubKeyCommand = new byte[48 + 4];
        sendPubKeyCommand[0] = 0x04;
        sendPubKeyCommand[1] = 0x02;
        sendPubKeyCommand[2] = 0x00;
        sendPubKeyCommand[3] = 0x02;
        System.arraycopy(publicEC, 0, sendPubKeyCommand, 4, 48);
        huami2021ChunkedEncoder.write(chunk -> builder.write(characteristicChunked2021Write, chunk), CHUNKED2021_ENDPOINT_AUTH, sendPubKeyCommand, true, false);
    }

    private void generateKeyPair() {
        new Random().nextBytes(privateEC);
        publicEC = ECDH_B163.ecdh_generate_public(privateEC);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        final UUID characteristicUUID = characteristic.getUuid();
        if (!HuamiService.UUID_CHARACTERISTIC_CHUNKEDTRANSFER_2021_READ.equals(characteristicUUID)) {
            LOG.warn("Unhandled characteristic changed: {}", characteristicUUID);
            return super.onCharacteristicChanged(gatt, characteristic, value);
        }

        if (value.length <= 1 || value[0] != 0x03) {
            // Not chunked
            return super.onCharacteristicChanged(gatt, characteristic, value);
        }

        final boolean needsAck = huami2021ChunkedDecoder.decode(value);
        if (needsAck) {
            huamiSupport.sendChunkedAck();
        }

        return true;
    }

    @Override
    public void handle2021Payload(final short type, final byte[] payload) {
        if (type != CHUNKED2021_ENDPOINT_AUTH) {
            this.huamiSupport.handle2021Payload(type, payload);
            return;
        }

        if (payload[0] == RESPONSE && payload[1] == 0x04 && payload[2] == SUCCESS) {
            LOG.debug("Got remote random + public key");
            // Received remote random (16 bytes) + public key (48 bytes)

            final byte[] remotePublicEC = new byte[48];
            final byte[] remoteRandom = new byte[16];
            final byte[] finalSharedSessionAES = new byte[16];

            System.arraycopy(payload, 3, remoteRandom, 0, 16);
            System.arraycopy(payload, 19, remotePublicEC, 0, 48);
            final byte[] sharedEC = Objects.requireNonNull(ECDH_B163.ecdh_generate_shared(privateEC, remotePublicEC));
            final int encryptedSequenceNumber = (sharedEC[0] & 0xff) | ((sharedEC[1] & 0xff) << 8) | ((sharedEC[2] & 0xff) << 16) | ((sharedEC[3] & 0xff) << 24);

            final byte[] secretKey = getSecretKey();
            for (int i = 0; i < 16; i++) {
                finalSharedSessionAES[i] = (byte) (sharedEC[i + 8] ^ secretKey[i]);
            }

            LOG.debug("Shared Session Key: {}", GB.hexdump(finalSharedSessionAES));
            huami2021ChunkedEncoder.setEncryptionParameters(encryptedSequenceNumber, finalSharedSessionAES);
            huami2021ChunkedDecoder.setEncryptionParameters(finalSharedSessionAES);

            try {
                final byte[] encryptedRandom1 = CryptoUtils.encryptAES(remoteRandom, secretKey);
                final byte[] encryptedRandom2 = CryptoUtils.encryptAES(remoteRandom, finalSharedSessionAES);
                if (encryptedRandom1.length == 16 && encryptedRandom2.length == 16) {
                    final byte[] command = new byte[33];
                    command[0] = 0x05;
                    System.arraycopy(encryptedRandom1, 0, command, 1, 16);
                    System.arraycopy(encryptedRandom2, 0, command, 17, 16);
                    TransactionBuilder builder = createTransactionBuilder("Sending double encryted random to device");
                    huami2021ChunkedEncoder.write(chunk -> builder.write(characteristicChunked2021Write, chunk), CHUNKED2021_ENDPOINT_AUTH, command, true, false);
                    builder.queueImmediately();
                }
            } catch (final Exception e) {
                LOG.error("AES encryption failed", e);
            }
        } else if (payload[0] == RESPONSE && payload[1] == 0x05 && payload[2] == SUCCESS) {
            LOG.debug("Auth Success");

            try {
                final TransactionBuilder builder = createTransactionBuilder("Authenticated, now initialize phase 2");
                builder.setDeviceState(GBDevice.State.INITIALIZING);
                builder.setCallback(null); // remove init operation as the callback
                huamiSupport.enableFurtherNotifications(builder, true);
                huamiSupport.setCurrentTime(builder);
                huamiSupport.requestDeviceInfo(builder);
                huamiSupport.phase2Initialize(builder);
                huamiSupport.phase3Initialize(builder);
                huamiSupport.setInitialized(builder);
                builder.queueImmediately();
            } catch (final Exception e) {
                LOG.error("failed initializing device", e);
            }
        } else if (payload[0] == RESPONSE && payload[1] == 0x05 && payload[2] == 0x25) {
            LOG.error("Authentication failed, disconnecting");
            GB.toast(getContext(), R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN);
            final GBDevice device = getDevice();
            if (device != null) {
                GBApplication.deviceService(device).disconnect();
            }
        } else {
            LOG.warn("Unhandled auth payload: {}", GB.hexdump(payload));
        }
    }
}
