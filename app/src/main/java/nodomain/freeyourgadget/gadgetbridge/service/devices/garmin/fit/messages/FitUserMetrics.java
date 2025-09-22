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
public class FitUserMetrics extends RecordData {
    public FitUserMetrics(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 79) {
            throw new IllegalArgumentException("FitUserMetrics expects global messages of " + 79 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getVo2Max() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getAge() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Float getHeight() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Float getWeight() {
        return (Float) getFieldByNumber(3);
    }

    @Nullable
    public Integer getGender() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getMaxHr() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getRemainingRecoveryTime() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getInitialBodyBattery() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public Long getStartOfActivity() {
        return (Long) getFieldByNumber(16);
    }

    @Nullable
    public Integer getBeginningPotential() {
        return (Integer) getFieldByNumber(32);
    }

    @Nullable
    public Long getEndOfPreviousActivity() {
        return (Long) getFieldByNumber(35);
    }

    @Nullable
    public Long getWakeUpTime() {
        return (Long) getFieldByNumber(39);
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
            super(79);
        }

        public Builder setVo2Max(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setAge(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setHeight(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setWeight(final Float value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setGender(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMaxHr(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setRemainingRecoveryTime(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setInitialBodyBattery(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setStartOfActivity(final Long value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setBeginningPotential(final Integer value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setEndOfPreviousActivity(final Long value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setWakeUpTime(final Long value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitUserMetrics build() {
            return (FitUserMetrics) super.build();
        }
    }
}
