package nodomain.freeyourgadget.gadgetbridge.service.devices.evenrealities;

import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;

public class G1Communications {
    private static final Logger LOG = LoggerFactory.getLogger(G1Communications.class);

    public static class CommandBrightnessGet extends G1CommandHandler {
        public CommandBrightnessGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.BRIGHTNESS_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 3 && payload[0] == G1Constants.CommandId.BRIGHTNESS_GET;
        }

        @Override
        public String getName() {
            return "brightness_get";
        }

        public static int getBrightnessLevel(byte[] payload) {
            return payload[2];
        }

        public static boolean isAutoBrightnessEnabled(byte[] payload) {
            return payload[3] == 0x01;
        }
    }

    public static class CommandBrightnessSet extends G1CommandHandler {
        private final boolean enableAutoBrightness;
        private final byte brightnessLevel;
        public CommandBrightnessSet(boolean enableAutoBrightness, byte brightnessLevel) {
            super(true, null);
            this.enableAutoBrightness = enableAutoBrightness;
            this.brightnessLevel = brightnessLevel;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.BRIGHTNESS_SET,
                    brightnessLevel,
                    enableAutoBrightness ? 0x01 : (byte)0x00
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length > 1 && payload[0] == G1Constants.CommandId.BRIGHTNESS_SET;
        }

        @Override
        public String getName() {
            return "brightness_set_" + enableAutoBrightness + "_" + brightnessLevel;
        }
    }

    public static class CommandDashboardWeatherAndTimeSet extends G1CommandHandler {
        long timeMilliseconds;
        boolean use12HourFormat;
        byte tempInCelsius;
        byte weatherIcon;
        boolean useFahrenheit;

        public CommandDashboardWeatherAndTimeSet(byte sequence, long timeMilliseconds, boolean use12HourFormat,
                                                 WeatherSpec weatherInfo, boolean useFahrenheit) {
            super(sequence, true, null);
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
        public CommandDashboardWeatherAndTimeSet(byte sequence, long timeMilliseconds, boolean use12HourFormat,
                                                 boolean useFahrenheit) {
            this(sequence, timeMilliseconds, use12HourFormat, null, useFahrenheit);
        }

        @Override
        public byte[] serialize() {
            byte[] packet = new byte[] {
                    G1Constants.CommandId.DASHBOARD_SET,
                    0x15, // Length = 21 bytes
                    0x00,
                    sequence,
                    // Subcommand
                    G1Constants.DashboardSetSubcommand.TIME_AND_WEATHER,
                    // Time 32bit place holders
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    // Time 64bit place holders
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                    (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
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
                   payload[0] == G1Constants.CommandId.DASHBOARD_SET &&
                   payload[3] == sequence &&
                   payload[4] == G1Constants.DashboardSetSubcommand.TIME_AND_WEATHER;
        }

        @Override
        public String getName() {
            return "dashboard_time_and_weather_set";
        }
    }

    public static class CommandDashboardCalendarSet extends G1ChunkedCommandHandler {
        // Formatters can be expensive to recreate repeatedly, so create them statically for re-use.
        private static final SimpleDateFormat timeFormat24h = new SimpleDateFormat("HH:mm");
        private static final SimpleDateFormat dateFormat24h = new SimpleDateFormat("EEE MM-dd");
        // The below formatters seem a bit excessive, but this is required to match the clean
        // formatting that Google Calendar has for date ranges on am/pm time.
        private static final SimpleDateFormat timeFormat12hAmPmMin = new SimpleDateFormat("h:mma");
        private static final SimpleDateFormat timeFormat12hAmPm = new SimpleDateFormat("ha");
        private static final SimpleDateFormat timeFormat12h = new SimpleDateFormat("h");
        private static final SimpleDateFormat timeFormat12hMin = new SimpleDateFormat("h:mm");
        private static final SimpleDateFormat dateFormat12h = new SimpleDateFormat("EEE MM/dd");

        static {
            // Set the AM/PM to lower case in the formatter.
            DateFormatSymbols symbols = new DateFormatSymbols(Locale.getDefault());
            symbols.setAmPmStrings(new String[] { "am", "pm" });
            timeFormat12hAmPmMin.setDateFormatSymbols(symbols);
            timeFormat12hAmPm.setDateFormatSymbols(symbols);
        }

        public static byte getRequiredSequenceCount(final byte[] payload) {
            return getChunkCountForPayloadLength(payload.length, headerSize());
        }

        public CommandDashboardCalendarSet(byte[] sequenceIds, byte[] payload, Consumer<G1CommandHandler> sendCallback) {
            super(sequenceIds, sendCallback, null, payload);
        }

        public static byte[] generatePayload(boolean use12HourFormat, List<CalendarEvent> events) {
            ByteArrayOutputStream payloadStream = new ByteArrayOutputStream( );
            try {
                // Start with the magic numbers. These might mean something, but not sure.
                payloadStream.write(new byte[] { 0x01, 0x03, 0x03 });

                if (events.isEmpty()) {
                    // If there are new events, write a dummy event saying there are no events.
                    payloadStream.write(1);
                    byte[] message = GBApplication.getContext().getString(R.string.even_realities_no_calendar_events).getBytes(StandardCharsets.UTF_8);
                    int messageLen = Math.min(0xFF, message.length);
                    payloadStream.write(0x01);
                    payloadStream.write((byte)messageLen);
                    payloadStream.write(message, 0, messageLen);
                    payloadStream.write(0x02);
                    payloadStream.write(0x00);
                    payloadStream.write(0x03);
                    payloadStream.write(0x00);
                } else {
                    payloadStream.write(
                            (byte) Math.min(G1Constants.MAX_CALENDAR_EVENTS, events.size()));
                    int i = 0;
                    for (CalendarEvent event : events) {
                        if (i >= G1Constants.MAX_CALENDAR_EVENTS) {
                            break;
                        }

                        String eventTitleString = event.getTitle();
                        byte[] title = eventTitleString.getBytes(StandardCharsets.UTF_8);
                        int titleLen = Math.min(0xFF, title.length);
                        payloadStream.write(0x01);
                        payloadStream.write((byte)titleLen);
                        payloadStream.write(title, 0, titleLen);

                        String eventTimeString = formatTime(use12HourFormat, event);
                        byte[] time = eventTimeString.getBytes(StandardCharsets.UTF_8);
                        int timeLen = Math.min(0xFF, time.length);
                        payloadStream.write(0x02);
                        payloadStream.write((byte)timeLen);
                        payloadStream.write(time, 0, timeLen);

                        byte[] location = event.getLocation().getBytes(StandardCharsets.UTF_8);
                        int locationLen = Math.min(0xFF, location.length);
                        payloadStream.write(0x03);
                        payloadStream.write((byte)locationLen);
                        payloadStream.write(location, 0, locationLen);

                        LOG.info("Calendar event: '{}' - '{}' - '{}'", eventTitleString, eventTimeString, event.getLocation());
                        // Keep track of the number of events to enforce the max count.
                        i++;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            return payloadStream.toByteArray();
        }

        private static String formatTime(boolean use12HourFormat, CalendarEvent event) {
            if (event.isAllDay()) {
                if (use12HourFormat) {
                    return GBApplication.getContext().getString(R.string.dnd_all_day) + " " + dateFormat12h.format(event.getBegin());
                } else {
                    return GBApplication.getContext().getString(R.string.dnd_all_day) + " " + dateFormat24h.format(event.getBegin());
                }
            }

            Calendar now = Calendar.getInstance();
            Calendar start = Calendar.getInstance();
            start.setTimeInMillis(event.getBegin());
            Calendar end = Calendar.getInstance();
            end.setTimeInMillis(event.getEnd());

            // Today: Show the start and end -> 12h: 11:30am-2pm | 24h: 11:30-14:00
            // Otherwise: Show Day and Time -> 12h: Thu 03/27 3:45pm | 24h: Thu 27-03 14:45
            String time = "";
            if (now.get(Calendar.YEAR) == start.get(Calendar.YEAR) &&
                now.get(Calendar.DAY_OF_YEAR) == start.get(Calendar.DAY_OF_YEAR)) {
                if (use12HourFormat) {
                    boolean amPmMatches = start.get(Calendar.AM_PM) == end.get(Calendar.AM_PM);
                    boolean startHasMinutes = start.get(Calendar.MINUTE) != 0;
                    boolean endHasMinutes = end.get(Calendar.MINUTE) != 0;

                    DateFormat startFormat;
                    if (amPmMatches) {
                        // If both are the same, am or pm can be omitted from the start time.
                        startFormat = startHasMinutes ? timeFormat12hMin: timeFormat12h;
                    } else {
                        startFormat = startHasMinutes ? timeFormat12hAmPmMin: timeFormat12hAmPm;
                    }

                    DateFormat endFormat = endHasMinutes ? timeFormat12hAmPmMin : timeFormat12hAmPm;

                    time = startFormat.format(start.getTime()) + "-" + endFormat.format(end.getTime());
                } else {
                    time = timeFormat24h.format(start.getTime()) + "-" + timeFormat24h.format(end.getTime());
                }
            } else {
                boolean startHasMinutes = start.get(Calendar.MINUTE) != 0;
                String timePart = use12HourFormat ? (startHasMinutes ? timeFormat12hAmPmMin
                                                                     : timeFormat12hAmPm).format(start.getTime())
                                                  : timeFormat24h.format(start.getTime());
                String datePart = use12HourFormat ? dateFormat12h.format(start.getTime())
                                                  : dateFormat24h.format(start.getTime());

                time = datePart + " " + timePart;
            }

            return time;
        }

        @Override
        protected boolean chunkMatches(byte currentChunk, byte currentSequence, byte[] payload) {
            return payload.length >= 8 &&
                   payload[0] == G1Constants.CommandId.DASHBOARD_SET &&
                   payload[3] == currentSequence &&
                   payload[4] == G1Constants.DashboardSetSubcommand.CALENDAR &&
                   // Current chunk for this command indexes at 1.
                   payload[7] == (byte)(currentChunk + 1);
        }

        @Override
        protected void writeHeader(byte currentChunk, byte currentSequence, byte chunkCount, byte[] chunk) {
            chunk[0] = G1Constants.CommandId.DASHBOARD_SET;
            chunk[1] = (byte)chunk.length;
            chunk[2] = 0x0;
            chunk[3] = currentSequence;
            chunk[4] = G1Constants.DashboardSetSubcommand.CALENDAR;
            chunk[5] = chunkCount;
            chunk[6] = 0x0;
            // Current chunk for this command indexes at 1. Why is this API so inconsistent? :/
            chunk[7] = (byte)(currentChunk + 1);
            chunk[8] = 0x0;
        }

        private static int headerSize() {
            return 9;
        }

        @Override
        protected int getHeaderSize() {
            return headerSize();
        }

        @Override
        public String getPacketName() {
            return "dashboard_calendar_set";
        }
    }

    public static class CommandDashboardModeSet extends G1CommandHandler {
        byte mode;
        byte secondaryPaneMode;
        public CommandDashboardModeSet(byte sequence, byte mode, byte secondaryPaneMode) {
            super(sequence, true, null);
            this.mode = mode;
            this.secondaryPaneMode = secondaryPaneMode;
        }

        @Override
        public byte[] serialize() {
            return new byte[]{
                    G1Constants.CommandId.DASHBOARD_SET,
                    0x07, // Length
                    0x00, // pad
                    sequence,
                    G1Constants.DashboardSetSubcommand.MODE,
                    mode,
                    secondaryPaneMode
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            // Command should match and the sequence should match.
            return payload.length >= 5 &&
                   payload[0] == G1Constants.CommandId.DASHBOARD_SET &&
                   payload[3] == sequence &&
                   payload[4] == G1Constants.DashboardSetSubcommand.MODE;
        }

        @Override
        public String getName() {
            return "dashboard_mode_set_" + mode + "_" + secondaryPaneMode;
        }
    }

    public static class CommandHardwareDisplaySet extends G1CommandHandler {
        private final boolean preview;
        private final byte height;
        private final byte depth;
        public CommandHardwareDisplaySet(byte sequence, boolean preview, byte height, byte depth) {
            super(sequence, true, null);
            this.preview = preview;
            this.height = height;
            this.depth = depth;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.HARDWARE_SET,
                    0x08, // Length
                    0x00,
                    sequence,
                    G1Constants.HardwareSubcommand.DISPLAY,
                    preview ? 0x01 : (byte)0x00,
                    height,
                    depth
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 6 &&
                   payload[0] == G1Constants.CommandId.HARDWARE_SET &&
                   payload[1] == 0x06 && // Response Length
                   payload[3] == sequence &&
                   payload[4] == G1Constants.HardwareSubcommand.DISPLAY;
        }

        @Override
        public String getName() {
            return "hardware_display_set_" + height + "_" + depth;
        }
    }

    public static class CommandHardwareDisplayGet extends G1CommandHandler {
        public CommandHardwareDisplayGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.HARDWARE_DISPLAY_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.HARDWARE_DISPLAY_GET;
        }

        @Override
        public String getName() {
            return "hardware_display_get";
        }

        public static int getHeight(byte[] payload) {
            return payload[2];
        }

        public static int getDepth(byte[] payload) {
            return payload[3];
        }
    }

    public static class CommandHeadUpAngleGet extends G1CommandHandler {
        public CommandHeadUpAngleGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.HEAD_UP_ANGLE_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.HEAD_UP_ANGLE_GET;
        }

        @Override
        public String getName() {
            return "head_up_angle_get";
        }

        public static int getActivationAngle(byte[] payload) {
            return payload[2];
        }
    }

    public static class CommandHeadUpAngleSet extends G1CommandHandler {
        private final byte angle;
        // Allowed Angles are 0-60.
        public CommandHeadUpAngleSet(byte angle) {
            super(true, null);
            this.angle = angle;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.HEAD_UP_ANGLE_SET,
                    angle,
                    // Magic number, other project called it the "level setting".
                    // Maybe try sending 0x00 and see what happens?
                    0x01
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.HEAD_UP_ANGLE_SET;
        }

        @Override
        public String getName() {
            return "head_up_angle_set_" + angle;
        }
    }

    public static class CommandInfoBatteryAndFirmwareGet extends G1CommandHandler {
        public CommandInfoBatteryAndFirmwareGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }
        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.INFO_BATTERY_AND_FIRMWARE_GET, 0x01 };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            if (payload.length < 1) {
                return false;
            }
            return payload[0] == G1Constants.CommandId.INFO_BATTERY_AND_FIRMWARE_GET;
        }

        @Override
        public String getName() {
            return "info_battery_and_firmware_get";
        }

        public static String getFrameType(byte[] payload) {
            // Returns A or B.
            return String.valueOf((char)payload[1]);
        }

        public static int getBatteryPercent(byte[] payload) {
            return payload[2];
        }

        public static String getFirmwareVersion(byte[] payload) {
            return String.valueOf(payload[7]) + "." + String.valueOf(payload[9]) + "." + String.valueOf(payload[9]);
        }
    }

    public static class CommandInfoSerialNumberGlassesGet extends G1CommandHandler {
        public CommandInfoSerialNumberGlassesGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.INFO_SERIAL_NUMBER_GLASSES_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 16 && payload[0] == G1Constants.CommandId.INFO_SERIAL_NUMBER_GLASSES_GET;
        }

        @Override
        public String getName() {
            return "info_serial_number_glasses_get";
        }

        public static int getFrameType(byte[] payload) {
            String serialNumber = getSerialNumber(payload);
            if (serialNumber.length() < 7) return -1;
            return switch (serialNumber.substring(4, 7)) {
                case G1Constants.HardwareDescriptionKey.COLOR_GREY ->
                        R.string.even_realities_frame_color_grey;
                case G1Constants.HardwareDescriptionKey.COLOR_BROWN ->
                        R.string.even_realities_frame_color_brown;
                case G1Constants.HardwareDescriptionKey.COLOR_GREEN ->
                        R.string.even_realities_frame_color_green;
                default -> -1;
            };
        }

        public static int getFrameColor(byte[] payload) {
            String serialNumber = getSerialNumber(payload);
            if (serialNumber.length() < 4) return -1;
            return switch (serialNumber.substring(0, 4)) {
                case G1Constants.HardwareDescriptionKey.FRAME_ROUND ->
                        R.string.even_realities_frame_shape_G1A;
                case G1Constants.HardwareDescriptionKey.FRAME_SQUARE ->
                        R.string.even_realities_frame_shape_G1B;
                default -> -1;
            };
        }

        public static String getSerialNumber(byte[] payload) {
            return new String(payload, 2, 14, StandardCharsets.US_ASCII);
        }
    }

    public static class CommandLanguageSet extends G1CommandHandler {
        private final byte language;

        public CommandLanguageSet(byte sequence, byte language) {
            super(sequence, true, null);
            this.language = language;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.LANGUAGE_SET,
                0x06, // Size
                0x00, // Pad
                sequence,
                0x01, // Magic
                language
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length == 6 &&
                   payload[0] == G1Constants.CommandId.LANGUAGE_SET &&
                   payload[3] == sequence &&
                   payload[5] == language;
        }

        @Override
        public String getName() {
            return "language_set_" + language;
        }
    }

    public static class CommandMtuSet extends G1CommandHandler {
        private final byte mtu;

        public CommandMtuSet(byte mtu) {
            super(true, null);
            this.mtu = mtu;
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.MTU_SET, mtu };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload[0] == G1Constants.CommandId.MTU_SET;
        }

        @Override
        public String getName() {
            return "mtu_set_" + mtu;
        }
    }

    public static class CommandNotificationAppListSet extends G1ChunkedCommandHandler {
        public CommandNotificationAppListSet(Consumer<G1CommandHandler> sendCallback,
                                             List<Pair<String, String>> appIdentifiers,
                                             boolean enableCalendar,
                                             boolean enableCalls,
                                             boolean enableSMS) {
            // Sequence is not used.
            super(null, sendCallback, null,
                  generatePayload(appIdentifiers, enableCalendar, enableCalls, enableSMS));
        }

        private static byte[] generatePayload(List<Pair<String, String>> appIdentifiers, boolean enableCalendar, boolean enableCalls, boolean enableSMS) {
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
                System.arraycopy(jsonString.getBytes(StandardCharsets.US_ASCII),
                                 0, bytes, 0, jsonString.length());
                bytes[jsonString.length()] = 0;
                return bytes;
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected boolean chunkMatches(byte chunk, byte currentSequence, byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.NOTIFICATION_APP_LIST_SET;
        }

        @Override
        protected void writeHeader(byte currentChunk, byte currentSequence, byte chunkCount, byte[] chunk) {
            chunk[0] = G1Constants.CommandId.NOTIFICATION_APP_LIST_SET;
            chunk[1] = chunkCount;
            chunk[2] = currentChunk;
        }

        @Override
        protected int getHeaderSize() {
            return 3;
        }

        @Override
        public String getPacketName() {
            return "notification_app_list_set";
        }
    }

    public static class CommandNotificationAutoDisplayGet extends G1CommandHandler {
        public CommandNotificationAutoDisplayGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.NOTIFICATION_AUTO_DISPLAY_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.NOTIFICATION_AUTO_DISPLAY_GET;
        }

        @Override
        public String getName() {
            return "notification_auto_display_get";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == 0x01;
        }

        public static int getTimeout(byte[] payload) {
            return payload[3];
        }
    }

    public static class CommandNotificationAutoDisplaySet extends G1CommandHandler {
        private final boolean enable;
        private final byte timeout;
        public CommandNotificationAutoDisplaySet(boolean enable, byte timeout) {
            super(true, null);
            this.enable = enable;
            this.timeout = timeout;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.NOTIFICATION_AUTO_DISPLAY_SET,
                    enable ? 0x01 : (byte)0x00,
                    timeout
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.NOTIFICATION_AUTO_DISPLAY_SET;
        }

        @Override
        public String getName() {
            return "notification_auto_display_set_" + (enable ? "enabled" : "disabled") + "_" + timeout;
        }
    }

    public static class CommandNotificationSendControl extends G1ChunkedCommandHandler {
        private final int messageId;

        public CommandNotificationSendControl(Consumer<G1CommandHandler> sendCallback, NotificationSpec notificationSpec) {
            // Sequence is not used.
            super(null, sendCallback, null, generatePayload(notificationSpec));
            this.messageId = notificationSpec.getId();
        }

        private static byte[] generatePayload(NotificationSpec notificationSpec) {
            try {
                JSONObject notificationJson = new JSONObject();
                notificationJson.put("msg_id", notificationSpec.getId());
                notificationJson.put("action", 0);
                notificationJson.put("app_identifier",
                                     notificationSpec.sourceAppId.substring(
                                             0,Math.min(notificationSpec.sourceAppId.length(), 31)));
                if (notificationSpec.title != null)
                    notificationJson.put("title", notificationSpec.title);
                if (notificationSpec.subject != null)
                    notificationJson.put("subtitle", notificationSpec.subject);
                if (notificationSpec.body != null)
                    notificationJson.put("message", notificationSpec.body);
                notificationJson.put("time_s", notificationSpec.when / 1000);
                notificationJson.put("date", new Date(notificationSpec.when).toString());
                notificationJson.put("display_name", notificationSpec.sourceName);

                JSONObject json = new JSONObject();
                json.put("ncs_notification", notificationJson);
                return json.toString().getBytes(StandardCharsets.UTF_8);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected boolean chunkMatches(byte chunk, byte currentSequence, byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.NOTIFICATION_SEND_CONTROL;
        }

        @Override
        protected void writeHeader(byte currentChunk, byte currentSequence, byte chunkCount, byte[] chunk) {
            chunk[0] = G1Constants.CommandId.NOTIFICATION_SEND_CONTROL;
            chunk[1] = 0x0;
            chunk[2] = chunkCount;
            chunk[3] = currentChunk;
        }

        @Override
        protected int getHeaderSize() {
            return 4;
        }

        @Override
        public String getPacketName() {
            return "notification_send_control_" + messageId;
        }
    }

    public static class CommandNotificationClearControl extends G1CommandHandler {
        private final int messageId;

        public CommandNotificationClearControl(int messageId) {
            super(true, null);
            this.messageId = messageId;
        }

        @Override
        public byte[] serialize() {
            byte[] packet = new byte[] {
                    G1Constants.CommandId.NOTIFICATION_CLEAR_CONTROL,
                    0x0,0x0,0x0,0x0
            };
            BLETypeConversions.writeUint32BE(packet, 1, messageId);
            return packet;
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.CommandId.NOTIFICATION_CLEAR_CONTROL;
        }

        @Override
        public String getName() {
            return "notification_clear_control" + messageId;
        }
    }

    public static class CommandSilentModeGet extends G1CommandHandler {
        public CommandSilentModeGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.SILENT_MODE_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 4 && payload[0] == G1Constants.CommandId.SILENT_MODE_GET;
        }

        @Override
        public String getName() {
            return "silent_mode_get";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == G1Constants.SilentStatus.ENABLE;
        }
    }

    public static class CommandSilentModeSet extends G1CommandHandler {
        private final boolean enable;
        public CommandSilentModeSet(boolean enable) {
            super(true, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.SILENT_MODE_SET,
                    (byte)(enable ? G1Constants.SilentStatus.ENABLE : G1Constants.SilentStatus.DISABLE),
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length > 1 && payload[0] == G1Constants.CommandId.SILENT_MODE_SET;
        }

        @Override
        public String getName() {
            return "silent_mode_set_" + (enable ? "enabled" : "disabled");
        }
    }

    public static class CommandSystemDebugLoggingSet extends G1CommandHandler {
        private final boolean enable;
        public CommandSystemDebugLoggingSet(boolean enable) {
            super(false, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[]{
                    G1Constants.CommandId.SYSTEM_CONTROL,
                    G1Constants.SystemSubcommand.DEBUG_LOGGING_SET,
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
            return "system_debug_logging_set_" + (enable ? "enabled" : "disabled");
        }
    }

    public static class CommandSystemRebootControl extends G1CommandHandler {
        public CommandSystemRebootControl() {
            super(false, null);
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.SYSTEM_CONTROL,
                    G1Constants.SystemSubcommand.REBOOT
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return false;
        }

        @Override
        public String getName() {
            return "system_reboot_control";
        }
    }

    public static class CommandSystemFirmwareBuildStringGet extends G1CommandHandler {
        public CommandSystemFirmwareBuildStringGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.SYSTEM_CONTROL,
                    G1Constants.SystemSubcommand.FIRMWARE_BUILD_STRING_GET
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            if (payload.length < 10) {
                return false;
            }
            return payload[0] == G1Constants.SYSTEM_FIRMWARE_BUILD_STRING_PREFIX;
        }

        @Override
        public String getName() {
            return "system_firmware_build_string_get";
        }
    }

    public static class CommandWearDetectionGet extends G1CommandHandler {
        public CommandWearDetectionGet(Function<byte[], Boolean> callback) {
            super(true, callback);
        }

        @Override
        public byte[] serialize() {
            return new byte[] { G1Constants.CommandId.WEAR_DETECTION_GET };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.WEAR_DETECTION_GET;
        }

        @Override
        public String getName() {
            return "wear_detection_get";
        }

        public static boolean isEnabled(byte[] payload) {
            return payload[2] == 0x01;
        }
    }

    public static class CommandWearDetectionSet extends G1CommandHandler {
        private final boolean enable;
        public CommandWearDetectionSet(boolean enable) {
            super(true, null);
            this.enable = enable;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                    G1Constants.CommandId.WEAR_DETECTION_SET,
                    enable ? 0x01 : (byte)0x00
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.CommandId.WEAR_DETECTION_SET;
        }

        @Override
        public String getName() {
            return "wear_detection_set_" + (enable ? "enabled" : "disabled");
        }
    }

    /*--------------------------------------------------------------------------------------------*/

    public static class MessageDebug {
        public static boolean messageMatches(byte[] payload) {
            return payload.length >= 1 && payload[0] == G1Constants.MessageId.DEBUG;
        }

        public static String getMessage(byte[] payload) {
            return new String(payload, 1, payload.length-2, StandardCharsets.UTF_8);
        }
    }

    public static class MessageEvent {
        public static boolean messageMatches(byte[] payload) {
            return payload.length >= 2 && payload[0] == G1Constants.MessageId.EVENT;
        }

        public static byte getEventId(byte[] payload) {
            return payload[1];
        }

        public static byte getValue(byte[] payload) {
            return payload[2];
        }
    }
}
