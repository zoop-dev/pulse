/*  Copyright (C) 2022-2024 Arjan Schrijver, Damien Gaignon, Daniel Dakhno,
    Petr Vaněk

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
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.WidgetPreferenceStorage;

public class SleepAlarmWidgetConfigurationActivity extends Activity implements GBActivity {

    // modified copy of WidgetConfigurationActivity
    // if we knew which widget is calling this config activity, we could only use a single configuration
    // activity and customize the filter in getAllDevices based on the caller.

    int mAppWidgetId;

    List<GBDevice> allDevices;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);

        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        // make the result intent and set the result to canceled
        Intent resultValueCanceled = new Intent();
        resultValueCanceled.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_CANCELED, resultValueCanceled);

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(SleepAlarmWidgetConfigurationActivity.this);
        builder.setTitle(R.string.widget_settings_select_device_title);

        allDevices = GBApplication.app().getDeviceManager().getDevices().stream()
                .filter(device -> {
                    final DeviceCoordinator coordinator = device.getDeviceCoordinator();
                    return coordinator.getAlarmSlotCount(device) > 0;
                }).collect(Collectors.toList());

        List<String> list = new ArrayList<>();
        for (GBDevice dev : allDevices) {
            list.add(dev.getAliasOrName());
        }
        String[] allDevicesString = list.toArray(new String[0]);

        builder.setSingleChoiceItems(allDevicesString, 0, (dialog, which) -> {
            ListView lw = ((AlertDialog) dialog).getListView();
            int selectedItemPosition = lw.getCheckedItemPosition();

            if (selectedItemPosition > -1) {
                final GBDevice selectedItem = allDevices.get(selectedItemPosition);
                WidgetPreferenceStorage widgetPreferenceStorage = new WidgetPreferenceStorage();
                widgetPreferenceStorage.saveWidgetPrefs(getApplicationContext(), String.valueOf(mAppWidgetId), selectedItem.getAddress());
            }
            Intent resultValueOk = new Intent();
            resultValueOk.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
            setResult(RESULT_OK, resultValueOk);
            finish();
        });
        builder.setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void setLanguage(Locale language, boolean invalidateLanguage) {
        AndroidUtils.setLanguage(this, language);
    }
}
