package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

public class G1Communications {
    private static final Logger LOG = LoggerFactory.getLogger(G1Communications.class);

    public abstract static class CommandHandler {
        private final boolean expectResponse;
        private final Function<byte[], Boolean> callback;
        protected byte sequence;
        private byte[] responsePayload;
        private int retryCount;

        public CommandHandler(boolean expectResponse, Function<byte[], Boolean> callback) {
            this.expectResponse = expectResponse;
            this.callback = callback;
            this.responsePayload = null;
            this.retryCount = 0;
        }

        public boolean needsGlobalSequence() { return false; }
        public void setGlobalSequence(byte sequence) {
            this.sequence = sequence;
        }
        public int getTimeout() {
            return G1Constants.DEFAULT_COMMAND_TIMEOUT_MS;
        }
        public int getMaxRetryCount() {
            return G1Constants.DEFAULT_RETRY_COUNT;
        }
        private synchronized boolean continueWaiting() {
            return !hasResponsePayload() && hasRetryRemaining();
        }

        public synchronized void notifyAttempt() {
            retryCount++;
            notify();
        }

        public synchronized void setResponsePayload(byte[] payload) {
            this.responsePayload = payload;
            notify();
        }

        public synchronized boolean hasRetryRemaining() {
            return retryCount < getMaxRetryCount();
        }
        public synchronized boolean hasResponsePayload() {
            return responsePayload != null;
        }

