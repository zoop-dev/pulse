package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PWakeAppScreenshot implements HuaweiP2PManager.HuaweiWakeApp {
    @Override
    public boolean onWakeApp(HuaweiP2PManager manager, Uri uri) {
        if (HuaweiP2PScreenshotService.getRegisteredInstance(manager) == null) {
            new HuaweiP2PScreenshotService(manager).register();
        }

        HuaweiP2PScreenshotService screenshotService = HuaweiP2PScreenshotService.getRegisteredInstance(manager);

        screenshotService.sendNegotiateConfig();

        return true;
    }
}
