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

import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitMonitoring extends RecordData {
    public FitMonitoring(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 55) {
            throw new IllegalArgumentException("FitMonitoring expects global messages of " + 55 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getDeviceIndex() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getCalories() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Long getDistance() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Long getCycles() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public Long getActiveTime() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Integer getActivityType() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getActivitySubtype() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getActivityLevel() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getDistance16() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getCycles16() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getActiveTime16() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(11, Long.class);
    }

    @Nullable
    public Float getTemperature() {
        return getFieldByNumber(12, Float.class);
    }

    @Nullable
    public Float getTemperatureMin() {
        return getFieldByNumber(14, Float.class);
    }

    @Nullable
    public Float getTemperatureMax() {
        return getFieldByNumber(15, Float.class);
    }

    @Nullable
    public Number[] getActivityTime() {
        return getArrayFieldByNumber(16, Number.class);
    }

    @Nullable
    public Integer getActiveCalories() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Integer getDurationMin() {
        return getFieldByNumber(29, Integer.class);
    }

    @Nullable
    public Integer getCurrentActivityTypeIntensity() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Integer getTimestamp16() {
        return getFieldByNumber(26, Integer.class);
    }

    @Nullable
    public Integer getTimestampMin8() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getHeartRate() {
        return getFieldByNumber(27, Integer.class);
    }

    @Nullable
    public Float getIntensity() {
        return getFieldByNumber(28, Float.class);
    }

    @Nullable
    public Long getDuration() {
        return getFieldByNumber(30, Long.class);
    }

    @Nullable
    public Double getAscent() {
        return getFieldByNumber(31, Double.class);
    }

    @Nullable
    public Double getDescent() {
        return getFieldByNumber(32, Double.class);
    }

    @Nullable
    public Integer getModerateActivityMinutes() {
        return getFieldByNumber(33, Integer.class);
    }

    @Nullable
    public Integer getVigorousActivityMinutes() {
        return getFieldByNumber(34, Integer.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(55);
        }

        public Builder setDeviceIndex(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setCalories(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setDistance(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setCycles(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setActiveTime(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setActivityType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setActivitySubtype(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setActivityLevel(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setDistance16(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setCycles16(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setActiveTime16(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setTemperature(final Float value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setTemperatureMin(final Float value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setTemperatureMax(final Float value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setActivityTime(final Number[] value) {
            setFieldByNumber(16, (Object[]) value);
            return this;
        }

        public Builder setActiveCalories(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setDurationMin(final Integer value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setCurrentActivityTypeIntensity(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setTimestamp16(final Integer value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setTimestampMin8(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setHeartRate(final Integer value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setIntensity(final Float value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setDuration(final Long value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setAscent(final Double value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setDescent(final Double value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setModerateActivityMinutes(final Integer value) {
            setFieldByNumber(33, value);
            return this;
        }

        public Builder setVigorousActivityMinutes(final Integer value) {
            setFieldByNumber(34, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitMonitoring build() {
            return (FitMonitoring) super.build();
        }

        @Override
        public FitMonitoring build(final int localMessageType) {
            return (FitMonitoring) super.build(localMessageType);
        }
    }

    // manual changes below

    public Long computeTimestamp(final Long lastMonitoringTimestamp) {
        final Integer timestamp16 = getTimestamp16();

        if (timestamp16 != null && lastMonitoringTimestamp != null) {
            final int referenceGarminTs = GarminTimeUtils.unixTimeToGarminTimestamp(lastMonitoringTimestamp.intValue());
            return (long) (lastMonitoringTimestamp.intValue() + ((timestamp16 - (referenceGarminTs & 0xffff)) & 0xffff));
        }

        return getComputedTimestamp();
    }

    public Optional<Integer> getComputedActivityType() {
        final Integer activityType = getActivityType();
        if (activityType != null) {
            return Optional.of(activityType);
        }

        final Integer currentActivityTypeIntensity = getCurrentActivityTypeIntensity();
        if (currentActivityTypeIntensity != null) {
            return Optional.of(currentActivityTypeIntensity & 0x1F);
        }

        return Optional.empty();
    }

    public Integer getComputedIntensity() {
        final Integer currentActivityTypeIntensity = getCurrentActivityTypeIntensity();
        if (currentActivityTypeIntensity != null) {
            return (currentActivityTypeIntensity >> 5) & 0x7;
        }

        return null;
    }
}
