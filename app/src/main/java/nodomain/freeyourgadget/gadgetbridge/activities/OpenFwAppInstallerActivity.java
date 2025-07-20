/*  Copyright (C) 2022-2024 Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class OpenFwAppInstallerActivity extends AbstractGBActivity {
    private static final int READ_REQUEST_CODE = 42;
    private TextView label;
    private GBDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            device = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        }
        if (device == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        setContentView(R.layout.activity_open_fw_app_installer);
        Button pickFileButton = findViewById(R.id.open_fw_installer_pick_button);
        label = findViewById(R.id.open_fw_installer_no_device);
        label.setText(String.format(getString(R.string.open_fw_installer_select_file), device.getAliasOrName()));

        pickFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, READ_REQUEST_CODE);
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            final InstallHandler installHandler = device.getDeviceCoordinator().findInstallHandler(
                    resultData.getData(),
                    GBApplication.app().getApplicationContext()
            );

            if (installHandler == null) {
                label.setText(R.string.fwinstaller_file_not_compatible_to_device);
                return;
            }

            final Intent startIntent = new Intent(OpenFwAppInstallerActivity.this, installHandler.getInstallActivity());
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
            startIntent.setAction(Intent.ACTION_VIEW);
            startIntent.setDataAndType(resultData.getData(), null);
            startActivity(startIntent);
        }
    }
}
