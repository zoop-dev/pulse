package nodomain.freeyourgadget.gadgetbridge.devices.atctlsrpaper;

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.atctlsrpaper.ATCTLSRPaperDeviceSupport;

public class ATCTLSRPaperCoordinator extends AbstractDeviceCoordinator {
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_atc_tlsr_paper;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_default;
    }

    @Override
    public String getManufacturer() {
        return "atc1441";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass(final GBDevice device) {
        return ATCTLSRPaperDeviceSupport.class;
    }
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("ATC_.*");
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid ATCTLSRPaperService = new ParcelUuid(ATCTLSRPaperDeviceSupport.UUID_SERVICE_MAIN);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ATCTLSRPaperService).build();
        return Collections.singletonList(filter);
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_LAZY;
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        ATCTLSRPaperInstallHandler installHandler = new ATCTLSRPaperInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }
}
