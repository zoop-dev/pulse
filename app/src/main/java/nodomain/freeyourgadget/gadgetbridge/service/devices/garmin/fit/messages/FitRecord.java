/*  Copyright (C) 2025 Freeyourgadget

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 * @noinspection unused
 */
public class FitRecord extends RecordData {
    public FitRecord(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 20) {
            throw new IllegalArgumentException("FitRecord expects global messages of " + 20 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getLatitude() {
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Double getLongitude() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Float getAltitude() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHeartRate() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getCadence() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Double getDistance() {
        return (Double) getFieldByNumber(5);
    }

    @Nullable
    public Float getSpeed() {
        return (Float) getFieldByNumber(6);
    }

    @Nullable
    public Integer getPower() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getTemperature() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Long getAccumulatedPower() {
        return (Long) getFieldByNumber(29);
    }

    @Nullable
    public Float getOscillation() {
        return (Float) getFieldByNumber(39);
    }

    @Nullable
    public Integer getActivity() {
        return (Integer) getFieldByNumber(42);
    }

    @Nullable
    public Float getFractionalCadence() {
        return (Float) getFieldByNumber(53);
    }

    @Nullable
    public Double getEnhancedSpeed() {
        return (Double) getFieldByNumber(73);
    }

    @Nullable
    public Double getEnhancedAltitude() {
        return (Double) getFieldByNumber(78);
    }

    @Nullable
    public Float getVerticalRatio() {
        return (Float) getFieldByNumber(83);
    }

    @Nullable
    public Float getStepLength() {
        return (Float) getFieldByNumber(85);
    }

    @Nullable
    public Long getAbsolutePressure() {
        return (Long) getFieldByNumber(91);
    }

    @Nullable
    public Double getDepth() {
        return (Double) getFieldByNumber(92);
    }

    @Nullable
    public Double getNextStopDepth() {
        return (Double) getFieldByNumber(93);
    }

    @Nullable
    public Long getNextStopTime() {
        return (Long) getFieldByNumber(94);
    }

    @Nullable
    public Long getTimeToSurface() {
        return (Long) getFieldByNumber(95);
    }

    @Nullable
    public Long getNdlTime() {
        return (Long) getFieldByNumber(96);
    }

    @Nullable
    public Integer getCnsLoad() {
        return (Integer) getFieldByNumber(97);
    }

    @Nullable
    public Integer getN2Load() {
        return (Integer) getFieldByNumber(98);
    }

    @Nullable
    public Integer getEnhancedRespirationRate() {
        return (Integer) getFieldByNumber(108);
    }

    @Nullable
    public Integer getWristHeartRate() {
        return (Integer) getFieldByNumber(136);
    }

    @Nullable
    public Integer getBodyBattery() {
        return (Integer) getFieldByNumber(143);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(20);
        }

        public Builder setLatitude(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setLongitude(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAltitude(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHeartRate(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setCadence(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setSpeed(final Float value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setPower(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTemperature(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setAccumulatedPower(final Long value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setOscillation(final Float value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setActivity(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setFractionalCadence(final Float value) {
            setFieldByNumber(53, value);
            return this;
        }

        public Builder setEnhancedSpeed(final Double value) {
            setFieldByNumber(73, value);
            return this;
        }

        public Builder setEnhancedAltitude(final Double value) {
            setFieldByNumber(78, value);
            return this;
        }

        public Builder setVerticalRatio(final Float value) {
            setFieldByNumber(83, value);
            return this;
        }

        public Builder setStepLength(final Float value) {
            setFieldByNumber(85, value);
            return this;
        }

        public Builder setAbsolutePressure(final Long value) {
            setFieldByNumber(91, value);
            return this;
        }

        public Builder setDepth(final Double value) {
            setFieldByNumber(92, value);
            return this;
        }

        public Builder setNextStopDepth(final Double value) {
            setFieldByNumber(93, value);
            return this;
        }

        public Builder setNextStopTime(final Long value) {
            setFieldByNumber(94, value);
            return this;
        }

        public Builder setTimeToSurface(final Long value) {
            setFieldByNumber(95, value);
            return this;
        }

        public Builder setNdlTime(final Long value) {
            setFieldByNumber(96, value);
            return this;
        }

        public Builder setCnsLoad(final Integer value) {
            setFieldByNumber(97, value);
            return this;
        }

        public Builder setN2Load(final Integer value) {
            setFieldByNumber(98, value);
            return this;
        }

        public Builder setEnhancedRespirationRate(final Integer value) {
            setFieldByNumber(108, value);
            return this;
        }

        public Builder setWristHeartRate(final Integer value) {
            setFieldByNumber(136, value);
            return this;
        }

        public Builder setBodyBattery(final Integer value) {
            setFieldByNumber(143, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitRecord build() {
            return (FitRecord) super.build();
        }
    }

    // manual changes below

    public ActivityPoint toActivityPoint() {
        final ActivityPoint activityPoint = new ActivityPoint();
        activityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getLatitude() != null && getLongitude() != null) {
            activityPoint.setLocation(new GPSCoordinate(
                    getLongitude(),
                    getLatitude(),
                    getEnhancedAltitude() != null ? getEnhancedAltitude() : GPSCoordinate.UNKNOWN_ALTITUDE
            ));
        }
        if (getHeartRate() != null) {
            activityPoint.setHeartRate(getHeartRate());
        }
        if (getEnhancedSpeed() != null) {
            activityPoint.setSpeed(getEnhancedSpeed().floatValue());
        }
        if (getCadence() != null) {
            activityPoint.setCadence(getCadence());
        }
        if (getPower() != null) {
            activityPoint.setPower(getPower());
        }
        return activityPoint;
    }
}
