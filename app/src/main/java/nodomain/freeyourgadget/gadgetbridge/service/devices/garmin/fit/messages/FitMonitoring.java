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
    public Long getDistance() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Long getCycles() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Long getActiveTime() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Integer getActivityType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getActiveCalories() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Integer getDurationMin() {
        return (Integer) getFieldByNumber(29);
    }

    @Nullable
    public Integer getCurrentActivityTypeIntensity() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Integer getTimestamp16() {
        return (Integer) getFieldByNumber(26);
    }

    @Nullable
    public Integer getHeartRate() {
        return (Integer) getFieldByNumber(27);
    }

    @Nullable
    public Integer getModerateActivityMinutes() {
        return (Integer) getFieldByNumber(33);
    }

    @Nullable
    public Integer getVigorousActivityMinutes() {
        return (Integer) getFieldByNumber(34);
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
            super(55);
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

        public Builder setHeartRate(final Integer value) {
            setFieldByNumber(27, value);
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
