package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

/** @noinspection LoggingSimilarMessage*/
public class XiaomiBleProtocolV1 extends AbstractXiaomiBleProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiBleProtocolV1.class);

    private final XiaomiBleSupport xiaomiBleSupport;
    private final XiaomiSupport xiaomiSupport;
    private final AbstractBTLESingleDeviceSupport commsSupport;
    private final Context context;
    private final GBDevice gbDevice;

    private XiaomiCharacteristicV1 characteristicCommandRead;
    private XiaomiCharacteristicV1 characteristicCommandWrite;
    private XiaomiCharacteristicV1 characteristicActivityData;
    @Nullable
    private XiaomiCharacteristicV1 characteristicDataUpload;

    public XiaomiBleProtocolV1(final XiaomiBleSupport xiaomiBleSupport) {
        this.xiaomiBleSupport = xiaomiBleSupport;
        this.xiaomiSupport = xiaomiBleSupport.getXiaomiSupport();
        this.commsSupport = xiaomiBleSupport.getCommsSupport();
        this.context = xiaomiSupport.getContext();
        this.gbDevice = xiaomiSupport.getDevice();
    }

    @Override
    public boolean initializeDevice(final TransactionBuilder builder) {
        reset();

        XiaomiUuids.XiaomiBleUuidSet uuidSet = null;
        BluetoothGattCharacteristic btCharacteristicCommandRead = null;
        BluetoothGattCharacteristic btCharacteristicCommandWrite = null;
        BluetoothGattCharacteristic btCharacteristicActivityData = null;
        BluetoothGattCharacteristic btCharacteristicDataUpload = null;

        // Attempt to find a known xiaomi service
        for (Map.Entry<UUID, XiaomiUuids.XiaomiBleUuidSet> xiaomiUuid : XiaomiUuids.BLE_V1_UUIDS.entrySet()) {
            final XiaomiUuids.XiaomiBleUuidSet currentUuidSet = xiaomiUuid.getValue();
            UUID currentChar;

            if ((currentChar = currentUuidSet.getCharacteristicCommandRead()) == null ||
                    (btCharacteristicCommandRead = commsSupport.getCharacteristic(currentChar)) == null) {
                continue;
            }

            if ((currentChar = currentUuidSet.getCharacteristicCommandWrite()) == null ||
                    (btCharacteristicCommandWrite = commsSupport.getCharacteristic(currentChar)) == null) {
                continue;
            }

            if ((currentChar = currentUuidSet.getCharacteristicActivityData()) == null ||
                    (btCharacteristicActivityData = commsSupport.getCharacteristic(currentChar)) == null) {
                continue;
            }

            if ((currentChar = currentUuidSet.getCharacteristicDataUpload()) == null ||
                    (btCharacteristicDataUpload = commsSupport.getCharacteristic(currentChar)) == null) {
                LOG.warn("btCharacteristicDataUpload characteristic is null");
                // this characteristic may not be supported by all models
            }

            LOG.debug("Found Xiaomi service: {}", xiaomiUuid.getKey());
            uuidSet = xiaomiUuid.getValue();

            break;
        }

        if (uuidSet == null) {
            return false;
        }

        // FIXME unsetDynamicState unsets the fw version, which causes problems..
        if (gbDevice.getFirmwareVersion() == null) {
            gbDevice.setFirmwareVersion(xiaomiSupport.getCachedFirmwareVersion() != null ?
                    xiaomiSupport.getCachedFirmwareVersion() :
                    "N/A"
            );
        }

        // FIXME:
        // Because the first handshake packet is sent before the actions in the builder are run,
        // the maximum message size is not properly initialized if the device itself does not request
        // the MTU to be upgraded. However, since we will upgrade the MTU ourselves to the highest
        // possible (512) and the device will (likely) respond with something higher than 247,
        // we will initialize the characteristics with that MTU.
        final int expectedMtu = 247;
        characteristicCommandRead = new XiaomiCharacteristicV1(xiaomiBleSupport, btCharacteristicCommandRead, xiaomiSupport.getAuthService());
        characteristicCommandRead.setEncrypted(uuidSet.isEncrypted());
        characteristicCommandRead.setChannelHandler(xiaomiSupport::handleCommandBytes);
        characteristicCommandRead.setMtu(expectedMtu);
        characteristicCommandWrite = new XiaomiCharacteristicV1(xiaomiBleSupport, btCharacteristicCommandWrite, xiaomiSupport.getAuthService());
        characteristicCommandWrite.setEncrypted(uuidSet.isEncrypted());
        characteristicCommandWrite.setMtu(expectedMtu);
        characteristicActivityData = new XiaomiCharacteristicV1(xiaomiBleSupport, btCharacteristicActivityData, xiaomiSupport.getAuthService());
        characteristicActivityData.setChannelHandler(xiaomiSupport.getHealthService().getActivityFetcher()::addChunk);
        characteristicActivityData.setEncrypted(uuidSet.isEncrypted());
        characteristicActivityData.setMtu(expectedMtu);
        if (btCharacteristicDataUpload != null) {
            characteristicDataUpload = new XiaomiCharacteristicV1(xiaomiBleSupport, btCharacteristicDataUpload, xiaomiSupport.getAuthService());
            characteristicDataUpload.setEncrypted(uuidSet.isEncrypted());
            characteristicDataUpload.setIncrementNonce(false);
            characteristicDataUpload.setMtu(expectedMtu);
        }

        // request highest possible MTU; device should response with the highest supported MTU anyway
        builder.requestMtu(512);
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        builder.notify(btCharacteristicCommandWrite, true);
        builder.notify(btCharacteristicCommandRead, true);
        builder.notify(btCharacteristicActivityData, true);
        builder.notify(btCharacteristicDataUpload, true);
        builder.setDeviceState(GBDevice.State.AUTHENTICATING);

        if (uuidSet.isEncrypted()) {
            builder.run(() -> xiaomiSupport.getAuthService().startEncryptedHandshake());
        } else {
            builder.run(() -> xiaomiSupport.getAuthService().startClearTextHandshake());
        }

        return true;
    }

    @Override
    public void reset() {
        if (characteristicCommandRead != null)
            characteristicCommandRead.reset();
        if (characteristicCommandWrite != null)
            characteristicCommandWrite.reset();
        if (characteristicDataUpload != null)
            characteristicDataUpload.reset();
        if (characteristicActivityData != null)
            characteristicActivityData.reset();
    }

    @Override
    public void dispose() {
        if (characteristicCommandRead != null)
            characteristicCommandRead.dispose();
        if (characteristicCommandWrite != null)
            characteristicCommandWrite.dispose();
        if (characteristicDataUpload != null)
            characteristicDataUpload.dispose();
        if (characteristicActivityData != null)
            characteristicActivityData.dispose();
    }

    @Override
    public boolean onCharacteristicChanged(final BluetoothGatt gatt,
                                           final BluetoothGattCharacteristic characteristic,
                                           final byte[] value) {
        final UUID characteristicUUID = characteristic.getUuid();

        if (characteristicCommandRead.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicCommandRead.onCharacteristicChanged(value);
            return true;
        } else if (characteristicCommandWrite.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicCommandWrite.onCharacteristicChanged(value);
            return true;
        } else if (characteristicActivityData.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicActivityData.onCharacteristicChanged(value);
            return true;
        } else if (characteristicDataUpload != null && characteristicDataUpload.getCharacteristicUUID().equals(characteristicUUID)) {
            characteristicDataUpload.onCharacteristicChanged(value);
            return true;
        }

        return false;
    }

    @Override
    public void onMtuChanged(final BluetoothGatt gatt, final int mtu, final int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return;
        }

        if (characteristicCommandRead != null)
            characteristicCommandRead.setMtu(mtu);
        if (characteristicCommandWrite != null)
            characteristicCommandWrite.setMtu(mtu);
        if (characteristicDataUpload != null)
            characteristicDataUpload.setMtu(mtu);
        if (characteristicActivityData != null)
            characteristicActivityData.setMtu(mtu);
    }

    @Override
    public void onAuthSuccess() {
        characteristicCommandRead.reset();
        characteristicCommandWrite.reset();
        characteristicActivityData.reset();
        if (characteristicDataUpload != null) {
            characteristicDataUpload.reset();
        }
    }

    @Override
    public void sendCommand(final String taskName, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("Unable to {}, characteristicCommandWrite is null", taskName);
            return;
        }

        this.characteristicCommandWrite.write(taskName, command.toByteArray());
    }

    @Override
    public void sendCommand(final TransactionBuilder builder, final XiaomiProto.Command command) {
        if (this.characteristicCommandWrite == null) {
            // Can sometimes happen in race conditions when connecting + receiving calendar event or weather updates
            LOG.warn("Unable to {}, characteristicCommandWrite is null", builder.getTaskName());
            return;
        }

        this.characteristicCommandWrite.write(builder, command.toByteArray());
    }

    @Override
    public void sendDataChunk(final String taskName, final byte[] chunk, @Nullable final XiaomiSendCallback callback) {
        if (this.characteristicDataUpload == null) {
            LOG.warn("Unable to {}, characteristicDataUpload is null!", taskName);
            return;
        }

        this.characteristicDataUpload.write(taskName, chunk, callback);
    }
}