        public boolean waitForResponsePayload() {
            // Go to sleep until the either a response is gotten or there is a timeout.
            while (continueWaiting()) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ignored) {}
                }
            }

            // If the reties were exhausted return false to indicate that there was no response from
            // the glasses.
            return hasRetryRemaining();
        }

        public byte[] getResponsePayload(){
            if (responsePayload == null) {
                throw new RuntimeException("Null payload. Did you call waitForPayload()?");
            }
            return responsePayload;
        }

        public Function<byte[], Boolean> getCallback() {
            return callback;
        }

        public boolean expectResponse() {
            return expectResponse;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public abstract byte[] serialize();
        public abstract boolean responseMatches(byte[] payload);
        public abstract String getName();
    }

    public static class CommandSendInit extends CommandHandler {
        public CommandSendInit() {
            super(true, null);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.INIT.id, (byte)0xFB };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload[0] == G1Constants.CommandId.INIT.id;
        }

        @Override
        public String getName() {
            return "send_init";
        }
    }

    public static class CommandGetFirmwareInfo extends CommandHandler {
        public CommandGetFirmwareInfo(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.FW_INFO_REQUEST.id, 0x74 };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            if (payload.length < 10) {
                return false;
            }
            return payload[0] == G1Constants.CommandId.FW_INFO_RESPONSE.id;
        }

        @Override
        public String getName() {
            return "get_firmware_info";
        }
    }

     public static class CommandGetBatteryInfo extends CommandHandler {
        public CommandGetBatteryInfo(Function<byte[], Boolean> callback) {
            super(true, callback);
        }
        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.BATTERY_LEVEL.id, 0x01 };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            if (payload.length < 1) {
                return false;
            }
            return payload[0] == G1Constants.CommandId.BATTERY_LEVEL.id;
        }

        @Override
        public String getName() {
            return "get_battery_info";
        }
    }

    public static class CommandSendHeartBeat extends CommandHandler {
        public CommandSendHeartBeat(byte sequence) {
            super(false, null);
            setGlobalSequence(sequence);
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.HEARTBEAT.id,
                0x00, // length is a short
                0x06, // length
                sequence, // Sequence is included twice for some reason, also verified by the FW.
                0x04, // Magic value that the FW looks for.
                sequence
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return false;
        }

        @Override
        public String getName() {
            return "send_heart_beat";
        }
    }

    public static class CommandSetTimeAndWeather extends CommandHandler {
        long timeMilliseconds;
        boolean use12HourFormat;
        byte tempInCelsius;
        byte weatherIcon;
        boolean useFahrenheit;

        public CommandSetTimeAndWeather(long timeMilliseconds, boolean use12HourFormat, WeatherSpec weatherInfo, boolean useFahrenheit) {
            super(true, null);
            this.timeMilliseconds = timeMilliseconds;
            this.use12HourFormat = use12HourFormat;
            if (weatherInfo != null) {
                // TODO need to convert the weather spec enums to the ER enums.
                this.weatherIcon = 0x01;
                this.tempInCelsius = (byte) weatherInfo.currentTemp;
            } else {
                this.weatherIcon = 0x00;
                this.tempInCelsius = 0x00;
            }
            this.useFahrenheit = useFahrenheit;
        }
        public CommandSetTimeAndWeather(long timeMilliseconds, boolean use12HourFormat, boolean useFahrenheit) {
            this(timeMilliseconds, use12HourFormat, null, useFahrenheit);
        }

        @Override
        public boolean needsGlobalSequence() { return true; }

        @Override
        public byte[] serialize() {
            byte[] packet = new byte[] {
                G1Constants.CommandId.DASHBOARD_CONFIG.id,
                G1Constants.DashboardConfigSubCommand.SET_TIME_AND_WEATHER.id,
                0x00,
                sequence,
                // Magic number?
                0x01,
                // Time 32bit place holders
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                // Time 64bit place holders
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                (byte) 0xFF,
                // Weather info
                this.weatherIcon,
                tempInCelsius,
                // F/C
                (byte)(useFahrenheit ? 0x01 : 0x00),
                // 24H/12H
                (byte)(use12HourFormat ? 0x01 : 0x00)
            };
            BLETypeConversions.writeUint32(packet, 5, (int)(timeMilliseconds / 1000));
            BLETypeConversions.writeUint64(packet, 9, timeMilliseconds);

            return packet;
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            if (payload.length < 4) {
                return false;
            }

            // Command should match and the sequence should match.
            return payload[0] == G1Constants.CommandId.DASHBOARD_CONFIG.id &&
                   payload[1] == G1Constants.DashboardConfigSubCommand.SET_TIME_AND_WEATHER.id &&
                   payload[3] == sequence;
        }

        @Override
        public String getName() {
            return "set_time_and_weather";
        }
    }

    public static class CommandGetSilentModeSettings extends CommandHandler {
        public CommandGetSilentModeSettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_SILENT_MODE_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.GET_SILENT_MODE_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_silent_status";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == G1Constants.SilentStatus.ENABLE;
        }
    }

    public static class CommandSetSilentModeSettings extends CommandHandler {
        private final boolean enable;
        public CommandSetSilentModeSettings(boolean enable) {
            super(true, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SET_SILENT_MODE_SETTINGS.id,
                (byte)(enable ? G1Constants.SilentStatus.ENABLE : G1Constants.SilentStatus.DISABLE),
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length > 1 && payload[0] == G1Constants.CommandId.SET_SILENT_MODE_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "set_silent_mode_settings_" + (enable ? "enabled" : "disabled");
        }
    }

    public static class CommandGetDisplaySettings extends CommandHandler {
        public CommandGetDisplaySettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_DISPLAY_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.GET_DISPLAY_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_display_settings";
        }

        public static byte getHeight(byte[] payload) {
            return payload[2];
        }

        public static byte getDepth(byte[] payload) {
            return payload[3];
        }
    }

    public static class CommandSetDisplaySettings extends CommandHandler {
        private final boolean preview;
        private final byte height;
        private final byte depth;
        public CommandSetDisplaySettings(boolean preview, byte height, byte depth) {
            super(true, null);
            this.preview = preview;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public boolean needsGlobalSequence() { return true; }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SET_DISPLAY_SETTINGS.id,
                0x08, // Subcommand?
                0x00,
                sequence,
                0x02, // Seems to be a magic number?
                preview ? 0x01 : (byte)0x00,
                height,
                depth
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 6 &&
                   payload[0] == G1Constants.CommandId.SET_DISPLAY_SETTINGS.id &&
                   payload[1] == 0x06 && // Magic Number
                   payload[3] == sequence;
        }

        @Override
        public String getName() {
            return "set_display_settings_" + height + "_" + depth;
        }
    }

    public static class CommandGetHeadGestureSettings extends CommandHandler {
        public CommandGetHeadGestureSettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_HEAD_GESTURE_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.GET_HEAD_GESTURE_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_head_gesture_settings";
        }

        public static byte getActivationAngle(byte[] payload) {
            return payload[2];
        }
    }

    public static class CommandSetHeadGestureSettings extends CommandHandler {
        private final byte angle;
        // Allowed Angles are 0-60.
        public CommandSetHeadGestureSettings(byte angle) {
            super(true, null);
            this.angle = angle;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SET_HEAD_GESTURE_SETTINGS.id,
                angle,
                // Magic number, other project called it the "level setting".
                // Maybe try sending 0x00 and see what happens?
                0x01
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.SET_HEAD_GESTURE_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "set_head_gesture_settings_" + angle;
        }
    }

    public static class CommandGetBrightnessSettings extends CommandHandler {
        public CommandGetBrightnessSettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_BRIGHTNESS_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 3 && payload[0] == G1Constants.CommandId.GET_BRIGHTNESS_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_brightness_settings";
        }

        public static byte getBrightnessLevel(byte[] payload) {
            return payload[2];
        }

        public static boolean isAutoBrightnessEnabled(byte[] payload) {
            return payload[3] == 0x01;
        }
    }

    public static class CommandSetBrightnessSettings extends CommandHandler {
        private final boolean enableAutoBrightness;
        private final byte brightnessLevel;
        public CommandSetBrightnessSettings(boolean enableAutoBrightness, byte brightnessLevel) {
            super(true, null);
            this.enableAutoBrightness = enableAutoBrightness;
            this.brightnessLevel = brightnessLevel;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SET_BRIGHTNESS_SETTINGS.id,
                brightnessLevel,
                enableAutoBrightness ? 0x01 : (byte)0x00
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length > 1 && payload[0] == G1Constants.CommandId.SET_BRIGHTNESS_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "set_brightness_settings_" + enableAutoBrightness + "_" + brightnessLevel;
        }
    }

    public static class CommandGetWearDetectionSettings extends CommandHandler {
        public CommandGetWearDetectionSettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_WEAR_DETECTION_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.GET_WEAR_DETECTION_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_wear_detection_settings";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == 0x01;
        }
    }

    public static class CommandSetWearDetectionSettings extends CommandHandler {
        private final boolean enable;
        public CommandSetWearDetectionSettings(boolean enable) {
            super(true, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SET_WEAR_DETECTION_SETTINGS.id,
                enable ? 0x01 : (byte)0x00
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.SET_WEAR_DETECTION_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "set_wear_detection_settings_" + (enable ? "enabled" : "disabled");
        }
    }
}
