/*  Copyright (C) 2024 Arjan Schrijver

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.companion.CompanionDeviceManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.externalevents.NotificationListener;

public class PermissionsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(PermissionsUtils.class);

    public static final String CUSTOM_PERM_IGNORE_BATT_OPTIM = "custom_perm_ignore_battery_optimization";
    public static final String CUSTOM_PERM_NOTIFICATION_LISTENER = "custom_perm_notifications_listener";
    public static final String CUSTOM_PERM_NOTIFICATION_SERVICE = "custom_perm_notifications_service";
    public static final String CUSTOM_PERM_DISPLAY_OVER = "custom_perm_display_over";
    public static final String CUSTOM_PERM_INTERNET_HELPER = "nodomain.freeyourgadget.internethelper.INTERNET";
    public static final String PACKAGE_INTERNET_HELPER = "nodomain.freeyourgadget.internethelper";

    public static final List<String> specialPermissions = new ArrayList<>() {{
        add(CUSTOM_PERM_IGNORE_BATT_OPTIM);
        add(CUSTOM_PERM_NOTIFICATION_LISTENER);
        add(CUSTOM_PERM_NOTIFICATION_SERVICE);
        add(CUSTOM_PERM_DISPLAY_OVER);
        add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            add(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }};

    public static ArrayList<PermissionDetails> getRequiredPermissionsList(Activity activity) {
        final ArrayList<PermissionDetails> permissionsList = new ArrayList<>();
        int companionDevicesCount = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final CompanionDeviceManager manager = (CompanionDeviceManager) GBApplication.getContext().getSystemService(Context.COMPANION_DEVICE_SERVICE);
            companionDevicesCount = manager.getAssociations().size();
        }
        if (companionDevicesCount == 0) {
            addPermission(permissionsList, activity, CUSTOM_PERM_IGNORE_BATT_OPTIM,
                    R.string.permission_disable_doze_title, R.string.permission_disable_doze_summary,
                    false, R.drawable.ic_battery_saver, R.color.accent_coral);
        } else {
            LOG.info("Not requesting explicit battery optimization exemption due to paired Companion devices");
        }
        addPermission(permissionsList, activity, CUSTOM_PERM_NOTIFICATION_LISTENER,
                R.string.menuitem_notifications, R.string.permission_notifications_summary,
                false, R.drawable.ic_notification, R.color.accent_pink);
        addPermission(permissionsList, activity, CUSTOM_PERM_NOTIFICATION_SERVICE,
                R.string.permission_manage_dnd_title, R.string.permission_manage_dnd_summary,
                false, R.drawable.ic_dnd, R.color.accent_violet);
        addPermission(permissionsList, activity, CUSTOM_PERM_DISPLAY_OVER,
                R.string.permission_displayover_title, R.string.permission_displayover_summary,
                false, R.drawable.ic_smartphone, R.color.accent_blue);
        addPermission(permissionsList, activity, Manifest.permission.ACCESS_FINE_LOCATION,
                R.string.permission_fine_location_title, R.string.permission_fine_location_summary,
                true, R.drawable.ic_gps_location, R.color.accent_violet);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            addPermission(permissionsList, activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    R.string.permission_background_location_title, R.string.permission_background_location_summary,
                    false, R.drawable.ic_share_location, R.color.accent_violet);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
            addPermission(permissionsList, activity, Manifest.permission.BLUETOOTH,
                    R.string.permission_bluetooth_title, R.string.permission_bluetooth_summary,
                    true, R.drawable.ic_bluetooth, R.color.accent_blue);
            addPermission(permissionsList, activity, Manifest.permission.BLUETOOTH_ADMIN,
                    R.string.permission_bluetooth_admin_title, R.string.permission_bluetooth_admin_summary,
                    true, R.drawable.ic_bluetooth, R.color.accent_blue);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addPermission(permissionsList, activity, Manifest.permission.BLUETOOTH_SCAN,
                    R.string.permission_bluetooth_scan_title, R.string.permission_bluetooth_scan_summary,
                    true, R.drawable.ic_bluetooth_searching, R.color.accent_blue);
            addPermission(permissionsList, activity, Manifest.permission.BLUETOOTH_CONNECT,
                    R.string.permission_bluetooth_connect_title, R.string.permission_bluetooth_connect_summary,
                    true, R.drawable.ic_bluetooth_connected, R.color.accent_blue);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermission(permissionsList, activity, Manifest.permission.POST_NOTIFICATIONS,
                    R.string.permission_post_notification_title, R.string.permission_post_notification_summary,
                    true, R.drawable.ic_notifications, R.color.accent_mint);
        }
        if (isPermissionDeclared(activity, Manifest.permission.INTERNET)) {
            addPermission(permissionsList, activity, Manifest.permission.INTERNET,
                    R.string.permission_internet_access_title, R.string.permission_internet_access_summary,
                    false, R.drawable.ic_language, R.color.accent_mint);
        }
        if (!GBApplication.hasDirectInternetAccess() && AndroidUtils.isPackageInstalled(PACKAGE_INTERNET_HELPER)) {
            addPermission(permissionsList, activity, CUSTOM_PERM_INTERNET_HELPER,
                    R.string.internet_helper_permission_title, R.string.internet_helper_permission_summary,
                    false, R.drawable.ic_language, R.color.accent_mint);
        }
        addPermission(permissionsList, activity, Manifest.permission.READ_CONTACTS,
                R.string.permission_contacts_title, R.string.permission_contacts_summary,
                false, R.drawable.ic_phone_outline, R.color.pulse_ring_cal);
        addPermission(permissionsList, activity, Manifest.permission.READ_CALENDAR,
                R.string.permission_calendar_title, R.string.permission_calendar_summary,
                false, R.drawable.ic_calendar_month, R.color.accent_mint);
        addPermission(permissionsList, activity, Manifest.permission.RECEIVE_SMS,
                R.string.permission_receive_sms_title, R.string.permission_receive_sms_summary,
                false, R.drawable.ic_message_outline, R.color.accent_pink);
        addPermission(permissionsList, activity, Manifest.permission.SEND_SMS,
                R.string.permission_send_sms_title, R.string.permission_send_sms_summary,
                false, R.drawable.ic_message_outline, R.color.accent_pink);
        addPermission(permissionsList, activity, Manifest.permission.READ_CALL_LOG,
                R.string.permission_read_call_log_title, R.string.permission_read_call_log_summary,
                false, R.drawable.ic_phone_missed_outline, R.color.pulse_ring_cal);
        addPermission(permissionsList, activity, Manifest.permission.READ_PHONE_STATE,
                R.string.permission_read_phone_state_title, R.string.permission_read_phone_state_summary,
                false, R.drawable.ic_phone_outline, R.color.pulse_ring_cal);
        addPermission(permissionsList, activity, Manifest.permission.CALL_PHONE,
                R.string.permission_call_phone_title, R.string.permission_call_phone_summary,
                false, R.drawable.ic_phone_outline, R.color.pulse_ring_cal);
        addPermission(permissionsList, activity, Manifest.permission.PROCESS_OUTGOING_CALLS,
                R.string.permission_process_outgoing_calls_title, R.string.permission_process_outgoing_calls_summary,
                false, R.drawable.ic_phone_outline, R.color.pulse_ring_cal);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addPermission(permissionsList, activity, Manifest.permission.ANSWER_PHONE_CALLS,
                    R.string.permission_answer_phone_calls_title, R.string.permission_answer_phone_calls_summary,
                    false, R.drawable.ic_phone_outline, R.color.pulse_ring_cal);
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            addPermission(permissionsList, activity, Manifest.permission.READ_EXTERNAL_STORAGE,
                    R.string.permission_external_storage_title, R.string.permission_external_storage_summary,
                    false, R.drawable.ic_folder, R.color.accent_blue);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            addPermission(permissionsList, activity, Manifest.permission.QUERY_ALL_PACKAGES,
                    R.string.permission_query_all_packages_title, R.string.permission_query_all_packages_summary,
                    false, R.drawable.ic_smartphone, R.color.accent_violet);
        }
        return permissionsList;
    }

    public static boolean checkPermission(Context context, String permission) {
        switch (permission) {
            case CUSTOM_PERM_NOTIFICATION_LISTENER -> {
                Set<String> set = NotificationManagerCompat.getEnabledListenerPackages(context);
                return set.contains(context.getPackageName());
            }
            case CUSTOM_PERM_NOTIFICATION_SERVICE -> {
                return ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).isNotificationPolicyAccessGranted();
            }
            case CUSTOM_PERM_DISPLAY_OVER -> {
                return Settings.canDrawOverlays(context);
            }
            case CUSTOM_PERM_IGNORE_BATT_OPTIM -> {
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                return pm.isIgnoringBatteryOptimizations(context.getApplicationContext().getPackageName());
            }
            default -> {
                return ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_DENIED;
            }
        }
    }

    public static boolean checkAllPermissions(Activity activity) {
        boolean result = true;
        for (PermissionDetails permission : getRequiredPermissionsList(activity)) {
            if (!checkPermission(activity, permission.permission())) {
                result = false;
            }
        }
        return result;
    }

    public static void requestPermission(Activity activity, String permission) {
        if (permission.equals(CUSTOM_PERM_IGNORE_BATT_OPTIM)) {
            showRequestIgnoreBatteryOptimizationDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_NOTIFICATION_LISTENER)) {
            showNotifyListenerPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_NOTIFICATION_SERVICE)) {
            showNotifyPolicyPermissionsDialog(activity);
        } else if (permission.equals(CUSTOM_PERM_DISPLAY_OVER)) {
            showDisplayOverOthersPermissionsDialog(activity);
        } else if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION) && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)) {
            showBackgroundLocationPermissionsDialog(activity);
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{permission}, 0);
        }
    }

    public record PermissionDetails(String permission,
                                    String title,
                                    String summary,
                                    boolean required,
                                    int iconRes,
                                    int colorRes) {
    }

    private static void addPermission(final List<PermissionDetails> list, final Activity activity,
                                      final String permission, final int titleRes, final int summaryRes,
                                      final boolean required, final int iconRes, final int colorRes) {
        list.add(new PermissionDetails(permission, activity.getString(titleRes),
                activity.getString(summaryRes), required, iconRes, colorRes));
    }

    public static boolean isPermissionDeclared(Context context, String permission) {
        // Checks whether a permission has been declared in the (merged) manifest file.
        // This also includes permissions declared by dependencies.
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_PERMISSIONS
            );

            String[] requestedPermissions = info.requestedPermissions;
            if (requestedPermissions != null) {
                for (String p : requestedPermissions) {
                    if (p.equals(permission)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
        }
        return false;
    }

    @SuppressLint("BatteryLife")
    private static void showRequestIgnoreBatteryOptimizationDialog(Activity activity) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + activity.getApplicationContext().getPackageName()));
        activity.startActivity(intent);
    }

    private static void showNotifyListenerPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_notification_listener,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            Intent intent;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS);
                                intent.putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, new ComponentName(BuildConfig.APPLICATION_ID, NotificationListener.class.getName()).flattenToString());
                            } else {
                                intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            }
                            String showArgs = BuildConfig.APPLICATION_ID + "/" + NotificationListener.class.getName();
                            intent.putExtra(":settings:fragment_args_key", showArgs);
                            Bundle bundle = new Bundle();
                            bundle.putString(":settings:fragment_args_key", showArgs);
                            intent.putExtra(":settings:show_fragment_args", bundle);
                            activity.startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            GB.toast(activity, "'Notification Listener Settings' activity not found", Toast.LENGTH_LONG, GB.ERROR, e);
                            LOG.error("'Notification Listener Settings' activity not found");
                        }
                    }
                })
                .show();
    }

    private static void showNotifyPolicyPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_notification_policy_access,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    try {
                        activity.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                    } catch (ActivityNotFoundException e) {
                        GB.toast(activity, "'Notification Policy' activity not found", Toast.LENGTH_LONG, GB.ERROR, e);
                        LOG.error("'Notification Policy' activity not found");
                    }
                })
                .show();
    }

    private static void showDisplayOverOthersPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_display_over_other_apps,
                        activity.getString(R.string.app_name),
                        activity.getString(R.string.ok)))
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    Intent enableIntent = new Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    );
                    activity.startActivity(enableIntent);
                })
                .setNegativeButton(R.string.dismiss, (dialog, id) -> {
                })
                .show();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private static void showBackgroundLocationPermissionsDialog(Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setMessage(activity.getString(R.string.permission_location,
                        activity.getString(R.string.app_name),
                        activity.getPackageManager().getBackgroundPermissionOptionLabel()))
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
                })
                .show();
    }
}
