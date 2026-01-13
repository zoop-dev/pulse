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
public class FitWorkoutStep extends RecordData {
    public FitWorkoutStep(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 27) {
            throw new IllegalArgumentException("FitWorkoutStep expects global messages of " + 27 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getWktStepName() {
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getDurationType() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Long getDurationValue() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Integer getTargetType() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Long getTargetValue() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Long getCustomTargetValueLow() {
        return getFieldByNumber(5, Long.class);
    }

    @Nullable
    public Long getCustomTargetValueHigh() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public Integer getIntensity() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public String getNotes() {
        return getFieldByNumber(8, String.class);
    }

    @Nullable
    public Integer getEquipment() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getExerciseCategory() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getExerciseName() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Float getExerciseWeight() {
        return getFieldByNumber(12, Float.class);
    }

    @Nullable
    public Integer getWeightDisplayUnit() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getSecondaryTargetType() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Long getSecondaryTargetValue() {
        return getFieldByNumber(20, Long.class);
    }

    @Nullable
    public Long getSecondaryCustomTargetValueLow() {
        return getFieldByNumber(21, Long.class);
    }

    @Nullable
    public Long getSecondaryCustomTargetValueHigh() {
        return getFieldByNumber(22, Long.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(27);
        }

        public Builder setWktStepName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDurationType(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setDurationValue(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setTargetType(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTargetValue(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setCustomTargetValueLow(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setCustomTargetValueHigh(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setIntensity(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setNotes(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setEquipment(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setExerciseCategory(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setExerciseName(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setExerciseWeight(final Float value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setWeightDisplayUnit(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setSecondaryTargetType(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setSecondaryTargetValue(final Long value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setSecondaryCustomTargetValueLow(final Long value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setSecondaryCustomTargetValueHigh(final Long value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitWorkoutStep build() {
            return (FitWorkoutStep) super.build();
        }

        @Override
        public FitWorkoutStep build(final int localMessageType) {
            return (FitWorkoutStep) super.build(localMessageType);
        }
    }
}
