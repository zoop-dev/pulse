package nodomain.freeyourgadget.gadgetbridge.devices.generic_headphones;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.generic_headphones.GenericHeadphonesSupport;

public class GenericHeadphonesCoordinator extends AbstractDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(GenericHeadphonesCoordinator.class);

    @Override
    public int getOrderPriority() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        // Not needed
        return false;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        try {
            final BluetoothDevice device = candidate.getDevice();
            final BluetoothClass deviceClass = device.getBluetoothClass();
            if (deviceClass == null) return false;
            int deviceType = deviceClass.getDeviceClass();
            return deviceType == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET ||
                    deviceType == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                    deviceType == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
                    deviceType == BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER ||
                    deviceType == BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO ||
                    deviceType == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE;
        } catch (final SecurityException e) {
            // Should never happen - we must have bluetooth permissions
            LOG.error("Failed to check bluetooth class", e);
        }

        return false;
    }

    @Override
    public String getManufacturer() {
        return "Generic";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return GenericHeadphonesSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_generic_headphones;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_headphones;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public boolean supportsOSBatteryLevel(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceKind.HEADPHONES;
    }
}
