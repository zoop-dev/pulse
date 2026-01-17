/*  Copyright (C) 2024 Damien Gaignon, Martin.JM, Vitalii Tomin

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.CameraActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.App;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications.NotificationConstraintsType;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Watchface;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZonesSpec;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

import androidx.annotation.NonNull;

public class HuaweiState {
    Logger LOG = LoggerFactory.getLogger(HuaweiState.class);

    TreeMap<Integer, byte[]> commandsPerService = new TreeMap<>();
    // Each byte of expandCapabilities represent a "service"
    // Each bit in a "service" represent a feature so 1 or 0 is used to check is support or not
    byte[] expandCapabilities = null;
    byte notificationCapabilities = -0x01;
    ByteBuffer notificationConstraints = null;

    private boolean supportsTruSleepNewSync = false;
    private boolean supportsRriNewSync = false;
    private boolean supportsGpsNewSync = false;

    private boolean supportsWorkoutNewSteps = false;

    private Watchface.WatchfaceDeviceParams watchfaceDeviceParams;

    private App.AppDeviceParams appDeviceParams;

    private HuaweiMusicUtils.MusicCapabilities musicDeviceParams = null;

    private HuaweiMusicUtils.MusicCapabilities musicExtendedDeviceParams = null;

    private final String address;

    private int maxContactsCount = 0;

    private String otaSoftwareVersion = null;
    private int otaSignatureLength = 256;

    private boolean navigationAvailable = false;

    public HuaweiState(final String address) {
        this.address = address;

        // Set non-numeric capabilities
        this.expandCapabilities = GB.hexStringToByteArray(getCapabilitiesSharedPreferences().getString("expandCapabilities", "00"));
        this.notificationCapabilities = (byte) getCapabilitiesSharedPreferences().getInt("notificationCapabilities", -0x01);
        this.notificationConstraints = ByteBuffer.wrap(GB.hexStringToByteArray(
                getCapabilitiesSharedPreferences().getString(
                        "notificationConstraints",
                        GB.hexdump(Notifications.defaultConstraints)
                )));
        this.maxContactsCount = getCapabilitiesSharedPreferences().getInt("maxContactsCount", 0);

        for (String key : getCapabilitiesSharedPreferences().getAll().keySet()) {
            int service;
            try {
                service = Integer.parseInt(key);
                byte[] commands = GB.hexStringToByteArray(getCapabilitiesSharedPreferences().getString(key, "00"));
                this.commandsPerService.put(service, commands);
            } catch (NumberFormatException e) {
                // These are the non-numeric capabilities, which have been set already
            }
        }
    }

    private SharedPreferences getCapabilitiesSharedPreferences() {
        return GBApplication.getContext().getSharedPreferences(getCapabilitiesSharedPreferencesName(address), Context.MODE_PRIVATE);
    }

    public static String getCapabilitiesSharedPreferencesName(String address) {
        return "huawei_coordinator_capabilities_" + address.toLowerCase(Locale.ROOT).replace(":", "_");
    }

    private SharedPreferences getDeviceSpecificSharedPreferences(GBDevice gbDevice) {
        return GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());
    }

    public boolean getForceOption(GBDevice gbDevice, String option) {
        return getDeviceSpecificSharedPreferences(gbDevice).getBoolean(option, false);
    }

    private void saveCommandsForService(int service, byte[] commands) {
        commandsPerService.put(service, commands);
        getCapabilitiesSharedPreferences().edit().putString(String.valueOf(service), GB.hexdump(commands)).apply();
    }

    public void saveExpandCapabilities(byte[] capabilities) {
        expandCapabilities = capabilities;
        getCapabilitiesSharedPreferences().edit().putString("expandCapabilities", GB.hexdump(capabilities)).apply();
    }

    public void saveNotificationCapabilities(byte capabilities) {
        notificationCapabilities = capabilities;
        getCapabilitiesSharedPreferences().edit().putInt("notificationCapabilities", (int) capabilities).apply();
    }

    public void saveNotificationConstraints(ByteBuffer constraints) {
        notificationConstraints = constraints;
        getCapabilitiesSharedPreferences().edit().putString("notificationConstraints", GB.hexdump(constraints.array())).apply();
    }

    public void saveMaxContactsCount(int maxContactsCount) {
        this.maxContactsCount = maxContactsCount;
        getCapabilitiesSharedPreferences().edit().putInt("maxContactsCount", maxContactsCount).apply();
    }

    public void addCommandsForService(int service, byte[] commands) {
        if (!commandsPerService.containsKey(service)) {
            saveCommandsForService(service, commands);
            return;
        }
        byte[] saved = commandsPerService.get(service);
        if (saved == null) {
            saveCommandsForService(service, commands);
            return;
        }
        if (saved.length != commands.length) {
            saveCommandsForService(service, commands);
            return;
        }
        boolean changed = false;
        for (int i = 0; i < saved.length; i++) {
            if (saved[i] != commands[i]) {
                changed = true;
                break;
            }
        }
        if (changed)
            saveCommandsForService(service, commands);
    }

    public byte[] getCommandsForService(int service) {
        return commandsPerService.get(service);
    }

    // Print all Services ID and Commands ID
    public void printCommandsPerService() {
        StringBuilder msg = new StringBuilder();
        for (Map.Entry<Integer, byte[]> entry : commandsPerService.entrySet()) {
            msg.append("ServiceID: ").append(Integer.toHexString(entry.getKey())).append(" => Commands: ");
            for (byte b : entry.getValue()) {
                msg.append(Integer.toHexString(b)).append(" ");
            }
            msg.append("\n");
        }
        LOG.info(msg.toString());
    }

    private boolean supportsCommandForService(int service, int command) {
        byte[] commands = commandsPerService.get(service);
        if (commands == null)
            return false;
        for (byte b : commands)
            if (b == (byte) command)
                return true;
        return false;
    }

    private boolean supportsExpandCapability(int which) {
        // capability is a number containing :
        //  - the index of the "service"
        //  - the real capability number
        if (expandCapabilities == null) {
            LOG.debug("Expand capabilities is null");
            return false;
        }
        if (which >= expandCapabilities.length * 8) {
            LOG.debug("Capability is not supported");
            return false;
        }
        int capability = 1 << (which % 8);
        if ((expandCapabilities[which / 8] & capability) == capability) return true;
        return false;
    }

    private boolean supportsNotificationConstraint(byte which) {
        return notificationConstraints.get(which) == 0x01;
    }

    private int getNotificationConstraint(byte which) {
        return notificationConstraints.getShort(which);
    }

    public boolean supportsDateFormat() {
        return supportsCommandForService(0x01, 0x04);
    }

    public boolean supportsActivateOnLift() {
        return supportsCommandForService(0x01, 0x09);
    }

    public boolean supportsDoNotDisturb() {
        return supportsCommandForService(0x01, 0x0a);
    }

    public boolean supportsDoNotDisturb(GBDevice gbDevice) {
        return supportsDoNotDisturb() || getForceOption(gbDevice, PREF_FORCE_DND_SUPPORT);
    }

    public boolean supportsActivityType() {
        return supportsCommandForService(0x01, 0x12);
    }

    public boolean supportsWearLocation() {
        return supportsCommandForService(0x01, 0x1a);
    }

    public boolean supportsWearLocation(GBDevice gbDevice) {
        return supportsWearLocation() || getForceOption(gbDevice, PREF_FORCE_ENABLE_WEAR_LOCATION);
    }

    public boolean supportsRotateToCycleInfo() {
        return supportsCommandForService(0x01, 0x1b);
    }

    public boolean supportsQueryDndLiftWristDisturbType() {
        return supportsCommandForService(0x01, 0x1d);
    }

    public boolean supportsCameraRemote() {
        return supportsCommandForService(0x01, 0x29) && CameraActivity.supportsCamera();
    }

    public boolean supportsContacts() {
        return supportsCommandForService(0x03, 0x1);
    }

    public boolean supportsCalendarEvents() {
        return supportsP2PService() && supportsCalendar();
    }

    public boolean supportsAcceptAgreement() {
        return supportsCommandForService(0x01, 0x30);
    }

    public boolean supportsSettingRelated() {
        return supportsCommandForService(0x01, 0x31);
    }

    public boolean supportsTimeAndZoneId() {
        return supportsCommandForService(0x01, 0x32);
    }

    public boolean supportsConnectStatus() {
        return supportsCommandForService(0x01, 0x35);
    }

    public boolean supportsExpandCapability() {
        return supportsCommandForService(0x01, 0x37);
    }

    public boolean supportsNotificationAlert() {
        return supportsCommandForService(0x02, 0x01);
    }

    public boolean supportsNotification() {
        return supportsCommandForService(0x02, 0x04);
    }

    public boolean supportsWearMessagePush() {
        return supportsCommandForService(0x02, 0x08);
    }

    public boolean supportsMotionGoal() {
        return supportsCommandForService(0x07, 0x01);
    }

    public boolean supportsInactivityWarnings() {
        return supportsCommandForService(0x07, 0x06);
    }

    public boolean supportsActivityReminder() {
        return supportsCommandForService(0x07, 0x07);
    }

    public boolean supportsDeviceReportThreshold() {
        return supportsCommandForService(0x07, 0x0e);
    }

    public boolean supportsTruSleep() {
        return supportsCommandForService(0x07, 0x16);
    }

    public boolean supportsHeartRate() {
        return supportsCommandForService(0x07, 0x17);
    }

    public boolean supportsHeartRate(GBDevice gbDevice) {
        return supportsHeartRate() || getForceOption(gbDevice, PREF_FORCE_ENABLE_HEARTRATE_SUPPORT);
    }

    public boolean supportsRealtimeHeartRate() {
        return supportsCommandForService(0x07, 0x1c);
    }

    public boolean supportsHighHeartRateAlert() {
        return supportsCommandForService(0x07, 0x1d);
    }

    public boolean supportsLowHeartRateAlert() {
        return supportsCommandForService(0x07, 0x22);
    }

    public boolean supportsHeartRateZones() {
        return supportsCommandForService(0x07, 0x13);
    }

    public boolean supportsExtendedHeartRateZones() {
        return supportsCommandForService(0x07, 0x21);
    }

    public boolean supportsFitnessRestHeartRate() {
        return supportsCommandForService(0x07, 0x23);
    }

    public boolean supportsSPo2() {
        return supportsCommandForService(0x07, 0x24);
    }

    public boolean supportsSPo2(GBDevice gbDevice) {
        return supportsSPo2() || getForceOption(gbDevice, PREF_FORCE_ENABLE_SPO2_SUPPORT);
    }

    public boolean supportsLowSPo2Alert() {
        return supportsCommandForService(0x07, 0x25);
    }

    public boolean supportsRunPaceConfig() {
        return supportsCommandForService(0x07, 0x28);
    }

    public boolean supportsFitnessThresholdValue() {
        return supportsCommandForService(0x07, 0x29);
    }

    public boolean supportsFitnessThresholdValueV2() {
        return supportsExpandCapability(0x9a) || supportsExpandCapability(0x9c);
    }

    // 0x1d - SupportTemperature
    // 0xba - SupportTemperatureClassification
    // 0x43 - SupportTemperatureStudy
    public boolean supportsTemperature() {
        return supportsExpandCapability(0x1d);
    }

    public boolean supportsBloodPressure() {
        return supportsExpandCapability(0x3b);
    }

    public boolean supportsEventAlarm() {
        return supportsCommandForService(0x08, 0x01);
    }

    public boolean supportsSmartAlarm() {
        return supportsCommandForService(0x08, 0x02);
    }

    public boolean supportsSmartAlarm(GBDevice gbDevice) {
        return supportsSmartAlarm() || getForceOption(gbDevice, PREF_FORCE_ENABLE_SMART_ALARM);
    }

    public boolean supportsSmartAlarm(GBDevice gbDevice, int alarmPosition) {
        return supportsSmartAlarm(gbDevice) && alarmPosition == 0;
    }

    public boolean forcedSmartWakeup(GBDevice device, int alarmPosition) {
        return supportsSmartAlarm(device, alarmPosition) && alarmPosition == 0;
    }

    /**
     * @return True if alarms can be changed on the device, false otherwise
     */
    public boolean supportsChangingAlarm() {
        return supportsCommandForService(0x08, 0x03);
    }

    public boolean supportsNotificationOnBluetoothLoss() {
        return supportsCommandForService(0x0b, 0x03);
    }

    public boolean supportsLanguageSetting() {
        return supportsCommandForService(0x0c, 0x01);
    }

    public boolean supportsWatchfaceParams() {
        return supportsCommandForService(0x27, 0x01);
    }

    public boolean supportsAppParams() {
        return supportsCommandForService(0x2a, 0x06);
    }

    public boolean supportsMusicUploading() {
        return supportsCommandForService(0x25, 0x04);
    }

    public boolean supportsWeather() {
        return supportsCommandForService(0x0f, 0x01);
    }

    public boolean supportsWeatherUnit() {
        return supportsCommandForService(0x0f, 0x05);
    }

    public boolean supportsWeatherExtended() {
        return supportsCommandForService(0x0f, 0x06);
    }

    public boolean supportsWeatherErrorSimple() {
        return supportsCommandForService(0x0f, 0x07);
    }

    public boolean supportsWeatherForecasts() {
        return supportsCommandForService(0x0f, 0x08);
    }

    public boolean supportsWeatherMoonRiseSet() {
        return supportsCommandForService(0x0f, 0x0a);
    }

    public boolean supportsWeatherTides() {
        return supportsCommandForService(0x0f, 0x0b);
    }

    public boolean supportsWeatherErrorExtended() {
        return supportsCommandForService(0x0f, 0x0c);
    }

    public boolean supportsWeatherUvIndex() {
        return supportsExpandCapability(0x2f);
    }

    public boolean supportsWeatherExtendedHourForecast() {
        return supportsExpandCapability(0xc0);
    }

    public boolean supportsWorkouts() {
        return supportsCommandForService(0x17, 0x01);
    }

    public boolean supportsWorkoutCapability() {
        return supportsCommandForService(0x17, 0x15);
    }

    public boolean supportsWorkoutsTrustHeartRate() {
        return supportsCommandForService(0x17, 0x17);
    }

    public boolean supportsSendingGps() {
        return supportsCommandForService(0x18, 0x02);
    }

    public boolean supportsGpsAndTimeToDevice() {
        return supportsCommandForService(0x18, 0x06);
    }

    public boolean supportsAccount() {
        return supportsCommandForService(0x1A, 0x01);
    }

    public boolean supportsAccountJudgment() {
        return supportsCommandForService(0x1A, 0x05);
    }

    public boolean supportsAccountSwitch() {
        return supportsCommandForService(0x1A, 0x06);
    }

    public boolean supportsDiffAccountPairingOptimization() {
        if (supportsExpandCapability())
            return supportsExpandCapability(0xac);
        return false;
    }

    public boolean supportsMusic() {
        return supportsCommandForService(0x25, 0x02);
    }

    public boolean supportsAutoWorkMode() {
        return supportsCommandForService(0x26, 0x02);
    }

    public boolean supportsMenstrual() {
        return supportsCommandForService(0x32, 0x01);
    }

    public boolean supportsP2PService() {
        return supportsCommandForService(0x34, 0x1);
    }

    public boolean supportsOTAUpdate() {
        return supportsCommandForService(0x09, 0x01);
    }

    public boolean supportsOTAAutoUpdate() {
        return supportsCommandForService(0x09, 0x0c);
    }

    public boolean supportsOTANotify() {
        return supportsCommandForService(0x09, 0x0e);
    }

    public boolean supportsOTADeviceRequest() {
        return supportsCommandForService(0x09, 0x0f);
    }

    public boolean supportDefaultSwitch() {
        return supportsCommandForService(0x01, 0x21);
    }

    public boolean supportsAutoStress() {
        return supportsCommandForService(0x20, 0x09);
    }

    public boolean supportsSleepBreath() {
        return supportsCommandForService(0x2d, 0x01);
    }

    public boolean supportsP2PGetAppVersion() {
        if (supportsExpandCapability())
            return supportsExpandCapability(14);
        return false;
    }

    public boolean supportsExternalCalendarService() {
        if (supportsExpandCapability())
            return supportsExpandCapability(184);
        return false;
    }

    public boolean supportsTrack() {
        if (supportsExpandCapability())
            return supportsExpandCapability(0x36);
        return false;
    }

    public boolean supportsCalendar() {
        if (supportsExpandCapability())
            return supportsExpandCapability(171) || supportsExpandCapability(184);
        return false;
    }

    public boolean supportsMultiDevice() {
        if (supportsExpandCapability())
            return supportsExpandCapability(109);
        return false;
    }

    public boolean supportsDictSleepSync() {
        if (supportsExpandCapability())
            return supportsExpandCapability(143);
        return false;
    }

    public boolean supportsBedTime() {
        if (supportsExpandCapability())
            return supportsExpandCapability(199);
        return false;
    }

    public boolean supportsUnknownGender() {
        if (supportsExpandCapability())
            return supportsExpandCapability(0x57);
        return false;
    }

    public boolean supportsPrecisionWeight() {
        if (supportsExpandCapability())
            return supportsExpandCapability(0xb3);
        return false;
    }

    public boolean supportsMoreMusic() {
        if (supportsExpandCapability())
            return supportsExpandCapability(122);
        return false;
    }

    public boolean supportsNotificationsStartCall() {
        if (supportsExpandCapability())
            return supportsExpandCapability(74);
        return false;
    }

    public boolean supportsNotificationsAddIconTimestamp() {
        if (supportsExpandCapability())
            return supportsExpandCapability(77);
        return false;
    }

    public boolean supportsNotificationsReplyActions() {
        if (supportsExpandCapability())
            return supportsExpandCapability(73);
        return false;
    }

    public boolean supportsNotificationsRepeatedNotify() {
        if (supportsExpandCapability())
            return supportsExpandCapability(94);
        return false;
    }

    public boolean supportsNotificationsReply() {
        if (supportsExpandCapability())
            return supportsExpandCapability(89);
        return false;
    }

    public boolean supportsNotificationsRemoveSingle() {
        if (supportsExpandCapability())
            return supportsExpandCapability(120);
        return false;
    }

    public boolean supportsNotificationPicture() {
        if (supportsExpandCapability())
            return supportsExpandCapability(256);
        return false;
    }

    public boolean supportsVoipType1() {
        if (supportsExpandCapability())
            return supportsExpandCapability(249);
        return false;
    }

    public boolean supportsVoipType2() {
        if (supportsExpandCapability())
            return supportsExpandCapability(185);
        return false;
    }

    public boolean supportsVoipType3() {
        if (supportsExpandCapability())
            return supportsExpandCapability(152);
        return false;
    }

    public boolean supportsCannedReplies() {
        if (supportsExpandCapability())
            return supportsExpandCapability(82);
        return false;
    }

    public boolean supportsDeviceCommandConfig() {
        if (supportsExpandCapability())
            return supportsExpandCapability(83);
        return false;
    }

    public boolean supportsDeviceCommandEvent() {
        if (supportsExpandCapability())
            return supportsExpandCapability(84);
        return false;
    }

    public boolean supportsDeviceCommandData() {
        if (supportsExpandCapability())
            return supportsExpandCapability(85);
        return false;
    }

    public boolean supportsDeviceCommandDictData() {
        if (supportsExpandCapability())
            return supportsExpandCapability(173);
        return false;
    }

    public boolean supportsThreeCircle() {
        if (supportsExpandCapability())
            return supportsExpandCapability(154);
        return false;
    }

    public boolean supportsThreeCircleLite() {
        if (supportsExpandCapability())
            return supportsExpandCapability(156);
        return false;
    }

    public boolean supportsOTAChangelog() {
        if (supportsExpandCapability())
            return supportsExpandCapability(52);
        return false;
    }

    public boolean supportsOTASignature() {
        if (supportsExpandCapability())
            return supportsExpandCapability(144);
        return false;
    }

    public boolean supportsWiFiDirect() {
        if (supportsExpandCapability())
            return supportsExpandCapability(147);
        return false;
    }

    public boolean supportsReverseCapabilities() {
        if (supportsExpandCapability())
            return supportsExpandCapability(182);
        return false;
    }

    public boolean supportsFindDeviceAbility() {
        if (supportsExpandCapability())
            return supportsExpandCapability(79);
        return false;
    }

    public boolean supportsEmotion() {
        if (supportsExpandCapability())
            return supportsExpandCapability(206);
        return false;
    }

    //111 - resend capability
    public boolean supportsSleepApnea() {
        if (supportsExpandCapability())
            return supportsExpandCapability(107); // 178
        return false;
    }

    public boolean supportsContactsSync() {
        if (supportsExpandCapability())
            return supportsExpandCapability(271);
        return false;
    }

    public boolean supportsSendCountryCode() {
        if (supportsExpandCapability())
            return supportsExpandCapability(30);
        return false;
    }

    public boolean supportsSendSiteId() {
        if (supportsExpandCapability())
            return supportsExpandCapability(170);
        return false;
    }

    public boolean supportsArrhythmiaOther() {
        if (supportsExpandCapability())
            return supportsExpandCapability(113);
        return false;
    }

    public boolean supportsArrhythmiaCE() {
        if (supportsExpandCapability())
            return supportsExpandCapability(255);
        return false;
    }

    public boolean supportsArrhythmiaCN() {
        if (supportsExpandCapability())
            return supportsExpandCapability(168);
        return false;
    }

    public boolean supportsArrhythmia() {
        return supportsArrhythmiaCE() || supportsArrhythmiaCN() || supportsArrhythmiaOther();
    }

    public boolean supportsECG() {
        if (supportsExpandCapability())
            return supportsExpandCapability(106); // 226
        return false;
    }

    public boolean supportsECGOpen() {
        if (supportsExpandCapability())
            return supportsExpandCapability(27);
        return false;
    }

    public boolean supportsECGNotification() {
        if (supportsExpandCapability())
            return supportsExpandCapability(138);
        return false;
    }

    public boolean supportsAltitude() {
        if (supportsExpandCapability())
            return supportsExpandCapability(72);
        return false;
    }

    public boolean supportsArterialStiffnessDetection() {
        if (supportsExpandCapability())
            return supportsExpandCapability(75);
        return false;
    }

    public boolean supportsHRV() {
        if (supportsExpandCapability())
            return supportsExpandCapability(235);
        return false;
    }

    public boolean supportsPromptPushMessage() {
//              do not ask for capabilities under specific condition
//                  if (deviceType == 10 && deviceVersion == 73617766697368 && deviceSoftVersion == 372E312E31) -> leo device
//                  if V1V0Device
//                  if (serviceId = 0x01 && commandId = 0x03) && productType == 3
        return (((notificationCapabilities >> 1) & 1) == 0);
    }

    public boolean supportsOutgoingCall() {
        return (((notificationCapabilities >> 2) & 1) == 0);
    }

    public boolean supportsYellowPages() {
        return supportsNotificationConstraint(NotificationConstraintsType.yellowPagesSupport);
    }

    public boolean supportsContentSign() {
        return supportsNotificationConstraint(NotificationConstraintsType.contentSignSupport);
    }

    public boolean supportsIncomingNumber() {
        return supportsNotificationConstraint(NotificationConstraintsType.incomingNumberSupport);
    }

    public int getContentFormat() {
        return getNotificationConstraint(NotificationConstraintsType.contentFormat);
    }

    public int getYellowPagesFormat() {
        return getNotificationConstraint(NotificationConstraintsType.yellowPagesFormat);
    }

    public int getContentSignFormat() {
        return getNotificationConstraint(NotificationConstraintsType.contentSignFormat);
    }

    public int getIncomingNumberFormat() {
        return getNotificationConstraint(NotificationConstraintsType.incomingNumberFormat);
    }

    public int getContentLength() {
        return getNotificationConstraint(NotificationConstraintsType.contentLength);
    }

    public int getYellowPagesLength() {
        return getNotificationConstraint(NotificationConstraintsType.yellowPagesLength);
    }

    public int getContentSignLength() {
        return getNotificationConstraint(NotificationConstraintsType.contentSignLength);
    }

    public int getIncomingNumberLength() {
        return getNotificationConstraint(NotificationConstraintsType.incomingNumberLength);
    }

    public int getAlarmSlotCount(GBDevice gbDevice) {
        int alarmCount = 0;
        if (supportsEventAlarm())
            alarmCount += 5; // Always five event alarms
        if (supportsSmartAlarm(gbDevice))
            alarmCount += 1; // Always a single smart alarm
        return alarmCount;
    }

    public int getContactsSlotCount(GBDevice device) {
        if (supportsContactsSync()) {
            // TODO: Currently I don't know how to obtain contacts limit in runtime, and I don't know is the limit exists,
            // set limit to 20 because more items not comfortable to use in the current GB's UI. Can be increased in the future.
            return 20;
        }
        return supportsContacts() ? maxContactsCount : 0;
    }

    public int getCannedRepliesSlotCount(GBDevice device) {
        // TODO: find proper count
        return supportsCannedReplies() ? 10 : 0;
    }

    public short getWidth() {
        return (short) watchfaceDeviceParams.width;
    }

    public short getHeight() {
        return (short) watchfaceDeviceParams.height;
    }

    public void setWatchfaceDeviceParams(Watchface.WatchfaceDeviceParams watchfaceDeviceParams) {
        this.watchfaceDeviceParams = watchfaceDeviceParams;
    }

    public void setAppDeviceParams(App.AppDeviceParams appDeviceParams) {
        this.appDeviceParams = appDeviceParams;
    }

    public App.AppDeviceParams getAppDeviceParams() {
        return appDeviceParams;
    }

    public void setExtendedMusicInfoParams(HuaweiMusicUtils.MusicCapabilities musicDeviceParams) {
        LOG.info(musicDeviceParams.toString());
        this.musicExtendedDeviceParams = musicDeviceParams;
    }

    public HuaweiMusicUtils.MusicCapabilities getExtendedMusicInfoParams() {
        return musicExtendedDeviceParams;
    }

    public void setMusicInfoParams(HuaweiMusicUtils.MusicCapabilities musicDeviceParams) {
        LOG.info(musicDeviceParams.toString());
        this.musicDeviceParams = musicDeviceParams;
    }

    public HuaweiMusicUtils.MusicCapabilities getMusicInfoParams() {
        return musicDeviceParams;
    }


    public Class<? extends Activity> getAppManagerActivity() {
        return AppManagerActivity.class;
    }

    public boolean getSupportsAppListFetching() {
        return true;
    }

    public boolean getSupportsAppsManagement() {
        return true;
    }

    public boolean getSupportsInstalledAppManagement() {
        return this.supportsAppParams(); // NOTE: this check can be incorrect. But looks like it works
    }

    public boolean getSupportsCachedAppManagement() {
        return false;
    }

    public boolean getSupportsFlashing() {
        return true;
    }

    public boolean getSupportsTruSleepNewSync() {
        return supportsTruSleepNewSync;
    }

    public void setSupportsTruSleepNewSync(boolean supportsTruSleepNewSync) {
        this.supportsTruSleepNewSync = supportsTruSleepNewSync;
    }

    public boolean getSupportsRriNewSync() {
        return supportsRriNewSync;
    }

    public void setSupportsRriNewSync(boolean supportsRriNewSync) {
        this.supportsRriNewSync = supportsRriNewSync;
    }

    public boolean isSupportsGpsNewSync() {
        return supportsGpsNewSync;
    }

    public void setSupportsGpsNewSync(boolean supportsGpsNewSync) {
        this.supportsGpsNewSync = supportsGpsNewSync;
    }

    public boolean isSupportsWorkoutNewSteps() {
        return supportsWorkoutNewSteps;
    }

    public void setSupportsWorkoutNewSteps(boolean supportsWorkoutNewSteps) {
        this.supportsWorkoutNewSteps = supportsWorkoutNewSteps;
    }

    public String getOtaSoftwareVersion() {
        return otaSoftwareVersion;
    }

    public void setOtaSoftwareVersion(String otaSoftwareVersion) {
        this.otaSoftwareVersion = otaSoftwareVersion;
    }

    public int getOtaSignatureLength() {
        return otaSignatureLength;
    }

    public void setOtaSignatureLength(int otaSignatureLength) {
        this.otaSignatureLength = otaSignatureLength;
    }

    public boolean getSupportsNewTrueSleep() {
        return supportsTruSleep() && supportsDictSleepSync();
    }

    public HeartRateZonesSpec getHeartRateZonesSpec(@NonNull GBDevice device) {
        return new HuaweiHeartRateZonesSpec(device, this);
    }

    public String getCountryCode(GBDevice gbDevice) {
        return getDeviceSpecificSharedPreferences(gbDevice).getString("pref_huawei_country_code", "");
    }

    public boolean getSendCountryCodeEnabled(GBDevice gbDevice) {
        return getDeviceSpecificSharedPreferences(gbDevice).getBoolean("pref_huawei_country_code_enable", false) && !TextUtils.isEmpty(getCountryCode(gbDevice));
    }

    public boolean isShowForceCountrySpecificFeatures(GBDevice gbDevice) {
        return getDeviceSpecificSharedPreferences(gbDevice).getBoolean("pref_huawei_force_features_settings_switch", false) || getSendCountryCodeEnabled(gbDevice);
    }

    public boolean supportsNavigation() {
        return navigationAvailable;
    }

    public void setNavigationAvailability(boolean state) {
        navigationAvailable = state;
    }

}
