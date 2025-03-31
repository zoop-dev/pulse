/*  Copyright (C) 2025 Me7c7

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
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
