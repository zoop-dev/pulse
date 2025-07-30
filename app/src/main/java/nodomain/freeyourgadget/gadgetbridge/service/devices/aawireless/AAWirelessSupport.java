/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.aawireless.AAWirelessProto;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.AbstractBTBRDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class AAWirelessSupport extends AbstractBTBRDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(AAWirelessSupport.class);

    private static final UUID UUID_SERVICE_AAWIRELESS = UUID.fromString("b7b2ee43-c32a-41a6-9ee4-336c1365a1c4");

    private static final int MAX_MTU = 2048;

    private static final String MAC_ADDR_PATTERN = "^([0-9A-F]{2}:){5}[0-9A-F]{2}$";

    private static final short CMD_STATUS_REQUEST = 0x0001;
    private static final short CMD_STATUS_RESPONSE = 0x0002;
    private static final short CMD_SETTINGS_SET = 0x0003;
    private static final short CMD_SETTINGS_ACK = 0x0004;
    private static final short CMD_PHONE_DELETE_REQUEST = 0x000b;
    private static final short CMD_PHONE_DELETE_ACK = 0x000c;
    private static final short CMD_PHONE_POSITION_REQUEST = 0x000d;
    private static final short CMD_PHONE_POSITION_ACK = 0x000e;
    private static final short CMD_FACTORY_RESET_REQUEST = 0x000f;
    private static final short CMD_FACTORY_RESET_ACK = 0x0010;
    private static final short CMD_PHONE_SWITCH_REQUEST = 0x0011;
    private static final short CMD_PHONE_SWITCH_ACK = 0x0012;

    private final ByteBuffer packetBuffer = ByteBuffer.allocate(MAX_MTU).order(ByteOrder.BIG_ENDIAN);

    private final BroadcastReceiver commandReceiver = new AAWirelessCommandReceiver();

    public AAWirelessSupport() {
        super(LOG, MAX_MTU);
        addSupportedService(UUID_SERVICE_AAWIRELESS);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public AAWirelessPrefs getDevicePrefs() {
        return new AAWirelessPrefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()), gbDevice);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        packetBuffer.clear();

        final IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(AAWirelessPrefs.ACTION_PHONE_SWITCH);
        commandFilter.addAction(AAWirelessPrefs.ACTION_PHONE_SORT);
        commandFilter.addAction(AAWirelessPrefs.ACTION_PHONE_DELETE);
        ContextCompat.registerReceiver(getContext(), commandReceiver, commandFilter, ContextCompat.RECEIVER_NOT_EXPORTED);

        sendCommand(builder, CMD_STATUS_REQUEST, new byte[0]);
        return builder;
    }

    @Override
    public void dispose() {
        synchronized (ConnectionMonitor) {
            try {
                getContext().unregisterReceiver(commandReceiver);
            } catch (final Exception e) {
                LOG.warn("Failed to unregister receiver", e);
            }

            super.dispose();
        }
    }

    @Override
    public void onSocketRead(final byte[] data) {
        packetBuffer.put(data);
        packetBuffer.flip();

        while (packetBuffer.hasRemaining()) {
            packetBuffer.mark();

            if (packetBuffer.remaining() < 6) {
                // not enough bytes for min packet
                packetBuffer.reset();
                break;
            }

            final int payloadLength = packetBuffer.getInt();
            final short command = packetBuffer.getShort();

            if (packetBuffer.remaining() < payloadLength) {
                // not enough bytes for payload
                packetBuffer.reset();
                break;
            }

            final byte[] payload = new byte[payloadLength];
            packetBuffer.get(payload);

            try {
                handleCommand(command, payload);
            } catch (final Exception e) {
                LOG.error("Failed to handle command", e);
            }
        }

        packetBuffer.compact();
    }

    private void sendCommand(final TransactionBuilder builder, final short command, final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.allocate(payload.length + 6).order(ByteOrder.BIG_ENDIAN);
        buf.putInt(payload.length);
        buf.putShort(command);
        buf.put(payload);

        builder.write(buf.array());
    }

    private void sendCommand(final String taskName, final short command, final byte[] payload) {
        final TransactionBuilder builder = createTransactionBuilder(taskName);
        sendCommand(builder, command, payload);
        builder.queue();
    }

    private void handleCommand(final short command, final byte[] payload) throws Exception {
        switch (command) {
            case CMD_STATUS_RESPONSE -> {
                final AAWirelessProto.Status status = AAWirelessProto.Status.parseFrom(payload);
                LOG.info("Got status = {}", status);
                handleStatus(status);
            }
            case CMD_SETTINGS_ACK -> {
                final AAWirelessProto.Ack ack = AAWirelessProto.Ack.parseFrom(payload);
                LOG.info("Got settings ack, status={}, message={}", ack.getStatus(), ack.getMessage());
                if (ack.getStatus() != 0) {
                    GB.toast("AAW " + ack.getStatus() + ": " + ack.getMessage(), Toast.LENGTH_LONG, GB.WARN);
                }
                sendCommand("request status", CMD_STATUS_REQUEST, new byte[0]);
            }
            case CMD_PHONE_DELETE_ACK -> {
                final AAWirelessProto.Ack ack = AAWirelessProto.Ack.parseFrom(payload);
                LOG.info("Got Phone delete ack, status={}, message={}", ack.getStatus(), ack.getMessage());
                if (ack.getStatus() != 0) {
                    GB.toast("AAW " + ack.getStatus() + ": " + ack.getMessage(), Toast.LENGTH_LONG, GB.WARN);
                }
                sendCommand("request status", CMD_STATUS_REQUEST, new byte[0]);
            }
            case CMD_PHONE_POSITION_ACK -> {
                final AAWirelessProto.Ack ack = AAWirelessProto.Ack.parseFrom(payload);
                LOG.info("Got phone position ack, status={}, message={}", ack.getStatus(), ack.getMessage());
                if (ack.getStatus() != 0) {
                    GB.toast("AAW " + ack.getStatus() + ": " + ack.getMessage(), Toast.LENGTH_LONG, GB.WARN);
                }
                sendCommand("request status", CMD_STATUS_REQUEST, new byte[0]);
            }
            case CMD_FACTORY_RESET_ACK -> {
                final AAWirelessProto.Ack ack = AAWirelessProto.Ack.parseFrom(payload);
                LOG.info("Got factory reset ack, status={}, message={}", ack.getStatus(), ack.getMessage());
                if (ack.getStatus() != 0) {
                    GB.toast("AAW " + ack.getStatus() + ": " + ack.getMessage(), Toast.LENGTH_LONG, GB.WARN);
                }
            }
            case CMD_PHONE_SWITCH_ACK -> {
                final AAWirelessProto.Ack ack = AAWirelessProto.Ack.parseFrom(payload);
                LOG.info("Got phone switch ack, status={}, message={}", ack.getStatus(), ack.getMessage());
                if (ack.getStatus() != 0) {
                    GB.toast("AAW " + ack.getStatus() + ": " + ack.getMessage(), Toast.LENGTH_LONG, GB.WARN);
                }
            }
            default -> LOG.warn(
                    "Got unknown command {}, payload={}",
                    String.format("0x%04x", command),
                    GB.hexdump(payload)
            );
        }
    }

    private void handleStatus(final AAWirelessProto.Status status) {
        // FW / HW
        final GBDeviceEventVersionInfo versionInfoEvent = new GBDeviceEventVersionInfo();
        versionInfoEvent.hwVersion = status.getHardwareModel();
        versionInfoEvent.fwVersion = status.getFirmwareVersion();
        evaluateGBDeviceEvent(versionInfoEvent);

        // Mark as initialized
        evaluateGBDeviceEvent(new GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED));

        // Settings
        final AAWirelessProto.Settings settings = status.getSettings();
        final GBDeviceEventUpdatePreferences preferencesEvent = new GBDeviceEventUpdatePreferences();
        preferencesEvent.withPreference(DeviceSettingsPreferenceConst.PREF_COUNTRY, settings.getCountry());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_AUTO_STANDBY_ENABLED, settings.getAutoStandbyEnabled());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_AUTO_STANDBY_DEVICE, settings.getAutoStandbyDevice());
        preferencesEvent.withPreference(
                DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY,
                settings.getWifiFrequency() == AAWirelessProto.WiFiFrequency.FREQ_2_4_GHZ ? "2.4" : "5"
        );
        switch (settings.getWifiFrequency()) {
            case FREQ_5_GHZ -> preferencesEvent.withPreference(
                    DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5,
                    String.valueOf(settings.getWifiChannel())
            );
            case FREQ_2_4_GHZ -> preferencesEvent.withPreference(
                    DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4,
                    String.valueOf(settings.getWifiChannel())
            );
        }
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_HAS_BUTTON, settings.hasButtonsConfig());
        if (settings.hasButtonsConfig()) {
            preferencesEvent.withPreference(AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK, buttonModeToPref(settings.getButtonsConfig().getSingle()));
            preferencesEvent.withPreference(AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK, buttonModeToPref(settings.getButtonsConfig().getDouble()));
        }
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_DONGLE_MODE, settings.getDongleMode());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_PASSTHROUGH, settings.getPassthrough());
        final String audioStutterFixPrefValue;
        if (settings.getAudioStutterFix() == 50) {
            audioStutterFixPrefValue = AAWirelessPrefs.AUDIO_STUTTER_FIX_LOW;
        } else if (settings.getAudioStutterFix() == 100) {
            audioStutterFixPrefValue = AAWirelessPrefs.AUDIO_STUTTER_FIX_HIGH;
        } else if (settings.getAudioStutterFix() == 4096) {
            audioStutterFixPrefValue = AAWirelessPrefs.AUDIO_STUTTER_FIX_UNLIMITED;
        } else {
            audioStutterFixPrefValue = AAWirelessPrefs.AUDIO_STUTTER_FIX_OFF;
        }
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_AUDIO_STUTTER_FIX, audioStutterFixPrefValue);
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_DPI, String.valueOf(settings.getDpi()));
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_DISABLE_MEDIA_SINK, settings.getDisableMediaSink());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_DISABLE_TTS_SINK, settings.getDisableTtsSink());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_REMOVE_TAP_RESTRICTION, settings.getRemoveTapRestriction());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_VAG_CRASH_FIX, settings.getVagCrashFix());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_START_FIX, settings.getStartFix());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_DEVELOPER_MODE, settings.getDeveloperMode());
        preferencesEvent.withPreference(AAWirelessPrefs.PREF_AUTO_VIDEO_FOCUS, settings.getAutoVideoFocus());

        preferencesEvent.withPreference(AAWirelessPrefs.PREF_PREFER_LAST_CONNECTED, settings.getPreferLastConnectedPhone());

        preferencesEvent.withPreference(AAWirelessPrefs.PREF_KNOWN_PHONES_COUNT, status.getPairedPhoneCount());
        for (int i = 0; i < status.getPairedPhoneCount(); i++) {
            final AAWirelessProto.Phone phone = status.getPairedPhone(i);
            preferencesEvent.withPreference(AAWirelessPrefs.PREF_KNOWN_PHONES_MAC + i, phone.getMacAddress());
            preferencesEvent.withPreference(AAWirelessPrefs.PREF_KNOWN_PHONES_NAME + i, phone.getName());
        }

        evaluateGBDeviceEvent(preferencesEvent);
    }

    private String buttonModeToPref(final AAWirelessProto.ButtonMode buttonMode) {
        if (buttonMode.hasNextPhone()) {
            return AAWirelessPrefs.BUTTON_MODE_NEXT_PHONE;
        } else if (buttonMode.hasSelectPhone()) {
            return buttonMode.getSelectPhone().getMacAddress();
        } else if (buttonMode.hasStandbyOnOff()) {
            return AAWirelessPrefs.BUTTON_MODE_STANDBY_ON_OFF;
        } else {
            return AAWirelessPrefs.BUTTON_MODE_NONE;
        }
    }

    private AAWirelessProto.ButtonMode prefToButtonMode(final String prefValue) {
        final AAWirelessProto.ButtonMode.Builder builder = AAWirelessProto.ButtonMode.newBuilder();
        switch (prefValue) {
            case AAWirelessPrefs.BUTTON_MODE_NEXT_PHONE:
                builder.setNextPhone("");
                break;
            case AAWirelessPrefs.BUTTON_MODE_STANDBY_ON_OFF:
                builder.setStandbyOnOff("");
                break;
            case AAWirelessPrefs.BUTTON_MODE_NONE:
                break;
            default:
                if (prefValue.matches(MAC_ADDR_PATTERN)) {
                    builder.setSelectPhone(AAWirelessProto.Phone.newBuilder().setMacAddress(prefValue));
                } else {
                    LOG.warn("Unexpected button mode preference value {}", prefValue);
                }
        }

        return builder.build();
    }

    @Override
    public void onSendConfiguration(final String config) {
        switch (config) {
            case DeviceSettingsPreferenceConst.PREF_COUNTRY:
            case AAWirelessPrefs.PREF_AUTO_STANDBY_ENABLED:
            case AAWirelessPrefs.PREF_AUTO_STANDBY_DEVICE:
            case DeviceSettingsPreferenceConst.PREF_WIFI_FREQUENCY:
            case DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_2_4:
            case DeviceSettingsPreferenceConst.PREF_WIFI_CHANNEL_5:
            case AAWirelessPrefs.PREF_BUTTON_MODE_SINGLE_CLICK:
            case AAWirelessPrefs.PREF_BUTTON_MODE_DOUBLE_CLICK:
            case AAWirelessPrefs.PREF_DONGLE_MODE:
            case AAWirelessPrefs.PREF_PASSTHROUGH:
            case AAWirelessPrefs.PREF_AUDIO_STUTTER_FIX:
            case AAWirelessPrefs.PREF_DPI:
            case AAWirelessPrefs.PREF_DISABLE_MEDIA_SINK:
            case AAWirelessPrefs.PREF_DISABLE_TTS_SINK:
            case AAWirelessPrefs.PREF_REMOVE_TAP_RESTRICTION:
            case AAWirelessPrefs.PREF_VAG_CRASH_FIX:
            case AAWirelessPrefs.PREF_START_FIX:
            case AAWirelessPrefs.PREF_DEVELOPER_MODE:
            case AAWirelessPrefs.PREF_AUTO_VIDEO_FOCUS:
                sendSettings();
                return;
        }

        super.onSendConfiguration(config);
    }

    private void sendSettings() {
        final AAWirelessPrefs prefs = getDevicePrefs();

        final AAWirelessProto.WiFiFrequency wiFiFrequency = switch (prefs.getWiFiFrequency()) {
            case "5" -> AAWirelessProto.WiFiFrequency.FREQ_5_GHZ;
            case "2.4" -> AAWirelessProto.WiFiFrequency.FREQ_2_4_GHZ;
            default -> throw new IllegalArgumentException(
                    "Unknown wifi frequency " + prefs.getWiFiFrequency()
            );
        };

        final long audioStutterFix = switch (prefs.getAudioStutterFix()) {
            case "unlimited" -> 4096;
            case "high" -> 100;
            case "low" -> 50;
            case "off" -> -1; // 18446744073709551615 in uint64
            default -> throw new IllegalArgumentException(
                    "Unknown audio stutter fix " + prefs.getAudioStutterFix()
            );
        };

        final AAWirelessProto.ButtonsConfig buttonsConfig = AAWirelessProto.ButtonsConfig.newBuilder()
                .setSingle(prefToButtonMode(prefs.getButtonModeSingleClick()))
                .setDouble(prefToButtonMode(prefs.getButtonModeDoubleClick()))
                .build();

        final AAWirelessProto.Settings.Builder builder = AAWirelessProto.Settings.newBuilder()
                .setPassthrough(prefs.enablePassthrough())
                .setVagCrashFix(prefs.enableVagCrashFix())
                .setDpi(prefs.getDpi())
                .setRemoveTapRestriction(prefs.removeTapRestriction())
                .setDeveloperMode(prefs.enableDeveloperMode())
                .setDisableMediaSink(prefs.disableMediaSink())
                .setWifiFrequency(wiFiFrequency)
                .setAutoVideoFocus(prefs.enableAutoVideoFocus())
                .setWifiChannel(prefs.getWiFiChannel())
                .setCountry(prefs.getCountry())
                .setPreferLastConnectedPhone(prefs.preferLastConnected())
                .setDisableTtsSink(prefs.disableTtsSink())
                .setDongleMode(prefs.enableDongleMode())
                .setAutoStandbyDevice(prefs.getAutoStandbyDevice())
                .setStartFix(prefs.enableStartFix())
                .setAudioStutterFix(audioStutterFix)
                .setButtonsConfig(buttonsConfig)
                .setAutoStandbyEnabled(prefs.getAutoStandbyEnabled());

        sendCommand("send settings", CMD_SETTINGS_SET, builder.build().toByteArray());
    }

    @Override
    public void onReset(final int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            sendCommand("factory reset", CMD_FACTORY_RESET_REQUEST, new byte[0]);
        }
    }

    private class AAWirelessCommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }

            final String mac = intent.getStringExtra(AAWirelessPrefs.EXTRA_PHONE_MAC);

            switch (action) {
                case AAWirelessPrefs.ACTION_PHONE_SWITCH: {
                    LOG.debug("Switch phone to {}", mac);
                    final AAWirelessProto.Phone payload = AAWirelessProto.Phone.newBuilder().setMacAddress(mac).build();
                    sendCommand("switch phone", CMD_PHONE_SWITCH_REQUEST, payload.toByteArray());
                    return;
                }
                case AAWirelessPrefs.ACTION_PHONE_SORT: {
                    final int newPosition = intent.getIntExtra(AAWirelessPrefs.EXTRA_PHONE_NEW_POSITION, 0);
                    LOG.debug("Sort phone {} to {}", mac, newPosition);
                    final AAWirelessProto.PhonePosition payload = AAWirelessProto.PhonePosition.newBuilder()
                            .setMacAddress(mac)
                            .setPosition(newPosition)
                            .build();
                    sendCommand("set phone position", CMD_PHONE_POSITION_REQUEST, payload.toByteArray());
                    return;
                }
                case AAWirelessPrefs.ACTION_PHONE_DELETE: {
                    final AAWirelessProto.Phone payload = AAWirelessProto.Phone.newBuilder().setMacAddress(mac).build();
                    LOG.debug("Delete phone {}", mac);
                    sendCommand("delete phone", CMD_PHONE_DELETE_REQUEST, payload.toByteArray());
                    return;
                }
            }

            LOG.warn("Unknown action {}", action);
        }
    }
}
