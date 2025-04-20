/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.ECDH_B163;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

/**
 * This is a reimplementation of nodomain.freeyourgadget.gadgetbridge.service.devices.huami.operations.init.InitOperation2021
 * to avoid a big refactor on older devices, while making it compatible with btrfcomm devices.
 */
public class ZeppOsAuthenticationService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsAuthenticationService.class);

    public static final short ENDPOINT = 0x0082;

    public static final byte CMD_PUB_KEY = 0x04;
    public static final byte CMD_SESSION_KEY = 0x05;

    private final byte[] privateEC = new byte[24];
    private final byte[] publicEC = new byte[48];
    private final byte[] finalSharedSessionAES = new byte[16];

    public ZeppOsAuthenticationService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        if (payload[0] != HuamiService.RESPONSE) {
            LOG.warn("Got non-response auth byte {}", String.format("0x%02x", payload[0]));
            return;
        }

        switch (payload[1]) {
            case CMD_PUB_KEY:
                if (payload[2] != HuamiService.SUCCESS) {
                    LOG.error("Got pub key command failure, status={}", String.format("0x%02x", payload[0]));
                    return;
                }

                LOG.debug("Got remote random + public key");
                final byte[] remoteRandom = new byte[16];
                final byte[] remotePublicEC = new byte[48];

                // Received remote random (16 bytes) + public key (48 bytes)
                System.arraycopy(payload, 3, remoteRandom, 0, 16);
                System.arraycopy(payload, 19, remotePublicEC, 0, 48);
                final byte[] sharedEC = Objects.requireNonNull(ECDH_B163.ecdh_generate_shared(privateEC, remotePublicEC));
                final int encryptedSequenceNumber = BLETypeConversions.toUint32(sharedEC, 0);

                final byte[] secretKey = getSecretKey();
                for (int i = 0; i < 16; i++) {
                    finalSharedSessionAES[i] = (byte) (sharedEC[i + 8] ^ secretKey[i]);
                }

                LOG.debug("Shared Session Key: {}", GB.hexdump(finalSharedSessionAES));
                getSupport().setEncryptionParameters(encryptedSequenceNumber, finalSharedSessionAES);

                try {
                    final byte[] encryptedRandom1 = CryptoUtils.encryptAES(remoteRandom, secretKey);
                    final byte[] encryptedRandom2 = CryptoUtils.encryptAES(remoteRandom, finalSharedSessionAES);
                    if (encryptedRandom1.length == 16 && encryptedRandom2.length == 16) {
                        byte[] command = new byte[33];
                        command[0] = 0x05;
                        System.arraycopy(encryptedRandom1, 0, command, 1, 16);
                        System.arraycopy(encryptedRandom2, 0, command, 17, 16);
                        write("send double encrypted random to device", command);
                    }
                } catch (final Exception e) {
                    LOG.error("AES encryption failed", e);
                }
                return;
            case CMD_SESSION_KEY:
                if (payload[2] == 0x25) {
                    // wrong auth key
                    onAuthFailed();
                    return;
                } else if (payload[2] != HuamiService.SUCCESS) {
                    LOG.error("Got session key failure, status={}", String.format("0x%02x", payload[0]));
                    return;
                }

                LOG.debug("Auth Success");

                try {
                    getSupport().onAuthenticationSuccess();
                } catch (Exception e) {
                    LOG.error("failed initializing device", e);
                }
                return;
        }

        LOG.warn("Got unknown auth byte {}", String.format("0x%02x", payload[0]));
    }

    public void startAuthentication(final ZeppOsTransactionBuilder builder) {
        new Random().nextBytes(privateEC);

        final byte[] pub = ECDH_B163.ecdh_generate_public(privateEC);
        assert pub != null;
        System.arraycopy(pub, 0, publicEC, 0, 48);

        final byte[] sendPubKeyCommand = new byte[48 + 4];
        sendPubKeyCommand[0] = 0x04;
        sendPubKeyCommand[1] = 0x02;
        sendPubKeyCommand[2] = 0x00;
        sendPubKeyCommand[3] = 0x02;
        System.arraycopy(publicEC, 0, sendPubKeyCommand, 4, 48);

        write(builder, sendPubKeyCommand);
    }

    private void onAuthFailed() {
        LOG.error("Authentication failed, disconnecting");
        GB.toast(getContext(), R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN);
        final GBDevice device = getSupport().getDevice();
        if (device != null) {
            GBApplication.deviceService(device).disconnect();
        }
    }

    private byte[] getSecretKey() {
        final byte[] authKeyBytes = new byte[]{0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x40, 0x41, 0x42, 0x43, 0x44, 0x45};

        final Prefs devicePrefs = getDevicePrefs();

        final String authKey = devicePrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            byte[] srcBytes = authKey.trim().getBytes();
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                srcBytes = GB.hexStringToByteArray(authKey.substring(2));
            } else if (authKey.length() == 32) {
                // All Zepp OS devices require a hex key
                srcBytes = GB.hexStringToByteArray(authKey);
            }
            System.arraycopy(srcBytes, 0, authKeyBytes, 0, Math.min(srcBytes.length, 16));
        }

        return authKeyBytes;
    }
}
