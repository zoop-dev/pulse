package nodomain.freeyourgadget.gadgetbridge.service.devices.generic_headphones;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothDisconnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.HeadphoneHelper;

public class GenericHeadphonesSupport extends AbstractDeviceSupport implements HeadphoneHelper.Callback {

    private HeadphoneHelper headphoneHelper;
    private BluetoothDisconnectReceiver mBlueToothDisconnectReceiver = null;

    private final BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            gbDevice.setState(GBDevice.State.INITIALIZED);
            gbDevice.sendDeviceUpdateIntent(getContext());
        }

        @Override
        public void onServiceDisconnected(int profile) {

        }
    };

    @Override
    public void onSetCallState(CallSpec callSpec) {
        headphoneHelper.onSetCallState(callSpec);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        headphoneHelper.onNotification(notificationSpec);
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);
        headphoneHelper = new HeadphoneHelper(getContext(), getDevice(), this);
    }

    @Override
    public void onSendConfiguration(String config) {
        if (!headphoneHelper.onSendConfiguration(config))
            super.onSendConfiguration(config);
    }

    @Override
    public void dispose() {
        if (headphoneHelper != null) {
            headphoneHelper.dispose();
            headphoneHelper = null;
        }
        if (mBlueToothDisconnectReceiver != null) {
            getContext().unregisterReceiver(mBlueToothDisconnectReceiver);
            mBlueToothDisconnectReceiver = null;
        }
    }

    @Override
    public boolean connect() {
        if (isConnected()) {
            return false;
        }
        gbDevice.setState(GBDevice.State.CONNECTING);
        gbDevice.sendDeviceUpdateIntent(getContext(), GBDevice.DeviceUpdateSubject.CONNECTION_STATE);

        final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
        versionCmd.fwVersion2 = "N/A";
        handleGBDeviceEvent(versionCmd);

        getBluetoothAdapter().getProfileProxy(getContext(), profileListener, BluetoothProfile.HEADSET);

        mBlueToothDisconnectReceiver = new BluetoothDisconnectReceiver();
        ContextCompat.registerReceiver(getContext(), mBlueToothDisconnectReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED), ContextCompat.RECEIVER_EXPORTED);
        return true;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

}
