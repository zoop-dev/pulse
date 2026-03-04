package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.content.Context;
import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.devices.GpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiGpxRouteInstallHandler extends GpxRouteInstallHandler {
    public HuaweiGpxRouteInstallHandler(final Uri uri, final Context context) {
        super(uri, context);
    }

    @Override
    protected boolean isCompatible(final GBDevice device) {
        return HuaweiDeviceStateManager.get(device).supportsRouteUpload();
    }
}
