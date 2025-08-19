/*  Copyright (C) 2022-2024 José Rebelo, octospacc

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
package nodomain.freeyourgadget.gadgetbridge.externalevents;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.PeriodicDbExporter;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;
import nodomain.freeyourgadget.gadgetbridge.util.backup.PeriodicZipExporter;

public class IntentApiReceiver extends BroadcastReceiver {
    private static final Logger LOG = LoggerFactory.getLogger(IntentApiReceiver.class);

    private static final String msgDebugNotAllowed = "Intent API Allow Debug Commands not allowed";

    public static final String COMMAND_ACTIVITY_SYNC = "nodomain.freeyourgadget.gadgetbridge.command.ACTIVITY_SYNC";
    @Deprecated
    public static final String COMMAND_TRIGGER_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_EXPORT";
    public static final String TRIGGER_DATABASE_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_DATABASE_EXPORT";
    public static final String COMMAND_TRIGGER_ZIP_EXPORT = "nodomain.freeyourgadget.gadgetbridge.command.TRIGGER_ZIP_EXPORT";
    public static final String COMMAND_DEBUG_SEND_NOTIFICATION = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_SEND_NOTIFICATION";
    public static final String COMMAND_DEBUG_INCOMING_CALL = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_INCOMING_CALL";
    public static final String COMMAND_DEBUG_END_CALL = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_END_CALL";
    public static final String COMMAND_DEBUG_SET_DEVICE_ADDRESS = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_SET_DEVICE_ADDRESS";
    public static final String COMMAND_DEBUG_SET_DEVICE_TYPE = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_SET_DEVICE_TYPE";
    public static final String COMMAND_DEBUG_TEST_NEW_FUNCTION = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_TEST_NEW_FUNCTION";
    public static final String COMMAND_DEBUG_HEAP_DUMP = "nodomain.freeyourgadget.gadgetbridge.command.DEBUG_HEAP_DUMP";

    private static final String MAC_ADDR_PATTERN = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction() == null) {
            LOG.warn("Action is null");
            return;
        }

        final Prefs prefs = GBApplication.getPrefs();

        switch (intent.getAction()) {
            case COMMAND_ACTIVITY_SYNC:
                if (!prefs.getBoolean("intent_api_allow_activity_sync", false)) {
                    LOG.warn("Intent API activity sync trigger not allowed");
                    return;
                }

                final int dataTypes;
                final String dataTypesHex = intent.getStringExtra("dataTypesHex");
                if (dataTypesHex != null) {
                    final Matcher matcher = Pattern.compile("^0[xX]([0-9a-fA-F]+)$").matcher(dataTypesHex);
                    if (!matcher.find()) {
                        LOG.warn("Failed to parse dataTypesHex '{}' as hex", dataTypesHex);
                        return;
                    }
                    dataTypes = Integer.parseInt(Objects.requireNonNull(matcher.group(1)), 16);
                } else {
                    dataTypes = RecordedDataTypes.TYPE_SYNC;
                }

                LOG.info("Triggering activity sync for data types {}", String.format("0x%08x", dataTypes));

                GBApplication.deviceService().onFetchRecordedData(dataTypes);
                break;

            case COMMAND_TRIGGER_EXPORT:
                LOG.warn(
                        "The action {} is deprecated, please use {}",
                        COMMAND_TRIGGER_EXPORT,
                        TRIGGER_DATABASE_EXPORT
                );
            case TRIGGER_DATABASE_EXPORT:
                if (!prefs.getBoolean("intent_api_allow_trigger_export", false)) {
                    LOG.warn("Intent API db export trigger not allowed");
                    return;
                }

                LOG.info("Triggering db export");

                PeriodicDbExporter.INSTANCE.executeNow();
                break;

            case COMMAND_TRIGGER_ZIP_EXPORT:
                if (!prefs.getBoolean("intent_api_allow_trigger_zip_export", false)) {
                    LOG.warn("Intent API zip export trigger not allowed");
                    return;
                }

                LOG.info("Triggering zip export");

                PeriodicZipExporter.INSTANCE.executeNow();
                break;

            case COMMAND_DEBUG_SEND_NOTIFICATION:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug Send notification message");
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.sender = intent.getStringExtra("sender");
                if (notificationSpec.sender == null) {
                    notificationSpec.sender = "DEBUG Sender";
                }
                notificationSpec.phoneNumber = intent.getStringExtra("phoneNumber");
                if (notificationSpec.phoneNumber == null) {
                    notificationSpec.phoneNumber = "DEBUG PhoneNumber";
                }
                notificationSpec.subject = intent.getStringExtra("subject");
                if (notificationSpec.subject == null) {
                    notificationSpec.subject = "DEBUG Subject";
                }
                notificationSpec.body = intent.getStringExtra("body");
                if (notificationSpec.body == null) {
                    notificationSpec.body = "DEBUG Body";
                }
                notificationSpec.type = NotificationType.GENERIC_SMS;
                if (intent.getStringExtra("type") != null) {
                    try {
                        notificationSpec.type = NotificationType.valueOf(intent.getStringExtra("type"));
                    } catch (IllegalArgumentException e) {
                        LOG.error("Failed to parse notification type {}", intent.getStringExtra("type"), e);
                    }
                }
                if (notificationSpec.type != NotificationType.GENERIC_SMS) {
                    // SMS notifications don't have a source app ID when sent by the SMSReceiver,
                    // so let's not set it here as well for consistency
                    notificationSpec.sourceAppId = BuildConfig.APPLICATION_ID;
                }
                notificationSpec.sourceName = context.getApplicationInfo()
                        .loadLabel(context.getPackageManager())
                        .toString();
                notificationSpec.attachedActions = new ArrayList<>();
                if (notificationSpec.type == NotificationType.GENERIC_SMS) {
                    // REPLY action
                    NotificationSpec.Action replyAction = new NotificationSpec.Action();
                    replyAction.title = context.getString(R.string._pebble_watch_reply);
                    replyAction.type = NotificationSpec.Action.TYPE_SYNTECTIC_REPLY_PHONENR;
                    notificationSpec.attachedActions.add(replyAction);
                }
                GBApplication.deviceService().onNotification(notificationSpec);
                break;

            case COMMAND_DEBUG_INCOMING_CALL:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug Incoming Call");
                CallSpec callSpec = new CallSpec();
                callSpec.command = CallSpec.CALL_INCOMING;
                callSpec.number = intent.getStringExtra("caller");
                if (callSpec.number == null) {
                    callSpec.number = "DEBUG_INCOMING_CALL";
                }
                GBApplication.deviceService().onSetCallState(callSpec);
                break;

            case COMMAND_DEBUG_END_CALL:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug End Call");
                CallSpec callSpecEnd = new CallSpec();
                callSpecEnd.command = CallSpec.CALL_END;
                GBApplication.deviceService().onSetCallState(callSpecEnd);
                break;

            case COMMAND_DEBUG_SET_DEVICE_ADDRESS:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                setDeviceAddress(intent);
                break;

            case COMMAND_DEBUG_SET_DEVICE_TYPE:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                setDeviceType(intent);
                break;

            case COMMAND_DEBUG_TEST_NEW_FUNCTION:
                if (!prefs.getBoolean("intent_api_allow_debug_commands", false)) {
                    LOG.warn(msgDebugNotAllowed);
                    return;
                }
                LOG.info("Triggering Debug Test New Function");
                GBApplication.deviceService().onTestNewFunction();
                break;

            case COMMAND_DEBUG_HEAP_DUMP:
                final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.US);
                try {
                    final File externalFilesDir = FileUtils.getExternalFilesDir();
                    final String filename = SDF.format(new Date()) + ".hprof";
                    final File dumpFolder = new File(externalFilesDir, "heapdump");
                    //noinspection ResultOfMethodCallIgnored
                    dumpFolder.mkdirs();
                    final File dumpFile = new File(dumpFolder, filename);
                    LOG.debug("Triggering heap dump to {}", dumpFile.getAbsolutePath());
                    android.os.Debug.dumpHprofData(dumpFile.getAbsolutePath());
                } catch (final Exception e) {
                    LOG.error("Heap dump failed", e);
                }
                break;

            default:
                LOG.warn("Got unknown intent API action: {}", intent.getAction());
        }
    }

    public IntentFilter buildFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(COMMAND_ACTIVITY_SYNC);
        intentFilter.addAction(COMMAND_TRIGGER_EXPORT);
        intentFilter.addAction(COMMAND_DEBUG_SEND_NOTIFICATION);
        intentFilter.addAction(COMMAND_DEBUG_INCOMING_CALL);
        intentFilter.addAction(COMMAND_DEBUG_END_CALL);
        intentFilter.addAction(COMMAND_DEBUG_SET_DEVICE_ADDRESS);
        intentFilter.addAction(COMMAND_DEBUG_SET_DEVICE_TYPE);
        intentFilter.addAction(COMMAND_DEBUG_TEST_NEW_FUNCTION);
        intentFilter.addAction(COMMAND_DEBUG_HEAP_DUMP);
        return intentFilter;
    }

    private void setDeviceAddress(final Intent intent) {
        final String oldAddress = intent.getStringExtra("oldAddress");
        if (!validAddress(oldAddress)) {
            return;
        }

        final String newAddress = intent.getStringExtra("newAddress");
        if (!validAddress(newAddress)) {
            return;
        }

        if (oldAddress.equals(newAddress)) {
            LOG.warn("Old and new addresses are the same");
            return;
        }

        final GBDevice oldDevice = GBApplication.app()
                .getDeviceManager()
                .getDeviceByAddress(oldAddress);
        if (oldDevice == null) {
            LOG.warn("Old device with address {} not found", oldAddress);
            return;
        }

        final GBDevice newDevice = GBApplication.app()
                .getDeviceManager()
                .getDeviceByAddress(newAddress);
        if (newDevice != null) {
            LOG.warn("New device address {} already exists", newAddress);
            return;
        }

        LOG.info("Updating device address from {} to {}", oldAddress, newAddress);

        final SharedPreferences settingsOld = GBApplication.getDeviceSpecificSharedPrefs(oldAddress);
        final SharedPreferences settingsNew = GBApplication.getDeviceSpecificSharedPrefs(newAddress);
        final SharedPreferences.Editor editorNew = settingsNew.edit().clear();
        final Map<String, ?> allSettings = settingsOld.getAll();
        LOG.debug("Copying {} preferences to new device", allSettings.size());
        for (final Map.Entry<String, ?> e : allSettings.entrySet()) {
            if (e.getValue().getClass().equals(Boolean.class)) {
                editorNew.putBoolean(e.getKey(), (Boolean) e.getValue());
            } else if (e.getValue().getClass().equals(Float.class)) {
                editorNew.putFloat(e.getKey(), (Float) e.getValue());
            } else if (e.getValue().getClass().equals(Integer.class)) {
                editorNew.putInt(e.getKey(), (Integer) e.getValue());
            } else if (e.getValue().getClass().equals(Long.class)) {
                editorNew.putLong(e.getKey(), (Long) e.getValue());
            } else if (e.getValue().getClass().equals(String.class)) {
                editorNew.putString(e.getKey(), (String) e.getValue());
            } else if (e.getValue().getClass().equals(HashSet.class)) {
                //noinspection unchecked
                editorNew.putStringSet(e.getKey(), (HashSet<String>) e.getValue());
            } else {
                LOG.error("Unexpected preference type {}", e.getValue().getClass());
                return;
            }
        }
        if (!editorNew.commit()) {
            LOG.error("Failed to persist preferences for new address");
            return;
        }

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            DBHelper.updateDeviceMacAddress(session, oldAddress, newAddress);
        } catch (final Exception e) {
            LOG.error("Failed to update device address", e);
            return;
        }

        LOG.info("Quitting GB after device address update");

        GBApplication.quit();
    }

    private void setDeviceType(final Intent intent) {
        final String address = intent.getStringExtra("address");
        if (!validAddress(address)) {
            return;
        }

        final GBDevice device = GBApplication.app()
                .getDeviceManager()
                .getDeviceByAddress(address);
        if (device == null) {
            LOG.error("Device with address {} not found", address);
            return;
        }

        final DeviceType newType;
        try {
            newType = DeviceType.valueOf(intent.getStringExtra("type"));
        } catch (final Exception e) {
            LOG.error("Invalid new device type", e);
            return;
        }

        LOG.info("Updating device type from {} to {}", device.getType(), newType);

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            final DaoSession session = dbHandler.getDaoSession();
            DBHelper.updateDeviceType(session, device.getAddress(), newType);
        } catch (final Exception e) {
            LOG.error("Failed to update device type", e);
            return;
        }

        LOG.info("Quitting GB after device type update");

        GBApplication.quit();
    }

    /**
     * @noinspection BooleanMethodIsAlwaysInverted
     */
    private boolean validAddress(final String address) {
        if (address == null) {
            return false;
        }

        if (!address.matches(MAC_ADDR_PATTERN)) {
            LOG.warn("Device address '{}' does not match '{}'", address, MAC_ADDR_PATTERN);
            return false;
        }

        return true;
    }
}
