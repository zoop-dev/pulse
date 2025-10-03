/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Weather {
    public static final byte id = 0x0f;

    public static class Settings {
        // WeatherSupport
        public boolean weatherSupported = false;
        public boolean windSupported = false;
        public boolean pm25Supported = false;
        public boolean temperatureSupported = false;
        public boolean locationNameSupported = false;
        public boolean currentTemperatureSupported = false;
        public boolean unitSupported = false;
        public boolean airQualityIndexSupported = false;

        // WeatherExtendedSupport
        public boolean timeSupported = false;
        public boolean sourceSupported = false;
        public boolean weatherIconSupported = false;
        public boolean extendedHourlyForecast = false;

        // WeatherSunMoonSupport
        public boolean sunRiseSetSupported = false;
        public boolean moonPhaseSupported = false;

        // ExpandCapabilityRequest
        public boolean uvIndexSupported = false;
    }

    public enum WeatherIcon {
        // Also used for the text on the watch
        SUNNY,
        CLOUDY,
        OVERCAST,
        SHOWERS,
        THUNDERSTORMS,
        THUNDER_AND_HAIL,
        SLEET,
        LIGHT_RAIN,
        RAIN,
        HEAVY_RAIN,
        RAIN_STORM,
        HEAVY_RAIN_STORMS,
        SEVERE_RAIN_STORMS,
        SNOW_FLURRIES,
        LIGHT_SNOW,
        SNOW,
        HEAVY_SNOW,
        SNOWSTORMS,
        FOG,
        FREEZING_RAIN,
        DUST_STORM,
        LIGHT_TO_MODERATE_RAIN,
        MODERATE_TO_HEAVY_RAIN,
        HEAVY_TO_SEVERE_RAIN,
        HEAVY_TO_TORRENTIAL_RAIN,
        SEVERE_TO_TORRENTIAL_RAIN,
        LIGHT_TO_MODERATE_SNOW,
        MODERATE_TO_HEAVY_SNOW,
        HEAVY_SNOW_TO_BLIZZARD,
        DUST,
        SAND,
        SANDSTORMS,
        FREEZING, // misses small/non-moving icon
        HOT, // misses small/non-moving icon
        COLD, // misses small/non-moving icon
        WINDY,
        HAZY,
        UNKNOWN // Good to have probably
    }

    private static byte iconToByte(WeatherIcon weatherIcon) {
        return switch (weatherIcon) {
            case SUNNY -> 0x00;
            case CLOUDY -> 0x01;
            case OVERCAST -> 0x02;
            case SHOWERS -> 0x03;
            case THUNDERSTORMS -> 0x04;
            case THUNDER_AND_HAIL -> 0x05;
            case SLEET -> 0x06;
            case LIGHT_RAIN -> 0x07;
            case RAIN -> 0x08;
            case HEAVY_RAIN -> 0x09;
            case RAIN_STORM -> 0x0a;
            case HEAVY_RAIN_STORMS -> 0x0b;
            case SEVERE_RAIN_STORMS -> 0x0c;
            case SNOW_FLURRIES -> 0x0d;
            case LIGHT_SNOW -> 0x0e;
            case SNOW -> 0x0f;
            case HEAVY_SNOW -> 0x10;
            case SNOWSTORMS -> 0x11;
            case FOG -> 0x12;
            case FREEZING_RAIN -> 0x13;
            case DUST_STORM -> 0x14;
            case LIGHT_TO_MODERATE_RAIN -> 0x15;
            case MODERATE_TO_HEAVY_RAIN -> 0x16;
            case HEAVY_TO_SEVERE_RAIN -> 0x17;
            case HEAVY_TO_TORRENTIAL_RAIN -> 0x18;
            case SEVERE_TO_TORRENTIAL_RAIN -> 0x19;
            case LIGHT_TO_MODERATE_SNOW -> 0x1a;
            case MODERATE_TO_HEAVY_SNOW -> 0x1b;
            case HEAVY_SNOW_TO_BLIZZARD -> 0x1c;
            case DUST -> 0x1d;
            case SAND -> 0x1e;
            case SANDSTORMS -> 0x1f;
            case FREEZING -> 0x20;
            case HOT -> 0x21;
            case COLD -> 0x22;
            case WINDY -> 0x23;
            case HAZY -> 0x35;
            default -> 0x63; // Any higher and the current weather breaks
        };
    }

    private static WeatherIcon byteToIcon(byte weatherIcon) {
        return switch (weatherIcon) {
            case 0x00 -> WeatherIcon.SUNNY;
            case 0x01 -> WeatherIcon.CLOUDY;
            case 0x02 -> WeatherIcon.OVERCAST;
            case 0x03 -> WeatherIcon.SHOWERS;
            case 0x04 -> WeatherIcon.THUNDERSTORMS;
            case 0x05 -> WeatherIcon.THUNDER_AND_HAIL;
            case 0x06 -> WeatherIcon.SLEET;
            case 0x07 -> WeatherIcon.LIGHT_RAIN;
            case 0x08 -> WeatherIcon.RAIN;
            case 0x09 -> WeatherIcon.HEAVY_RAIN;
            case 0x0a -> WeatherIcon.RAIN_STORM;
            case 0x0b -> WeatherIcon.HEAVY_RAIN_STORMS;
            case 0x0c -> WeatherIcon.SEVERE_RAIN_STORMS;
            case 0x0d -> WeatherIcon.SNOW_FLURRIES;
            case 0x0e -> WeatherIcon.LIGHT_SNOW;
            case 0x0f -> WeatherIcon.SNOW;
            case 0x10 -> WeatherIcon.HEAVY_SNOW;
            case 0x11 -> WeatherIcon.SNOWSTORMS;
            case 0x12 -> WeatherIcon.FOG;
            case 0x13 -> WeatherIcon.FREEZING_RAIN;
            case 0x14 -> WeatherIcon.DUST_STORM;
            case 0x15 -> WeatherIcon.LIGHT_TO_MODERATE_RAIN;
            case 0x16 -> WeatherIcon.MODERATE_TO_HEAVY_RAIN;
            case 0x17 -> WeatherIcon.HEAVY_TO_SEVERE_RAIN;
            case 0x18 -> WeatherIcon.HEAVY_TO_TORRENTIAL_RAIN;
            case 0x19 -> WeatherIcon.SEVERE_TO_TORRENTIAL_RAIN;
            case 0x1a -> WeatherIcon.LIGHT_TO_MODERATE_SNOW;
            case 0x1b -> WeatherIcon.MODERATE_TO_HEAVY_SNOW;
            case 0x1c -> WeatherIcon.HEAVY_SNOW_TO_BLIZZARD;
            case 0x1d -> WeatherIcon.DUST;
            case 0x1e -> WeatherIcon.SAND;
            case 0x1f -> WeatherIcon.SANDSTORMS;
            case 0x20 -> WeatherIcon.FREEZING;
            case 0x21 -> WeatherIcon.HOT;
            case 0x22 -> WeatherIcon.COLD;
            case 0x23 -> WeatherIcon.WINDY;
            case 0x35 -> WeatherIcon.HAZY;
            default -> WeatherIcon.UNKNOWN;
        };
    }

    public enum HuaweiTemperatureFormat {
        CELSIUS,
        FAHRENHEIT
    }

    private static byte temperatureFormatToByte(HuaweiTemperatureFormat temperatureFormat) {
        if (temperatureFormat == HuaweiTemperatureFormat.FAHRENHEIT)
            return 1;
        return 0;
    }

    public enum MoonPhase {
        NEW_MOON,
        WAXING_CRESCENT,
        FIRST_QUARTER,
        WAXING_GIBBOUS,
        FULL_MOON,
        WANING_GIBBOUS,
        THIRD_QUARTER,
        WANING_CRESCENT
    }

    public static MoonPhase degreesToMoonPhase(int degrees) {
        final int leeway = 6; // Give some leeway for the new moon, first quarter, full moon, and third quarter
        if (degrees < 0 || degrees > 360)
            return null;
        else if (degrees >= 360 - leeway || degrees <= leeway)
            return MoonPhase.NEW_MOON;
        else if (degrees < 90)
            return MoonPhase.WAXING_CRESCENT;
        else if (degrees <= 90 + leeway)
            return MoonPhase.FIRST_QUARTER;
        else if (degrees < 180 - leeway)
            return MoonPhase.WAXING_GIBBOUS;
        else if (degrees <= 180 + leeway)
            return MoonPhase.FULL_MOON;
        else if (degrees < 270 - leeway)
            return MoonPhase.WANING_GIBBOUS;
        else if (degrees <= 270 + leeway)
            return MoonPhase.THIRD_QUARTER;
        else
            return MoonPhase.WANING_CRESCENT;
    }

    private static byte moonPhaseToByte (MoonPhase moonPhase) {
        return switch (moonPhase) {
            case NEW_MOON -> 1;
            case WAXING_CRESCENT -> 2;
            case FIRST_QUARTER -> 3;
            case WAXING_GIBBOUS -> 4;
            case FULL_MOON -> 5;
            case WANING_GIBBOUS -> 6;
            case THIRD_QUARTER -> 7;
            case WANING_CRESCENT -> 8;
        };
    }

    private static MoonPhase byteToMoonPhase(byte moonPhase) {
        return switch (moonPhase) {
            case 1 -> MoonPhase.NEW_MOON;
            case 2 -> MoonPhase.WAXING_CRESCENT;
            case 3 -> MoonPhase.FIRST_QUARTER;
            case 4 -> MoonPhase.WAXING_GIBBOUS;
            case 5 -> MoonPhase.FULL_MOON;
            case 6 -> MoonPhase.WANING_GIBBOUS;
            case 7 -> MoonPhase.THIRD_QUARTER;
            case 8 -> MoonPhase.WANING_CRESCENT;
            default -> null;
        };
    }

    // no wind - 0, NE - 1, E - 2, SE - 3, S - 4, SW - 5, W - 6, NW - 7, N - 8
    private static int convertWindDirection(int degrees) {
        if (degrees < 0 || degrees > 360)
            return 0;
        int val = ((int)((degrees/45.0) + 0.5)) % 8;
        return (val == 0)?8:val;
    }

    public enum ErrorCode {
        NETWORK_ERROR,
        GPS_PERMISSION_ERROR,
        WEATHER_DISABLED
    }

    private static byte errorCodeToByte(ErrorCode errorCode) {
        return switch (errorCode) {
            case NETWORK_ERROR -> 0;
            case GPS_PERMISSION_ERROR -> 1;
            case WEATHER_DISABLED -> 2;
        };
    }

    public static class CurrentWeatherRequest extends HuaweiPacket {
        public static final byte id = 0x01;

        public CurrentWeatherRequest(
                ParamsProvider paramsProvider,
                Settings settings,
                WeatherIcon icon,
                Integer windDirection,
                Byte windSpeed,
                Byte lowestTemperature,
                Byte highestTemperature,
                Short pm25, // TODO: might be float?
                String locationName,
                Byte currentTemperature,
                HuaweiTemperatureFormat temperatureUnit,
                Short airQualityIndex,
                Integer observationTime,
                Float uvIndex,
                String sourceName,
                Byte humidity,
                Integer windSpeedValue,
                Integer feelLikeTemperature
        ) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV();

            HuaweiTLV tlv81 = new HuaweiTLV();

            if (icon != null && settings.weatherIconSupported) {
                tlv81.put(0x02, iconToByte(icon));
            }

            if (settings.windSupported) {
                short wind = 0;
                if (windSpeed != null)
                    wind = (short) windSpeed;

                if (windDirection != null) {
                    wind |= (short) (convertWindDirection(windDirection) << 8);
                }
                tlv81.put(0x03, wind);
            }

            if (settings.weatherIconSupported || settings.windSupported)
                this.tlv.put(0x81, tlv81);

            if (lowestTemperature != null && highestTemperature != null && settings.temperatureSupported) {
                this.tlv.put(0x85, new HuaweiTLV()
                        .put(0x06, lowestTemperature)
                        .put(0x07, highestTemperature)
                );
            }
            if (pm25 != null && settings.pm25Supported)
                this.tlv.put(0x04, pm25);
            if (locationName != null && settings.locationNameSupported)
                this.tlv.put(0x08, locationName);
            if (currentTemperature != null && settings.currentTemperatureSupported)
                this.tlv.put(0x09, currentTemperature);
            if (temperatureUnit != null && settings.unitSupported)
                this.tlv.put(0x0a, temperatureFormatToByte(temperatureUnit));
            if (airQualityIndex != null && settings.airQualityIndexSupported)
                this.tlv.put(0x0b, airQualityIndex);
            if (observationTime != null && settings.timeSupported)
                this.tlv.put(0x0c, observationTime);
            if (sourceName != null && settings.sourceSupported)
                this.tlv.put(0x0e, sourceName);
            if (uvIndex != null && settings.uvIndexSupported)
                this.tlv.put(0x0f, (byte)Math.round(uvIndex));

            if(settings.extendedHourlyForecast) {
                if(humidity != null)
                    this.tlv.put(0x10, humidity);
                if(windSpeedValue != null)
                    this.tlv.put(0x11, windSpeedValue);
                if(feelLikeTemperature != null)
                    this.tlv.put(0x12, feelLikeTemperature);

            }

            this.isEncrypted = true;
            this.complete = true;
        }
    }

    public static class WeatherSupport {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public byte supportedBitmap = 0;

            public boolean weatherSupported = false;
            public boolean windSupported = false;
            public boolean pm25Supported = false;
            public boolean temperatureSupported = false;
            public boolean locationNameSupported = false;
            public boolean currentTemperatureSupported = false;
            public boolean unitSupported = false;
            public boolean airQualityIndexSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getByte(0x01);

                this.weatherSupported = (this.supportedBitmap & 0x01) != 0;
                this.windSupported = (this.supportedBitmap & 0x02) != 0;
                this.pm25Supported = (this.supportedBitmap & 0x04) != 0;
                this.temperatureSupported = (this.supportedBitmap & 0x08) != 0;
                this.locationNameSupported = (this.supportedBitmap & 0x10) != 0;
                this.currentTemperatureSupported = (this.supportedBitmap & 0x20) != 0;
                this.unitSupported = (this.supportedBitmap & 0x40) != 0;
                this.airQualityIndexSupported = (this.supportedBitmap & 0x80) != 0;
            }
        }
    }

    public static class WeatherDeviceRequest extends HuaweiPacket {
        public static final byte id = 0x04;

        public WeatherDeviceRequest(ParamsProvider paramsProvider, int responseValue) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV().put(0x01, responseValue);
            this.isEncrypted = false;
            this.complete = true;
        }
    }

    public static class WeatherUnitRequest extends HuaweiPacket {
        public static final byte id = 0x05;

        public WeatherUnitRequest(ParamsProvider paramsProvider, HuaweiTemperatureFormat temperatureFormat) {
            super(paramsProvider);

            this.serviceId = Weather.id;
            this.commandId = id;
            this.tlv = new HuaweiTLV().put(0x01, temperatureFormatToByte(temperatureFormat));
            this.isEncrypted = true;
            this.complete = true;
        }
    }

    public static class WeatherExtendedSupport {
        public static final byte id = 0x06;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public short supportedBitmap = 0;

            public boolean timeSupported = false;
            public boolean sourceSupported = false;
            public boolean weatherIconSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getShort(0x01);

                this.timeSupported = (this.supportedBitmap & 0x01) != 0;
                this.sourceSupported = (this.supportedBitmap & 0x02) != 0;
                this.weatherIconSupported = (this.supportedBitmap & 0x04) != 0;
            }
        }
    }

    public static class WeatherErrorSimple {
        public static final byte id = 0x07;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, ErrorCode errorCode) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01, errorCodeToByte(errorCode));
                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }

    public static class WeatherForecastData {
        public static final byte id = 0x08;

        public static class TimeData {
            public int timestamp;
            public WeatherIcon icon;
            public Byte temperature;
            public Byte precipitation;
            public Byte uvIndex;

            @NonNull
            @Override
            public String toString() {
                String timestampStr = new Date(timestamp * 1000L).toString();
                return "TimeData{" +
                        "timestamp=" + timestamp +
                        ", timestamp=" + timestampStr +
                        ", icon=" + icon +
                        ", temperature=" + temperature +
                        ", precipitation=" + precipitation +
                        ", uvIndex=" + uvIndex +
                        '}';
            }
        }

        public static class DayData {
            public int timestamp;
            public WeatherIcon icon;
            public Byte highTemperature;
            public Byte lowTemperature;
            public Integer sunriseTime;
            public Integer sunsetTime;
            public Integer moonRiseTime;
            public Integer moonSetTime;
            public MoonPhase moonPhase;

            @NonNull
            @Override
            public String toString() {
                String timestampStr = new Date(timestamp * 1000L).toString();
                return "DayData{" +
                        "timestamp=" + timestamp +
                        ", timestamp=" + timestampStr +
                        ", icon=" + icon +
                        ", highTemperature=" + highTemperature +
                        ", lowTemperature=" + lowTemperature +
                        ", sunriseTime=" + sunriseTime +
                        ", sunsetTime=" + sunsetTime +
                        ", moonRiseTime=" + moonRiseTime +
                        ", moonSetTime=" + moonSetTime +
                        ", moonPhase=" + moonPhase +
                        '}';
            }
        }

        public static class Request extends HuaweiPacket {
            public Request(
                    ParamsProvider paramsProvider,
                    Settings settings,
                    List<TimeData> timeDataList,
                    List<DayData> dayDataList
            ) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV();

                if (timeDataList != null && !timeDataList.isEmpty()) {
                    HuaweiTLV timeDataTlv = new HuaweiTLV();
                    for (TimeData timeData : timeDataList) {
                        HuaweiTLV timeTlv = new HuaweiTLV();
                        timeTlv.put(0x03, timeData.timestamp);
                        if (timeData.icon != null && settings.weatherIconSupported)
                            timeTlv.put(0x04, iconToByte(timeData.icon));
                        if (timeData.temperature != null && (settings.temperatureSupported || settings.currentTemperatureSupported))
                            timeTlv.put(0x05, timeData.temperature);
                        if(settings.extendedHourlyForecast) {
                            if (timeData.precipitation != null) {
                                timeTlv.put(0x06, timeData.precipitation);
                            }
                            if (timeData.uvIndex != null) {
                                timeTlv.put(0x07, timeData.uvIndex);
                            }
                        }
                        timeDataTlv.put(0x82, timeTlv);
                    }
                    this.tlv.put(0x81, timeDataTlv);
                }

                if (dayDataList != null && !dayDataList.isEmpty()) {
                    HuaweiTLV dayDataTlv = new HuaweiTLV();
                    for (DayData dayData : dayDataList) {
                        HuaweiTLV dayTlv = new HuaweiTLV();
                        dayTlv.put(0x12, dayData.timestamp);
                        if (dayData.icon != null && settings.weatherIconSupported)
                            dayTlv.put(0x13, iconToByte(dayData.icon));
                        if (settings.temperatureSupported) {
                            if (dayData.highTemperature != null)
                                dayTlv.put(0x14, dayData.highTemperature);
                            if (dayData.lowTemperature != null)
                                dayTlv.put(0x15, dayData.lowTemperature);
                        }
                        if (settings.sunRiseSetSupported) {
                            if (dayData.sunriseTime != null && dayData.sunriseTime != 0)
                                dayTlv.put(0x16, dayData.sunriseTime);
                            if (dayData.sunsetTime != null && dayData.sunsetTime != 0)
                                dayTlv.put(0x17, dayData.sunsetTime);
                            if (dayData.moonRiseTime != null && dayData.moonRiseTime != 0)
                                dayTlv.put(0x1a, dayData.moonRiseTime);
                            if (dayData.moonSetTime != null && dayData.moonSetTime != 0)
                                dayTlv.put(0x1b, dayData.moonSetTime);
                        }
                        if (dayData.moonPhase != null && settings.moonPhaseSupported)
                            dayTlv.put(0x1e, moonPhaseToByte(dayData.moonPhase));
                        dayDataTlv.put(0x91, dayTlv);
                    }
                    this.tlv.put(0x90, dayDataTlv);
                }

                this.isEncrypted = true;
                this.isSliced = true;
                this.complete = true;
            }
        }

        public static class OutgoingRequest extends HuaweiPacket {
            List<TimeData> timeDataList;
            List<DayData> dayDataList;

            public OutgoingRequest(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.complete = false;
            }

            @Override
            public void parseTlv() throws ParseException {
                timeDataList = new ArrayList<>(this.tlv.getObject(0x81).getObjects(0x82).size());
                for (HuaweiTLV timeTlv : this.tlv.getObject(0x81).getObjects(0x82)) {
                    TimeData timeData = new TimeData();
                    timeData.timestamp = timeTlv.getInteger(0x03);
                    timeData.icon = byteToIcon(timeTlv.getByte(0x04));
                    timeData.temperature = timeTlv.getByte(0x05);
                    timeDataList.add(timeData);
                }

                dayDataList = new ArrayList<>(this.tlv.getObject(0x90).getObjects(0x91).size());
                for (HuaweiTLV dayTlv : this.tlv.getObject(0x90).getObjects(0x91)) {
                    DayData dayData = new DayData();
                    dayData.timestamp = dayTlv.getInteger(0x12);
                    dayData.icon = byteToIcon(dayTlv.getByte(0x13));
                    dayData.highTemperature = dayTlv.getByte(0x14);
                    dayData.lowTemperature = dayTlv.getByte(0x15);
                    dayData.sunriseTime = dayTlv.getInteger(0x16);
                    dayData.sunsetTime = dayTlv.getInteger(0x17);
                    dayData.moonRiseTime = dayTlv.getInteger(0x1a);
                    dayData.moonSetTime = dayTlv.getInteger(0x1b);
                    dayData.moonPhase = byteToMoonPhase(dayTlv.getByte(0x1e));
                    dayDataList.add(dayData);
                }
            }
        }
    }

    public static class WeatherStart {
        public static final byte id = 0x09;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01, (byte) 0x03); // TODO: find out what this means
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public int successCode = -1;
            public boolean success = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                this.successCode = this.tlv.getInteger(0x7f);
                this.success = this.successCode == 0x000186A0 || this.successCode == 0x000186A3;
            }
        }
    }

    public static class WeatherSunMoonSupport {
        public static final byte id = 0x0a;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV().put(0x01);
                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public int supportedBitmap = 0;

            public boolean sunRiseSetSupported = false;
            public boolean moonPhaseSupported = false;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = Weather.id;
                this.commandId = id;
            }

            @Override
            public void parseTlv() throws ParseException {
                if (!this.tlv.contains(0x01))
                    throw new MissingTagException(0x01);
                this.supportedBitmap = this.tlv.getInteger(0x01);

                this.sunRiseSetSupported = (this.supportedBitmap & 0x01) != 0;
                this.moonPhaseSupported = (this.supportedBitmap & 0x02) != 0;
            }
        }
    }

    public static class WeatherErrorExtended {
        public static final byte id = 0x0c;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, ErrorCode errorCode, boolean serverError) {
                super(paramsProvider);

                this.serviceId = Weather.id;
                this.commandId = id;

                HuaweiTLV innerTlv = new HuaweiTLV();
                innerTlv.put(0x02, errorCodeToByte(errorCode));
                if (errorCode == ErrorCode.NETWORK_ERROR && serverError)
                    innerTlv.put(0x03, (byte) 0x01);

                this.tlv = new HuaweiTLV().put(0x81, innerTlv);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }
}
