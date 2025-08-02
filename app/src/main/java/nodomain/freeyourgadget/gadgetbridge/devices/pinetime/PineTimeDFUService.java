/*  Copyright (C) 2020-2024 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.pinetime;

import android.app.Activity;
import android.os.Build;

import androidx.annotation.RequiresApi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nordicsemi.android.dfu.DfuBaseService;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;

public class PineTimeDFUService extends DfuBaseService {
    private static final Logger LOG = LoggerFactory.getLogger(PineTimeDFUService.class);

    @Override
    protected Class<? extends Activity> getNotificationTarget() {
        return FwAppInstallerActivity.class;
    }

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public void onTimeout(int startId) {
        LOG.info("onTimeout startId={}", startId);
        super.onTimeout(startId);
    }

    @Override
    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    public void onTimeout(int startId, int fgsType) {
        LOG.info("onTimeout startId={} fgsType={}", startId, fgsType);
        super.onTimeout(startId, fgsType);
    }
}
