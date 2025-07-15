package nodomain.freeyourgadget.gadgetbridge.service.devices.gree;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.gree.GreeAcPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.AbstractGreeMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.GreeBindMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.GreeBleInfoMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.GreeBleKeyMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.GreePackMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.gree.messages.GreeWlanMessage;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class GreeAcSupport extends AbstractBTLESingleDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GreeAcSupport.class);

    public static final UUID UUID_SERVICE_GREE_PACK = UUID.fromString("0000fd06-173c-93d2-488e-fe144d2e12a2");
    public static final UUID UUID_CHARACTERISTIC_PACK_TX = UUID.fromString("0000fd03-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_PACK_RX = UUID.fromString("0000fd04-0000-1000-8000-00805f9b34fb");

    public static final byte[] DEFAULT_KEY = "a3K8Bx%2r8Y7#xDh".getBytes();

    private byte[] bindKey = null;

    private BluetoothGattCharacteristic characteristicTx;

    public GreeAcSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_GREE_PACK);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        characteristicTx = getCharacteristic(UUID_CHARACTERISTIC_PACK_TX);
        final BluetoothGattCharacteristic characteristicRx = getCharacteristic(UUID_CHARACTERISTIC_PACK_RX);

        if (characteristicTx == null || characteristicRx == null) {
            LOG.warn("Pack characteristics are null");
            builder.setDeviceState(GBDevice.State.NOT_CONNECTED);
            return builder;
        }

        characteristicTx.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        builder.notify(UUID_CHARACTERISTIC_PACK_RX, true);

        final String mac = getDevice().getAddress().trim().replace(":", "").toLowerCase(Locale.ROOT);
        writeMessage(builder, new GreeBindMessage(mac.substring(mac.length() - 4)));

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, byte[] value) {
        if (super.onCharacteristicChanged(gatt, characteristic, value)) {
            return true;
        }

        final UUID characteristicUUID = characteristic.getUuid();

        if (UUID_CHARACTERISTIC_PACK_RX.equals(characteristicUUID)) {
            final String packMessageJson = new String(value, StandardCharsets.UTF_8);
            LOG.debug("Got pack: {}", packMessageJson);

            final AbstractGreeMessage message;
            try {
                message = AbstractGreeMessage.fromJson(packMessageJson);
            } catch (final Exception e) {
                LOG.error("Failed to deserialize message from json", e);
                return true;
            }

            handleMessage(message);

            return true;
        }

        LOG.warn("Unknown characteristic {} changed: {}", characteristicUUID, GB.hexdump(value));

        return false;
    }

    private void handleMessage(final AbstractGreeMessage message) {
        if (message instanceof GreePackMessage) {
            final GreePackMessage packMessage = (GreePackMessage) message;
            final int encryptionKeyNum = packMessage.getEncryptionKey();

            final byte[] key;
            switch (encryptionKeyNum) {
                case GreePackMessage.KEY_BIND:
                    key = bindKey;
                    break;
                case GreePackMessage.KEY_DEFAULT:
                    key = DEFAULT_KEY;
                    break;
                default:
                    LOG.warn("Unknown pack message encryption key {}", encryptionKeyNum);
                    return;
            }

            if (key == null) {
                LOG.error("Key {} is not known", encryptionKeyNum);
                return;
            }

            final byte[] encryptedBytes = Base64.decode(packMessage.getPack(), Base64.DEFAULT);
            final byte[] decryptedBytes;
            try {
                decryptedBytes = CryptoUtils.decryptAES_ECB_Pad(encryptedBytes, key);
            } catch (final GeneralSecurityException e) {
                LOG.error("Failed to decrypt pack", e);
                return;
            }

            final AbstractGreeMessage packSubMessage;
            try {
                final String subMessageJson = new String(decryptedBytes, StandardCharsets.UTF_8);
                LOG.debug("Pack sub message: {}", subMessageJson);

                packSubMessage = AbstractGreeMessage.fromJson(subMessageJson);
            } catch (final Exception e) {
                LOG.error("Failed to deserialize pack sub message from json", e);
                return;
            }

            handleMessage(packSubMessage);
            return;
        }

        if (message instanceof GreeBleKeyMessage) {
            final GreeBleKeyMessage bleKeyMessage = (GreeBleKeyMessage) message;

            LOG.debug("Got bind key: {}", bleKeyMessage.getKey());
            bindKey = bleKeyMessage.getKey().getBytes(StandardCharsets.UTF_8);

            evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences("authkey", bleKeyMessage.getKey()));

            final DevicePrefs devicePrefs = getDevicePrefs();
            final String host = devicePrefs.getString(GreeAcPrefs.PREF_HOST, "");
            final String psw = devicePrefs.getString(GreeAcPrefs.PREF_PASSWORD, "");
            final String ssid = devicePrefs.getString(GreeAcPrefs.PREF_SSID, "");
            if (host.isEmpty() || psw.isEmpty() || ssid.isEmpty()) {
                final Intent intent = new Intent(GreeAcPairingActivity.ACTION_BIND_STATUS);
                intent.setPackage(BuildConfig.APPLICATION_ID);
                intent.putExtra(GreeAcPairingActivity.EXTRA_BIND_MESSAGE, "missing wlan params");
                getContext().sendBroadcast(intent);
                return;
            }

            final TransactionBuilder builder = createTransactionBuilder("setup wifi");
            writeMessage(builder, new GreeWlanMessage(host, psw, ssid, 1));
            builder.queue();

            return;
        }

        if (message instanceof GreeBleInfoMessage) {
            final GreeBleInfoMessage bleInfoMessage = (GreeBleInfoMessage) message;

            LOG.debug("Got ble info, wificon = {}", bleInfoMessage.getWificon());

            final Intent intent = new Intent(GreeAcPairingActivity.ACTION_BIND_STATUS);
            intent.setPackage(BuildConfig.APPLICATION_ID);
            intent.putExtra(GreeAcPairingActivity.EXTRA_BIND_KEY, new String(bindKey));
            intent.putExtra(GreeAcPairingActivity.EXTRA_BIND_MESSAGE, String.valueOf(bleInfoMessage.getWificon()));
            getContext().sendBroadcast(intent);

            // TODO disconnect - not much else we can do
        }

        LOG.warn("Unhandled message: {}", message);
    }

    private void writeMessage(final TransactionBuilder builder, final AbstractGreeMessage message) {
        final String messageJson = message.toString();

        LOG.debug("Will send: {}", messageJson);

        if (message instanceof GreePackMessage) {
            builder.write(characteristicTx, messageJson.getBytes(StandardCharsets.UTF_8));
            return;
        }

        final int key;
        final byte[] encryptedBytes;
        try {
            if (message instanceof GreeBindMessage) {
                key = GreePackMessage.KEY_DEFAULT;
                encryptedBytes = CryptoUtils.encryptAES_ECB_Pad(messageJson.getBytes(StandardCharsets.UTF_8), DEFAULT_KEY);
            } else {
                if (bindKey == null) {
                    LOG.error("No bind key, unable to encrypt");
                    return;
                }
                key = GreePackMessage.KEY_BIND;
                encryptedBytes = CryptoUtils.encryptAES_ECB_Pad(messageJson.getBytes(StandardCharsets.UTF_8), bindKey);
            }
        } catch (final GeneralSecurityException e) {
            LOG.error("Failed to encrypt message", e);
            return;
        }

        final GreePackMessage packMessage = new GreePackMessage(Base64.encodeToString(encryptedBytes, Base64.DEFAULT).replace("\n", "").trim(), key);
        writeMessage(builder, packMessage);
    }
}
