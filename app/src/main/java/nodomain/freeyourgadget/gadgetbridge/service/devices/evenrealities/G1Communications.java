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
        protected short sequence;
        private byte[] responsePayload;
        private int retryCount;

        public CommandHandler(boolean expectResponse, Function<byte[], Boolean> callback) {
            this.expectResponse = expectResponse;
            this.callback = callback;
            this.responsePayload = null;
            this.retryCount = 0;
        }

        public boolean needsGlobalSequence() { return false; }
        public void setGlobalSequence(short sequence) {
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

    public static class CommandFirmwareInfo extends CommandHandler {
        public CommandFirmwareInfo(Function<byte[], Boolean> callback) {
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
            return "firmware_info";
        }
    }

     public static class CommandBatteryLevel extends CommandHandler {
        public CommandBatteryLevel(Function<byte[], Boolean> callback) {
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
            return "battery_level";
        }
    }

    public static class CommandHeartBeat extends CommandHandler {
        public CommandHeartBeat(short sequence) {
            super(false, null);
            super.sequence = sequence;
        }

        @Override
        public byte[] serialize() {
            return new byte[] {
                G1Constants.CommandId.HEARTBEAT.id,
                0x00, // length is a short
                0x06, // length
                // TODO: What the heck is the 0x04 and why is the sequence split?
                //  Need to look at a real capture for this.
                (byte) (sequence % 0xFF),
                0x04,
                (byte) (sequence % 0xFF)
            };
        }

        @Override
        public boolean responseMatches(byte[] payload) {
            return false;
        }

        @Override
        public String getName() {
            return "heart_beat";
        }
    }

    public static class CommandTimeAndWeather extends CommandHandler {
        long timeMilliseconds;
        boolean use12HourFormat;
        byte tempInCelsius;
        byte weatherIcon;
        boolean useFahrenheit;

        public CommandTimeAndWeather(long timeMilliseconds, boolean use12HourFormat, WeatherSpec weatherInfo, boolean useFahrenheit) {
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

        @Override
        public byte[] serialize() {
            byte[] packet = new byte[] {
                G1Constants.CommandId.DASHBOARD_CONFIG.id,
                G1Constants.DashboardConfigSubCommand.SET_TIME_AND_WEATHER.id,
                // Sequence place holder
                0x00,
                0x00,
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
            BLETypeConversions.writeUint16BE(packet, 2, sequence);
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
            // TODO actually check the sequence (need to confirm the offset).
            return payload[0] == G1Constants.CommandId.DASHBOARD_CONFIG.id &&
                   payload[1] == G1Constants.DashboardConfigSubCommand.SET_TIME_AND_WEATHER.id;
           // payload[2] == (byte)(sequence >> 8) &&
           // payload[3] == (byte)sequence;
        }

        @Override
        public String getName() {
            return "time_and_weather";
        }
    }
}
