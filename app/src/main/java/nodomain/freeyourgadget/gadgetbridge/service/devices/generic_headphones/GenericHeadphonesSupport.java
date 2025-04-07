package nodomain.freeyourgadget.gadgetbridge.service.devices.generic_headphones;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothDisconnectReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.HeadphoneHelper;

public class GenericHeadphonesSupport extends AbstractDeviceSupport implements HeadphoneHelper.Callback {
    //the following constants are annotated as systemApi, hence cannot be referenced directly
    //they are reported as working in https://community.home-assistant.io/t/bluetooth-battery-levels-android/661525
    private static final String ANDROID_BLUETOOTH_DEVICE_EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL";
    private static final String ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED";

    private HeadphoneHelper headphoneHelper;
    private BluetoothDisconnectReceiver mBlueToothDisconnectReceiver = null;
    private BroadcastReceiver batteryLevelReceiver = null;

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
        if (batteryLevelReceiver != null) {
            getContext().unregisterReceiver(batteryLevelReceiver);
            batteryLevelReceiver = null;
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

        if (GBApplication.isRunningPieOrLater()) {
            batteryLevelReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    if (GBApplication.isRunningPieOrLater()) {
                        final String action = intent.getAction();

                        if (ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED.equals(action)) {
                            final int batteryLevel = intent.getIntExtra(ANDROID_BLUETOOTH_DEVICE_EXTRA_BATTERY_LEVEL, -1);
                            final GBDeviceEventBatteryInfo eventBatteryInfo = new GBDeviceEventBatteryInfo();
                            eventBatteryInfo.state = batteryLevel == -1 ? BatteryState.UNKNOWN : BatteryState.BATTERY_NORMAL;
                            eventBatteryInfo.level = batteryLevel;
                            evaluateGBDeviceEvent(eventBatteryInfo);
                        }
                    }
                }
            };
            ContextCompat.registerReceiver(getContext(), batteryLevelReceiver, new IntentFilter(ANDROID_BLUETOOTH_DEVICE_ACTION_BATTERY_LEVEL_CHANGED), ContextCompat.RECEIVER_EXPORTED);
        }

        mBlueToothDisconnectReceiver = new BluetoothDisconnectReceiver();
        ContextCompat.registerReceiver(getContext(), mBlueToothDisconnectReceiver, new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED), ContextCompat.RECEIVER_EXPORTED);
        return true;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

}
