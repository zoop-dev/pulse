/*  Copyright (C) 2019-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniel Dakhno, Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.ItemWithDetails;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.QHybridSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.buttonconfig.ConfigPayload;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.NotificationUtils;

public class QHybridConfigActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(QHybridConfigActivity.class);

    PackageAdapter adapter;
    ArrayList<NotificationConfiguration> list;
    PackageConfigHelper helper;

    final int REQUEST_CODE_ADD_APP = 0;

    private boolean hasControl = false;

    SharedPreferences prefs;

    private GBDevice device;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);

        super.onCreate(savedInstanceState);

        if (device == null) {
            GB.toast(this, "Device is null", Toast.LENGTH_LONG, GB.ERROR);
            finish();
            return;
        }

        setContentView(R.layout.activity_qhybrid_settings);

//        findViewById(R.id.buttonOverwriteButtons).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                LocalBroadcastManager.getInstance(QHybridConfigActivity.this).sendBroadcast(new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS));
//            }
//        });


        setTitle(R.string.preferences_qhybrid_settings);

        ListView appList = findViewById(R.id.qhybrid_appList);

        try {
            helper = new PackageConfigHelper(getApplicationContext());
            list = helper.getNotificationConfigurations();
        } catch (Exception e) {
            GB.toast("error getting configurations", Toast.LENGTH_SHORT, GB.ERROR, e);
            list = new ArrayList<>();
        }
        // null is added to indicate the plus button added handled in PackageAdapter#getView
        list.add(null);
        appList.setAdapter(adapter = new PackageAdapter(this, R.layout.qhybrid_package_settings_item, list));
        appList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(final AdapterView<?> adapterView, View view, final int i, long l) {
                PopupMenu menu = new PopupMenu(QHybridConfigActivity.this, view);
                menu.getMenu().add(0, 0, 0, "edit");
                menu.getMenu().add(0, 1, 1, "delete");
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case 0: {
                                TimePicker picker = new TimePicker(QHybridConfigActivity.this, (NotificationConfiguration) adapterView.getItemAtPosition(i));
                                picker.finishListener = new TimePicker.OnFinishListener() {
                                    @Override
                                    public void onFinish(boolean success, NotificationConfiguration config) {
                                        setControl(false, null);
                                        if (success) {
                                            try {
                                                helper.saveNotificationConfiguration(config);
                                                final Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED);
                                                intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                LocalBroadcastManager.getInstance(QHybridConfigActivity.this).sendBroadcast(intent);
                                            } catch (Exception e) {
                                                GB.toast("error saving notification", Toast.LENGTH_SHORT, GB.ERROR, e);
                                            }
                                            refreshList();
                                        }
                                    }
                                };
                                picker.handsListener = QHybridConfigActivity.this::setHands;
                                picker.vibrationListener = QHybridConfigActivity.this::vibrate;
                                setControl(true, picker.getSettings());
                                break;
                            }
                            case 1: {
                                try {
                                    helper.deleteNotificationConfiguration((NotificationConfiguration) adapterView.getItemAtPosition(i));
                                    final Intent intent = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION_CONFIG_CHANGED);
                                    intent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                    LocalBroadcastManager.getInstance(QHybridConfigActivity.this).sendBroadcast(intent);
                                } catch (Exception e) {
                                    GB.toast("error deleting setting", Toast.LENGTH_SHORT, GB.ERROR, e);
                                }
                                refreshList();
                                break;
                            }
                        }
                        return true;
                    }
                });
                menu.show();
                return true;
            }
        });

        appList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent notificationIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_NOTIFICATION);
                notificationIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                notificationIntent.putExtra("CONFIG", (NotificationConfiguration) adapterView.getItemAtPosition(i));
                LocalBroadcastManager.getInstance(QHybridConfigActivity.this).sendBroadcast(notificationIntent);
            }
        });

        if (device.getType() == DeviceType.FOSSILQHYBRID && device.isInitialized() && device.getFirmwareVersion().charAt(2) == '0') {
            updateSettings();
        } else {
            setSettingsError(getString(R.string.watch_not_connected));
        }
    }

    @NonNull
    private String getDeviceInfoDetails(final String name) {
        ItemWithDetails deviceInfo = device.getDeviceInfo(name);
        return deviceInfo != null ? deviceInfo.getDetails() : "";
    }

    private void setSettingsEnabled(boolean enables) {
        findViewById(R.id.settingsLayout).setAlpha(enables ? 1f : 0.2f);
    }

    private void updateSettings() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ItemWithDetails item = device.getDeviceInfo(FossilWatchAdapter.ITEM_BUTTONS);
                String buttonJson = null;
                if(item != null) {
                    buttonJson = item.getDetails();
                }
                try {
                    JSONArray buttonConfig_;
                    if (buttonJson == null || buttonJson.isEmpty()) {
                        buttonConfig_ = new JSONArray(new String[]{"", "", ""});
                    }else{
                        buttonConfig_ = new JSONArray(buttonJson);
                    }

                    final JSONArray buttonConfig = buttonConfig_;

                    LinearLayout buttonLayout = findViewById(R.id.buttonConfigLayout);
                    buttonLayout.removeAllViews();
//                    findViewById(R.id.buttonOverwriteButtons).setVisibility(View.GONE);
                    final ConfigPayload[] payloads = ConfigPayload.values();
                    final String[] names = new String[payloads.length];
                    for (int i = 0; i < payloads.length; i++)
                        names[i] = payloads[i].getDescription();
                    for (int i = 0; i < buttonConfig.length(); i++) {
                        final int currentIndex = i;
                        String configName = buttonConfig.getString(i);
                        TextView buttonTextView = new TextView(QHybridConfigActivity.this);
                        buttonTextView.setTextSize(20);
                        try {
                            ConfigPayload payload = ConfigPayload.valueOf(configName);
                            buttonTextView.setText("Button " + (i + 1) + ": " + payload.getDescription());
                        } catch (IllegalArgumentException e) {
                            buttonTextView.setText("Button " + (i + 1) + ": Unknown");
                        }

                        buttonTextView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                AlertDialog dialog = new MaterialAlertDialogBuilder(QHybridConfigActivity.this)
                                        .setItems(names, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                                ConfigPayload selected = payloads[which];

                                                try {
                                                    buttonConfig.put(currentIndex, selected.toString());
                                                    device.addDeviceInfo(new GenericItem(FossilWatchAdapter.ITEM_BUTTONS, buttonConfig.toString()));
                                                    updateSettings();
                                                    Intent buttonIntent = new Intent(QHybridSupport.QHYBRID_COMMAND_OVERWRITE_BUTTONS);
                                                    buttonIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                                                    buttonIntent.putExtra(FossilWatchAdapter.ITEM_BUTTONS, buttonConfig.toString());
                                                    LocalBroadcastManager.getInstance(QHybridConfigActivity.this).sendBroadcast(buttonIntent);
                                                } catch (JSONException e) {
                                                    LOG.error("Failed to update button config", e);
                                                }
                                            }
                                        })
                                        .setTitle("Button " + (currentIndex + 1))
                                        .create();
                                dialog.show();
                            }
                        });

                        buttonLayout.addView(buttonTextView);
                    }
                } catch (JSONException e) {
                    GB.toast("error parsing button config", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }
        });
    }

    private void setControl(boolean control, NotificationConfiguration config) {
        if (hasControl == control) return;
        Intent intent = new Intent(control ? QHybridSupport.QHYBRID_COMMAND_CONTROL : QHybridSupport.QHYBRID_COMMAND_UNCONTROL);
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        this.hasControl = control;
    }

    private void setHands(NotificationConfiguration config) {
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_SET);
    }

    private void vibrate(NotificationConfiguration config) {
        sendControl(config, QHybridSupport.QHYBRID_COMMAND_VIBRATE);
    }

    private void sendControl(NotificationConfiguration config, String request) {
        Intent intent = new Intent(request);
        intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        intent.putExtra("CONFIG", config);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void refreshList() {
        list.clear();
        try {
            list.addAll(helper.getNotificationConfigurations());
        } catch (Exception e) {
            GB.toast("error getting configurations", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
        // null is added to indicate the plus button added handled in PackageAdapter#getView
        list.add(null);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        ContextCompat.registerReceiver(this, buttonReceiver, new IntentFilter(QHybridSupport.QHYBRID_EVENT_BUTTON_PRESS), ContextCompat.RECEIVER_NOT_EXPORTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(fileReceiver, new IntentFilter(QHybridSupport.QHYBRID_EVENT_FILE_UPLOADED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(buttonReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(fileReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            this.onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setSettingsError(final String error) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setSettingsEnabled(false);
                findViewById(R.id.settingsErrorText).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.settingsErrorText)).setText(error);
            }
        });
    }

    class PackageAdapter extends ArrayAdapter<NotificationConfiguration> {
        PackageAdapter(@NonNull Context context, int resource, @NonNull List<NotificationConfiguration> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            if (!(view instanceof RelativeLayout))
                view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.qhybrid_package_settings_item, null);
            NotificationConfiguration settings = getItem(position);

            if (settings == null) {
                Button addButton = new Button(QHybridConfigActivity.this);
                addButton.setText("+");
                addButton.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                addButton.setOnClickListener(view1 -> {
                    final Intent appChooserIntent = new Intent(QHybridConfigActivity.this, QHybridAppChooserActivity.class);
                    appChooserIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                    startActivityForResult(appChooserIntent, REQUEST_CODE_ADD_APP);
                });
                return addButton;
            }

            final Drawable appIcon = NotificationUtils.getAppIcon(getContext(), settings.getPackageName());
            if (appIcon != null) {
                ((ImageView) view.findViewById(R.id.packageIcon)).setImageDrawable(appIcon);
            }
            final int square_side = 100;
            ((TextView) view.findViewById(R.id.packageName)).setText(settings.getAppName());
            Bitmap bitmap = Bitmap.createBitmap(square_side, square_side, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);

            Paint black = new Paint();
            black.setColor(GBApplication.getTextColor(getContext()));
            black.setStyle(Paint.Style.STROKE);
            black.setStrokeWidth(5);

            c.drawCircle(square_side / 2, square_side / 2, square_side / 2 - 3, black);

            int center = square_side / 2;
            if (settings.getHour() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getHour())) * (square_side / 4)),
                        (float) (center - Math.cos(Math.toRadians(settings.getHour())) * (square_side / 4)),
                        black
                );
            }
            if (settings.getMin() != -1) {
                c.drawLine(
                        center,
                        center,
                        (float) (center + Math.sin(Math.toRadians(settings.getMin())) * (square_side / 3)),
                        (float) (center - Math.cos(Math.toRadians(settings.getMin())) * (square_side / 3)),
                        black
                );
            }

            ((ImageView) view.findViewById(R.id.packageClock)).setImageBitmap(bitmap);

            return view;
        }
    }

    BroadcastReceiver fileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean error = intent.getBooleanExtra("EXTRA_ERROR", false);
            if (error) {
                GB.toast(getString(R.string.qhybrid_buttons_overwrite_error), Toast.LENGTH_SHORT, GB.ERROR);
                return;
            }
            GB.toast(getString(R.string.qhybrid_buttons_overwrite_success), Toast.LENGTH_SHORT, GB.INFO);
        }
    };

    BroadcastReceiver buttonReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GB.toast("Button " + intent.getIntExtra("BUTTON", -1) + " pressed", Toast.LENGTH_SHORT, GB.INFO);
        }
    };

    BroadcastReceiver settingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            GB.toast("Setting updated", Toast.LENGTH_SHORT, GB.INFO);
            updateSettings();
        }
    };
}
