package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
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

        public byte[] getResponsePayload() {
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

    /**
     * Certain payloads are too large for one packet, so this class is a simple extension of the
     * CommandHandler that allows the subclass to send multiple packets.
     * This works by forcing the caller to pass in a callback to the "send" function they are using
     * and the ChunkCommandHandler will intercept calls to the Command callback to send the next
     * chunk in the packet. Once the response for the last chunk is sent, the passed in callback
     * will be sent.
     */
    public abstract static class ChunkedCommandHandler extends CommandHandler {
        private final Consumer<CommandHandler> sendCallback;
        private byte currentChunk;
        protected final byte[] payload;
        protected final byte chunkCount;

        public ChunkedCommandHandler(Consumer<CommandHandler> sendCallback, Function<byte[], Boolean> callback, byte[] payload) {
            super(true, callback);
            this.sendCallback = sendCallback;
            this.currentChunk = 0;
            this.payload = payload;
            int maxChunkSize = G1Constants.MAX_PACKET_SIZE_BYTES - getHeaderSize();
            this.chunkCount = (byte)((this.payload.length / maxChunkSize) + 1);
        }

        @Override
        final public Function<byte[], Boolean> getCallback() {
            if (currentChunk < chunkCount) {
                // Return the callback which sends the next chunk.
                return this::sendNextChunk;
            } else {
                // Now that all the chunks have been received, return the user callback.
                return super.getCallback();
            }
        }

        @Override
        final public byte[] serialize() {
            // Calculate the size, begin and end of the chunk.
            int maxPayloadSize = G1Constants.MAX_PACKET_SIZE_BYTES - getHeaderSize();
            int chunkBegin = this.currentChunk * maxPayloadSize;
            int chunkEnd = Math.min(this.payload.length,
                                    (this.currentChunk + 1) * maxPayloadSize);
            int payloadSize = chunkEnd - chunkBegin;

            // Create the packet with space for the header.
            byte[] packet = new byte[getHeaderSize() + payloadSize];

            // Let the subclass write the header.
            writeHeader(this.currentChunk, this.chunkCount, packet);

            // Copy the chunk of the payload into the packet.
            System.arraycopy(this.payload, chunkBegin, packet, getHeaderSize(), payloadSize);

            // Advance the chunk.
            currentChunk++;

            return packet;
        }

        @Override
        final public boolean responseMatches(byte[] payload) {
            return chunkMatches(currentChunk, payload);
        }

        private boolean sendNextChunk(byte[] payload) {
            sendCallback.accept(this);
            return true;
        }

        protected abstract boolean chunkMatches(byte currentChunk, byte[] payload);
        protected abstract void writeHeader(byte currentChunk, byte chunkCount, byte[] chunk);
        protected abstract int getHeaderSize();
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

    public static class CommandSendReset extends CommandHandler {
        public CommandSendReset() {
            super(false, null);
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SYSTEM.id,
                G1Constants.SystemSubCommand.RESET.id
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return false;
        }

        @Override
        public String getName() {
            return "send_reset";
        }
    }

    public static class CommandGetFirmwareInfo extends CommandHandler {
        public CommandGetFirmwareInfo(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.SYSTEM.id,
                G1Constants.SystemSubCommand.GET_FW_INFO.id
            };
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

         public static int getBatteryPercent(byte[] payload) {
            return payload[2];
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
                this.weatherIcon = G1Constants.fromOpenWeatherCondition(weatherInfo.getCurrentConditionCode());
                // Convert sunny to a moon if the current time stamp is between sunrise and sunset.
                // At midnight, the sunrise and sunset time move forward.
                boolean isNight = (weatherInfo.getTimestamp() >= weatherInfo.getSunSet() &&
                                   weatherInfo.getTimestamp() >= weatherInfo.getSunRise()) ||
                                  (weatherInfo.getTimestamp() < weatherInfo.getSunRise() &&
                                   weatherInfo.getTimestamp() < weatherInfo.getSunSet());
                if (this.weatherIcon == G1Constants.WeatherId.SUNNY && isNight) {
                    this.weatherIcon = G1Constants.WeatherId.NIGHT;
                }
                // Convert Kelvin -> Celsius.
                this.tempInCelsius = (byte) (weatherInfo.getCurrentTemp() - 273);
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
                0x15, // Length = 21 bytes
                0x00,
                sequence,
                // Subcommand
                G1Constants.DashboardConfig.SUB_COMMAND_SET_TIME_AND_WEATHER,
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
                useFahrenheit ? G1Constants.TemperatureUnit.FAHRENHEIT
                              : G1Constants.TemperatureUnit.CELSIUS,
                use12HourFormat ? G1Constants.TimeFormat.TWELVE_HOUR
                                : G1Constants.TimeFormat.TWENTY_FOUR_HOUR
            };
            BLETypeConversions.writeUint32(packet, 5, (int)(timeMilliseconds / 1000));
            BLETypeConversions.writeUint64(packet, 9, timeMilliseconds);

            return packet;
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            // Command should match and the sequence should match.
            return payload.length >= 5 &&
                   payload[0] == G1Constants.CommandId.DASHBOARD_CONFIG.id &&
                   payload[3] == sequence &&
                   payload[4] == G1Constants.DashboardConfig.SUB_COMMAND_SET_TIME_AND_WEATHER;
        }

        @Override
        public String getName() {
            return "set_time_and_weather";
        }
    }

    public static class CommandSetDashboardModeSettings extends CommandHandler {
        byte mode;
        byte secondaryPaneMode;
        public CommandSetDashboardModeSettings(byte mode, byte secondaryPaneMode) {
            super(true, null);
            this.mode = mode;
            this.secondaryPaneMode = secondaryPaneMode;
        }

        @Override
        public boolean needsGlobalSequence() { return true; }

        @Override
        public byte[] serialize() {
            return new byte[]{
                G1Constants.CommandId.DASHBOARD_CONFIG.id,
                0x07, // Length
                0x00, // pad
                sequence,
                G1Constants.DashboardConfig.SUB_COMMAND_SET_MODE,
                mode,
                secondaryPaneMode
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            // Command should match and the sequence should match.
            return payload.length >= 5 &&
                   payload[0] == G1Constants.CommandId.DASHBOARD_CONFIG.id &&
                   payload[3] == sequence &&
                   payload[4] == G1Constants.DashboardConfig.SUB_COMMAND_SET_MODE;
        }

        @Override
        public String getName() {
            return "set_dashboard_mode_settings";
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

        public static int getHeight(byte[] payload) {
            return payload[2];
        }

        public static int getDepth(byte[] payload) {
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
                0x08, // Length
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

        public static int getActivationAngle(byte[] payload) {
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

        public static int getBrightnessLevel(byte[] payload) {
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

    public static class CommandGetSerialNumber extends CommandHandler {
        public CommandGetSerialNumber(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_SERIAL_NUMBER.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 16 && payload[0] == G1Constants.CommandId.GET_SERIAL_NUMBER.id;
        }

        @Override
        public String getName() {
            return "get_serial_number";
        }

        public static int getFrameType(byte[] payload) {
            String serialNumber = getSerialNumber(payload);
            if (serialNumber.length() < 7) return -1;
            switch(serialNumber.substring(4, 7)) {
                case G1Constants.HardwareDescriptionKey.COLOR_GREY:
                    return R.string.even_realities_frame_color_grey;
                case G1Constants.HardwareDescriptionKey.COLOR_BROWN:
                    return R.string.even_realities_frame_color_brown;
                case G1Constants.HardwareDescriptionKey.COLOR_GREEN:
                    return R.string.even_realities_frame_color_green;
                default:
                    return -1;
            }
        }

        public static int getFrameColor(byte[] payload) {
            String serialNumber = getSerialNumber(payload);
            if (serialNumber.length() < 4) return -1;
            switch(serialNumber.substring(0, 4)) {
                case G1Constants.HardwareDescriptionKey.FRAME_ROUND:
                    return R.string.even_realities_frame_shape_G1A;
                case G1Constants.HardwareDescriptionKey.FRAME_SQUARE:
                    return R.string.even_realities_frame_shape_G1B;
                default:
                    return -1;
            }
        }

        public static String getSerialNumber(byte[] payload) {
            return new String(payload, 2, 14, StandardCharsets.US_ASCII);
        }
    }

    public static class CommandGetNotificationDisplaySettings extends CommandHandler {
        public CommandGetNotificationDisplaySettings(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.GET_NOTIFICATION_DISPLAY_SETTINGS.id };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.GET_NOTIFICATION_DISPLAY_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "get_notification_display_settings";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == 0x01;
        }
        public static int getTimeout(byte[] payload) {
            return payload[3];
        }
    }

    public static class CommandSetNotificationDisplaySettings extends CommandHandler {
        private final boolean enable;
        private final byte timeout;
        public CommandSetNotificationDisplaySettings(boolean enable, byte timeout) {
            super(true, null);
            this.enable = enable;
            this.timeout = timeout;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.SET_NOTIFICATION_DISPLAY_SETTINGS.id,
                    enable ? 0x01 : (byte)0x00,
                    timeout
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.SET_NOTIFICATION_DISPLAY_SETTINGS.id;
        }

        @Override
        public String getName() {
            return "set_notification_display_settings_" + (enable ? "enabled" : "disabled") + "_" + timeout;
        }
    }

    public static class CommandSetAppNotificationSettings extends ChunkedCommandHandler {
        public CommandSetAppNotificationSettings(Consumer<CommandHandler> sendCallback,
                                                 List<Pair<String, String>> appIdentifiers,
                                                 boolean enableCalendar,
                                                 boolean enableCalls,
                                                 boolean enableSMS) {
            super(sendCallback, null,
                  generatePayload(appIdentifiers, enableCalendar, enableCalls, enableSMS));
        }

        private static byte[] generatePayload(List<Pair<String, String>> appIdentifiers,
                                              boolean enableCalendar,
                                              boolean enableCalls,
                                              boolean enableSMS) {
            try {
                JSONObject appJson = new JSONObject();
                JSONArray appList = new JSONArray();
                for (Pair<String, String> appInfo : appIdentifiers) {
                    JSONObject app = new JSONObject();
                    app.put("id", appInfo.first);
                    app.put("name", appInfo.second);
                    appList.put(app);
                }
                appJson.put("list", appList);
                appJson.put("enable", true);

                JSONObject json = new JSONObject();
                json.put("calendar_enable", enableCalendar);
                json.put("call_enable", enableCalls);
                json.put("msg_enable", enableSMS);
                json.put("ios_mail_enable", false);
                json.put("app", appJson);

                // Need to allocate one larger in order to null terminate.
                 String jsonString = json.toString();
                byte[] bytes = new byte[jsonString.length() + 1];
                System.arraycopy(jsonString.getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, jsonString.length());
                bytes[jsonString.length()] = 0;
                return bytes;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected boolean chunkMatches(byte chunk, byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.SET_NOTIFICATION_APP_SETTINGS.id;
        }

        @Override
        protected void writeHeader(byte currentChunk, byte chunkCount, byte[] chunk) {
            chunk[0] = G1Constants.CommandId.SET_NOTIFICATION_APP_SETTINGS.id;
            chunk[1] = chunkCount;
            chunk[2] = currentChunk;
        }

        @Override
        protected int getHeaderSize() {
            return 3;
        }

        @Override
        public String getName() {
            return "set_notification_app_settings";
        }
    }

    public static class CommandSendNotification extends ChunkedCommandHandler {
        private final int messageId;

        public CommandSendNotification(Consumer<CommandHandler> sendCallback, NotificationSpec notificationSpec) {
            super(sendCallback, null, generatePayload(notificationSpec));
            this.messageId = notificationSpec.getId();
        }

        private static byte[] generatePayload(NotificationSpec notificationSpec) {

            try {
                JSONObject notificationJson = new JSONObject();
                notificationJson.put("msg_id", notificationSpec.getId());
                notificationJson.put("action", 0);
                notificationJson.put("app_identifier", notificationSpec.sourceAppId.substring(0,Math.min(notificationSpec.sourceAppId.length(), 31)));
                if (notificationSpec.title != null)  notificationJson.put("title", notificationSpec.title);
                if (notificationSpec.subject != null)  notificationJson.put("subtitle", notificationSpec.subject);
                if (notificationSpec.body != null)  notificationJson.put("message", notificationSpec.body);
                notificationJson.put("time_s", notificationSpec.when / 1000);
                notificationJson.put("date", new Date(notificationSpec.when).toString());
                notificationJson.put("display_name", notificationSpec.sourceName);

                JSONObject json = new JSONObject();
                json.put("ncs_notification", notificationJson);

                // Need to allocate one larger in order to null terminate.
                String jsonString = json.toString();
                byte[] bytes = new byte[jsonString.length() + 1];
                System.arraycopy(jsonString.getBytes(StandardCharsets.US_ASCII), 0, bytes, 0, jsonString.length());
                bytes[jsonString.length()] = 0;
                return bytes;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected boolean chunkMatches(byte chunk, byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.SEND_NOTIFICATION.id;
        }

        @Override
        protected void writeHeader(byte currentChunk, byte chunkCount, byte[] chunk) {
            chunk[0] = G1Constants.CommandId.SEND_NOTIFICATION.id;
            chunk[1] = 0x0;
            chunk[2] = chunkCount;
            chunk[3] = currentChunk;
        }

        @Override
        protected int getHeaderSize() {
            return 4;
        }

        @Override
        public String getName() {
            return "send_notification_" + messageId;
        }
    }

    public static class CommandSendClearNotification extends CommandHandler {
        private final int messageId;

        public CommandSendClearNotification(int messageId) {
            super(true, null);
            this.messageId = messageId;
        }

        @Override
        public byte[] serialize() {
            byte[] packet = new byte[] {
                    G1Constants.CommandId.SEND_CLEAR_NOTIFICATION.id,
                    0x0,0x0,0x0,0x0
            };
            BLETypeConversions.writeUint32BE(packet, 1, messageId);
            return packet;
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.SEND_CLEAR_NOTIFICATION.id;
        }

        @Override
        public String getName() {
            return "send_clear_notification_" + messageId;
        }
    }

    public static class CommandSetDebugLogSettings extends CommandHandler {
        private final boolean enable;
        public CommandSetDebugLogSettings(boolean enable) {
            super(false, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[]{
                G1Constants.CommandId.SYSTEM.id,
                G1Constants.SystemSubCommand.SET_DEBUG_LOGGING.id,
                enable ? G1Constants.DebugLoggingStatus.ENABLE
                       : G1Constants.DebugLoggingStatus.DISABLE
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return false;
        }

        @Override
        public String getName() {
            return "set_debug_mode_settings_" + (enable ? "enabled" : "disabled");
        }
    }

    public static class DebugLog {
        public static boolean messageMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.DEBUG_LOG.id;
        }

        public static String getMessage(byte[] payload) {
            return new String(payload, 1, payload.length-2, StandardCharsets.US_ASCII);
        }
    }

    public static class DeviceEvent {
        public static boolean messageMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.DEVICE_EVENT.id;
        }

        public static byte getEventId(byte[] payload) {
            return payload[1];
        }

        public static byte getValue(byte[] payload) {
            return payload[2];
        }
    }
}
