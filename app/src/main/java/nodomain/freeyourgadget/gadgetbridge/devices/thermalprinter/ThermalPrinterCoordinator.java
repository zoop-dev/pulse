package nodomain.freeyourgadget.gadgetbridge.devices.thermalprinter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCardAction;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.thermalprinter.GenericThermalPrinterSupport;

public class ThermalPrinterCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_bluetooth_printer;
    }

    @Override
    public String getManufacturer() {
        return "";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(GBDevice gbDevice) {
        return GenericThermalPrinterSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_generic_thermal_printer;
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.supportsService(GenericThermalPrinterSupport.discoveryService);
    }

    @Override
    public boolean supportsFlashing(@NonNull GBDevice device) {
        return true;
    }

    @Override
    public boolean addBatteryPollingSettings() {
        return true;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        final ImageFilePrinterHandler imageFilePrinterHandler = new ImageFilePrinterHandler(uri, context);
        if (imageFilePrinterHandler.isValid()) {
            return imageFilePrinterHandler;
        }
        return null;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_print_received_text);
        return deviceSpecificSettings;
    }

    @Override
    public List<DeviceCardAction> getCustomActions() {
        return Collections.singletonList(new ControlDeviceCardAction());
    }

    @Override
    public DeviceCoordinator.DeviceKind getDeviceKind(@NonNull GBDevice device) {
        return DeviceCoordinator.DeviceKind.UNKNOWN;
    }

    private static final class ControlDeviceCardAction implements DeviceCardAction {

        @Override
        public int getIcon(GBDevice device) {
            return R.drawable.ic_file_upload;
        }

        @Override
        public String getDescription(final GBDevice device, final Context context) {
            return context.getString(R.string.activity_print_image_print_button);
        }

        @Override
        public void onClick(final GBDevice device, final Context context) {

            final Intent startIntent = new Intent(context, SendToPrinterActivity.class);
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            context.startActivity(startIntent);
        }

    }
}
