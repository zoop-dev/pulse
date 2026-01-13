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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory.ExerciseCategory;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitSet extends RecordData {
    public FitSet(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 225) {
            throw new IllegalArgumentException("FitSet expects global messages of " + 225 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getDuration() {
        return getFieldByNumber(0, Double.class);
    }

    @Nullable
    public Integer getRepetitions() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Float getWeight() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Integer getSetType() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Long getStartTime() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public ExerciseCategory[] getCategory() {
        return getArrayFieldByNumber(7, ExerciseCategory.class);
    }

    @Nullable
    public Number[] getCategorySubtype() {
        return getArrayFieldByNumber(8, Number.class);
    }

    @Nullable
    public Integer getWeightDisplayUnit() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(254, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(225);
        }

        public Builder setDuration(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setRepetitions(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setWeight(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSetType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setStartTime(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setCategory(final ExerciseCategory[] value) {
            setFieldByNumber(7, (Object[]) value);
            return this;
        }

        public Builder setCategorySubtype(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setWeightDisplayUnit(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setWktStepIndex(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSet build() {
            return (FitSet) super.build();
        }

        @Override
        public FitSet build(final int localMessageType) {
            return (FitSet) super.build(localMessageType);
        }
    }
}
