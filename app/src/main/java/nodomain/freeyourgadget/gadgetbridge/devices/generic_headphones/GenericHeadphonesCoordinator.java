package nodomain.freeyourgadget.gadgetbridge.devices.generic_headphones;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_headphones.GenericHeadphonesSupport;

public class GenericHeadphonesCoordinator extends AbstractDeviceCoordinator {
    @Override
    public int getOrderPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        BluetoothDevice device = candidate.getDevice();
        BluetoothClass deviceClass = device.getBluetoothClass();
        int deviceType = deviceClass.getDeviceClass();
        return deviceType == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                deviceType == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                deviceType == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
                deviceType == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER ||
                deviceType == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                deviceType == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE;
    }

    @Override
    public String getManufacturer() {
        return "generic";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return GenericHeadphonesSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_unknown;

    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_headphones;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_headphones_disabled;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }
}
