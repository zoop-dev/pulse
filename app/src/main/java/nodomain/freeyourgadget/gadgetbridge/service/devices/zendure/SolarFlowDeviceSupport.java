/*  Copyright (C) 2025 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.zendure;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALWAYS_ON_DISPLAY;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_ALLOW_BYPASS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_MAXIMUM_CHARGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BATTERY_MINIMUM_CHARGE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OFFGRID_MODE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_OUTPUT_POWER_GRID;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SolarEquipmentStatusActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


public class SolarFlowDeviceSupport extends AbstractBTLESingleDeviceSupport {
    public static final UUID UUID_CHARACTERISTIC_READ = UUID.fromString("0000c305-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_WRITE = UUID.fromString("0000c304-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_SERVICE_MAIN = UUID.fromString("0000a002-0000-1000-8000-00805f9b34fb");
    private static final Logger LOG = LoggerFactory.getLogger(SolarFlowDeviceSupport.class);
    private long messageId = 1;

    // cached values from various messages, named after keys from JSON
    private long messageIdReport = -1;
    private int solarPower1 = -1;
    private int solarPower2 = -1;
    private int solarPower3 = -1;
    private int solarPower4 = -1;
    private int gridOffPower = -1;
    private int gridOffMode = -1;
    private int outputHomePower = -1;
    private int gridReverse = -1;
    private int minSoc = -1;
    private int socSet = -1;
    private int outputLimit = -1;
    private int batteryTemp;
    private int hyperTmp;
    private int electricLevel = -1;
    private String deviceId;
    private int firmwareVersion = -1;
    private int lampSwitch = -1;

    private boolean isSettingsSynced = false;


    public SolarFlowDeviceSupport() {
        super(LOG);
        addSupportedService(UUID_SERVICE_MAIN);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        String message = new String(value, StandardCharsets.UTF_8);
        LOG.info("got message from SolarFlow {}", message);
        try {
            JSONObject jsonMessage = new JSONObject(message);
            if (jsonMessage.has("method")) {
                String method = jsonMessage.getString("method");

                switch (method) {
                    case "BLESPP" -> {
                        deviceId = jsonMessage.getString("deviceId");
                        JSONObject sendMessage = new JSONObject()
                                .put("messageId", messageId++)
                                .put("method", "BLESPP_OK");

                        sendMessage(sendMessage);

                        JSONObject sendMessageGetInfo = new JSONObject()
                                .put("messageId", messageId++)
                                .put("method", "getInfo")
                                .put("timestamp", System.currentTimeMillis() / 1000);
                        sendMessage(sendMessageGetInfo);
                    }
                    case "getInfo-rsp" -> {
                        JSONObject sendMessageGetAllInfos = new JSONObject()
                                .put("messageId", messageId++)
                                .put("deviceId", deviceId)
                                .put("method", "read")
                                .put("properties", new JSONArray().put("getAll"))
                                .put("timestamp", System.currentTimeMillis() / 1000);
                        sendMessage(sendMessageGetAllInfos);
                    }
                    case "report" -> {
                        long messageIdReport = jsonMessage.getLong("messageId");
                        if (messageIdReport != this.messageIdReport) {
                            if (this.messageIdReport == -1) { // first report
                                TransactionBuilder builder = createTransactionBuilder("setInitialized");
                                builder.setDeviceState(GBDevice.State.INITIALIZED);
                                builder.queue();
                            } else {
                                reportToStatusActivity();
                                if (!isSettingsSynced) {
                                    syncSettingsToPrefs();
                                    isSettingsSynced = true;
                                }
                            }
                            this.messageIdReport = messageIdReport;
                        }
                        if (jsonMessage.has("properties")) {
                            return handleReportProperties(jsonMessage.getJSONObject("properties"));
                        }
                        if (jsonMessage.has("packData")) {
                            return handleReportPackData(jsonMessage.getJSONArray("packData"));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            LOG.error("could not decode JSON: {}", e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.setDeviceState(GBDevice.State.INITIALIZING);
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        builder.requestMtu(512);
        builder.notify(UUID_CHARACTERISTIC_READ, true);
        return builder;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }


    @Override
    public void onSendConfiguration(final String config) {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        switch (config) {
            case PREF_BATTERY_MINIMUM_CHARGE -> {
                int minSoc = devicePrefs.getInt(PREF_BATTERY_MINIMUM_CHARGE, 10);
                if (minSoc < 0) minSoc = 0;
                if (minSoc > 50) minSoc = 50;
                sendWriteProperty("minSoc", minSoc * 10);
            }
            case PREF_BATTERY_MAXIMUM_CHARGE -> {
                int socSet = devicePrefs.getInt(PREF_BATTERY_MAXIMUM_CHARGE, 100);
                if (socSet < 70) socSet = 70;
                if (socSet > 100) socSet = 100;
                sendWriteProperty("socSet", socSet * 10);
            }
            case PREF_BATTERY_ALLOW_BYPASS -> {
                String batteryAllowBypass = devicePrefs.getString(PREF_BATTERY_ALLOW_BYPASS, "never");
                int gridReverse = switch (batteryAllowBypass) {
                    case "auto" -> 0;
                    case "always" -> 1;
                    default -> 2;
                };
                sendWriteProperty("gridReverse", gridReverse);
            }
            case PREF_OUTPUT_POWER_GRID -> {
                int outputLimit = devicePrefs.getInt(PREF_OUTPUT_POWER_GRID, 800);
                if (outputLimit < 0) outputLimit = 0;
                if (outputLimit > 2400) outputLimit = 2400;
                sendWriteProperty("outputLimit", outputLimit);
            }
            case PREF_OFFGRID_MODE -> {
                String offGridMode = devicePrefs.getString(PREF_OFFGRID_MODE, "off");
                int gridOffMode = switch (offGridMode) {
                    case "on" -> 0;
                    case "eco" -> 1;
                    default -> 2;
                };
                sendWriteProperty("gridOffMode", gridOffMode);
            }
            case PREF_ALWAYS_ON_DISPLAY -> {
                boolean lampSwitch = devicePrefs.getBoolean(PREF_ALWAYS_ON_DISPLAY, true);
                sendWriteProperty("lampSwitch", lampSwitch ? 1 : 0);
            }
            default -> LOG.warn("Unknown config changed: {}", config);
        }
    }

    private void sendWriteProperty(String property, Object value) {
        try {
            JSONObject sendMessageSetProperty = new JSONObject()
                    .put("messageId", messageId++)
                    .put("method", "write")
                    .put("timestamp", System.currentTimeMillis() / 1000)
                    .put("deviceId", deviceId)
                    .put("properties", new JSONObject()
                            .put(property, value));

            sendMessage(sendMessageSetProperty);
        } catch (JSONException e) {
            LOG.error("JSON error while constructing write property json: {}", e.getMessage());
        }


    }

    private void sendMessage(JSONObject message) {
        sendMessage(message.toString());
    }

    private boolean handleReportProperties(JSONObject properties) {
        try {
            if (properties.has("solarPower1")) {
                solarPower1 = properties.getInt("solarPower1");
            }
            if (properties.has("solarPower2")) {
                solarPower2 = properties.getInt("solarPower2");
            }
            if (properties.has("solarPower3")) {
                solarPower3 = properties.getInt("solarPower3");
            }
            if (properties.has("solarPower4")) {
                solarPower4 = properties.getInt("solarPower4");
            }
            if (properties.has("gridOffPower")) {
                gridOffPower = properties.getInt("gridOffPower");
            }
            if (properties.has("gridOffMode")) {
                gridOffMode = properties.getInt("gridOffMode");
            }
            if (properties.has("outputHomePower")) {
                outputHomePower = properties.getInt("outputHomePower");
            }
            if (properties.has("outputLimit")) {
                outputLimit = properties.getInt("outputLimit");
            }
            if (properties.has("minSoc")) {
                minSoc = properties.getInt("minSoc");
            }
            if (properties.has("socSet")) {
                socSet = properties.getInt("socSet");
            }
            if (properties.has("hyperTmp")) {
                hyperTmp = properties.getInt("hyperTmp");
            }
            if (properties.has("gridReverse")) {
                gridReverse = properties.getInt("gridReverse");
            }
            if (properties.has("electricLevel")) {
                int electricLevel = properties.getInt("electricLevel");
                if (electricLevel != this.electricLevel) {
                    this.electricLevel = electricLevel;
                    getDevice().setBatteryLevel(electricLevel); // update event will be sent when firmware gets read
                }
            }
            if (properties.has("MASTER")) {
                int firmwareVersion = properties.getInt("MASTER");
                if (firmwareVersion != this.firmwareVersion) {
                    this.firmwareVersion = firmwareVersion;
                    String firmwareString = ((firmwareVersion & 0xf000) >> 12) + "." + ((firmwareVersion & 0x0f00) >> 8) + "." + (firmwareVersion & 0x00ff);
                    getDevice().setFirmwareVersion(firmwareString);
                    getDevice().sendDeviceUpdateIntent(getContext());
                }
            }


        } catch (JSONException e) {
            LOG.error("JSON error while parsing report: {}", e.getMessage());
            return false;
        }
        return true;
    }

    private boolean handleReportPackData(JSONArray packArray) {
        try {
            JSONObject packData = packArray.getJSONObject(0); // FIXME, there can be more packs

            if (packData.has("maxTemp")) {
                batteryTemp = (packData.getInt("maxTemp") - 2731) / 10;
            }
        } catch (JSONException e) {
            LOG.error("JSON error while parsing pack data: {}", e.getMessage());
            return false;
        }
        return true;
    }

    private void sendMessage(String message) {
        TransactionBuilder builder = createTransactionBuilder("sendMessage");
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUID_CHARACTERISTIC_WRITE);
        if (characteristic != null && message != null) {
            builder.write(characteristic, message.getBytes(StandardCharsets.UTF_8));
            builder.queue();
        }
    }

    private void reportToStatusActivity() {
        Intent intent = new Intent(SolarEquipmentStatusActivity.ACTION_SEND_SOLAR_EQUIPMENT_STATUS)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_BATTERY_PCT, electricLevel)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_TEMP1, (hyperTmp - 2731) / 10)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_TEMP2, batteryTemp)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_OUTPUT1_WATT, outputHomePower)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_OUTPUT2_WATT, gridOffPower)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_PANEL1_WATT, solarPower1)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_PANEL2_WATT, solarPower2)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_PANEL3_WATT, solarPower3)
                .putExtra(SolarEquipmentStatusActivity.EXTRA_PANEL4_WATT, solarPower4);

        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        getContext().sendBroadcast(intent);
    }

    private void syncSettingsToPrefs() {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        SharedPreferences.Editor devicePrefsEdit = devicePrefs.getPreferences().edit();

        devicePrefsEdit.putString(PREF_BATTERY_MINIMUM_CHARGE, String.valueOf(minSoc/10));
        devicePrefsEdit.putString(PREF_BATTERY_MAXIMUM_CHARGE, String.valueOf(socSet/10));
        devicePrefsEdit.putString(PREF_OUTPUT_POWER_GRID, String.valueOf(outputLimit));
        devicePrefsEdit.putBoolean(PREF_ALWAYS_ON_DISPLAY, lampSwitch != 0);

        String offGridMode = switch (gridOffMode) {
            case 0 -> "on";
            case 1 -> "eco";
            default -> "off";
        };
        devicePrefsEdit.putString(PREF_OFFGRID_MODE, offGridMode);

        String batteryAllowBypass = switch (gridReverse) {
            case 0 -> "auto";
            case 1 -> "always";
            default -> "never";
        };
        devicePrefsEdit.putString(PREF_BATTERY_ALLOW_BYPASS, batteryAllowBypass);

        devicePrefsEdit.apply();
        devicePrefsEdit.commit();
    }
}
