package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitConnectivity extends RecordData {
    public FitConnectivity(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 127) {
            throw new IllegalArgumentException("FitConnectivity expects global messages of " + 127 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getBluetoothEnabled() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(3);
    }

    @Nullable
    public Integer getLiveTrackingEnabled() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getWeatherConditionsEnabled() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getWeatherAlertsEnabled() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getAutoActivityUploadEnabled() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getCourseDownloadEnabled() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getWorkoutDownloadEnabled() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getGpsEphemerisDownloadEnabled() {
        return (Integer) getFieldByNumber(10);
    }

    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(127);
        }

        public Builder setBluetoothEnabled(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setLiveTrackingEnabled(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setWeatherConditionsEnabled(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setWeatherAlertsEnabled(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setAutoActivityUploadEnabled(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setCourseDownloadEnabled(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setWorkoutDownloadEnabled(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setGpsEphemerisDownloadEnabled(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        @Override
        public FitConnectivity build() {
            return (FitConnectivity) super.build();
        }
    }
}
