/*  Copyright (C) 2023-2024 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.devices.divoom;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.activities.install.FwAppInstallerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.install.InstallActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class PixooInstallHandler implements InstallHandler {
    private static final Logger LOG = LoggerFactory.getLogger(PixooInstallHandler.class);

    private final Context mContext;
    private final Uri mUri;

    private Bitmap incomingBitmap;

    public PixooInstallHandler(Uri uri, Context context) {
        mContext = context;
        mUri = uri;

        try {
            incomingBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), mUri);
        } catch (final IOException e) {
            LOG.error("Could not decode image", e);
        }
    }

    @NonNull
    @Override
    public Class<? extends Activity> getInstallActivity() {
        return FwAppInstallerActivity.class;
    }

    @Override
    public boolean isValid() {
        return incomingBitmap != null;
    }

    @Override
    public void validateInstallation(InstallActivity installActivity, GBDevice device) {
        installActivity.setInstallEnabled(true);
    }

    @Override
    public void onStartInstall(GBDevice device) {

    }
}
