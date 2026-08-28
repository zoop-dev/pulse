package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.sport_x20;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.activities.multipoint.MultipointPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractHeadphoneSerialDeviceSupportV2;

public class SoundcoreSportX20DeviceSupport extends AbstractHeadphoneSerialDeviceSupportV2<SoundcoreSportX20Protocol> {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcoreSportX20DeviceSupport.class);

    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3968");

    private boolean multipointReceiverRegistered;

    public SoundcoreSportX20DeviceSupport() {
        addSupportedService(UUID_DEVICE_CTRL);
    }

    @Override
    protected SoundcoreSportX20Protocol createDeviceProtocol() {
        return new SoundcoreSportX20Protocol(getDevice());
    }

    @Override
    public void setContext(final GBDevice gbDevice, final BluetoothAdapter btAdapter, final Context context) {
        super.setContext(gbDevice, btAdapter, context);
        registerMultipointReceiver();
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            if (multipointReceiverRegistered) {
                LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(multipointReceiver);
                multipointReceiverRegistered = false;
            }
            super.dispose();
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        // 1. Request device info (battery, firmware, serial, embedded control functions)
        builder.write(mDeviceProtocol.encodeDeviceInfoRequest());
        // 2. Request extended info (firmware details, serial, additional settings)
        builder.write(mDeviceProtocol.encodeExtendedInfoRequest());
        // 3. Session-init handshake – the device ACKs with an empty response
        builder.write(mDeviceProtocol.encodeSessionInitRequest());
        builder.setDeviceState(GBDevice.State.INITIALIZED);
        return builder;
    }

    private void registerMultipointReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_ENABLE);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_DISABLE);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_GET_STATUS);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_CONNECT_DEVICE);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_DISCONNECT_DEVICE);
        intentFilter.addAction(MultipointPairingActivity.ACTION_MULTIPOINT_START_PAIRING);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(multipointReceiver, intentFilter);
        multipointReceiverRegistered = true;
    }

    private final BroadcastReceiver multipointReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final GBDevice intentDevice = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
            if (intentDevice == null || !intentDevice.getAddress().equals(getDevice().getAddress())) {
                return; // not for this device
            }

            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case MultipointPairingActivity.ACTION_MULTIPOINT_GET_STATUS:
                    mDeviceProtocol.broadcastMultipointStatus(isDualConnectionEnabled());
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_GET_DEVICES:
                    sendToDevice(mDeviceProtocol.encodePairedDevicesRequest());
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_ENABLE:
                    setDualConnection(true);
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_DISABLE:
                    setDualConnection(false);
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_START_PAIRING:
                    final boolean enabled = intent.getBooleanExtra(MultipointPairingActivity.EXTRA_PAIRING_ENABLED, false);
                    if (enabled) {
                        sendToDevice(mDeviceProtocol.encodeStartPairing());
                    }
                    mDeviceProtocol.broadcastMultipointPairing(enabled);
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_CONNECT_DEVICE:
                    connectDevice(intent.getStringExtra(MultipointPairingActivity.EXTRA_DEVICE_ADDRESS), true);
                    break;
                case MultipointPairingActivity.ACTION_MULTIPOINT_DISCONNECT_DEVICE:
                    connectDevice(intent.getStringExtra(MultipointPairingActivity.EXTRA_DEVICE_ADDRESS), false);
                    break;
                default:
                    LOG.warn("Unknown multipoint action {}", action);
            }
        }
    };

    private boolean isDualConnectionEnabled() {
        final SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        return prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_DUAL_CONNECTION, false);
    }

    private void setDualConnection(final boolean enabled) {
        GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).edit()
                .putBoolean(DeviceSettingsPreferenceConst.PREF_SOUNDCORE_DUAL_CONNECTION, enabled)
                .apply();
        sendToDevice(mDeviceProtocol.encodeDualConnection(enabled));
        mDeviceProtocol.broadcastMultipointStatus(enabled);
    }

    private void connectDevice(final String address, final boolean connect) {
        if (address == null) {
            return;
        }
        sendToDevice(connect
                ? mDeviceProtocol.encodeConnectDevice(address)
                : mDeviceProtocol.encodeDisconnectDevice(address));
        // Refresh the connection/active-audio status and the paired-device list afterwards.
        sendToDevice(mDeviceProtocol.encodeConnectionStatusRequest());
        sendToDevice(mDeviceProtocol.encodePairedDevicesRequest());
    }

    /**
     * Forgets/unpairs a paired device. Placeholder: the MultipointPairingActivity does not yet
     * expose a "forget device" action, but the command is implemented here for future use.
     */
    private void forgetDevice(final String address) {
        if (address == null) {
            return;
        }
        sendToDevice(mDeviceProtocol.encodeForgetDevice(address));
        // Refresh the paired-device list afterwards.
        sendToDevice(mDeviceProtocol.encodePairedDevicesRequest());
    }

    private void sendToDevice(final byte[] bytes) {
        if (bytes == null) {
            return;
        }
        final TransactionBuilder builder = createTransactionBuilder("multipoint");
        builder.write(bytes);
        builder.queue();
    }
}
