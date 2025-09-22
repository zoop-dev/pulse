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
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Integer getRepetitions() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Float getWeight() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSetType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(6);
    }

    @Nullable
    public ExerciseCategory[] getCategory() {
        final Object object = getFieldByNumber(7);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new ExerciseCategory[]{(ExerciseCategory) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final ExerciseCategory[] ret = new ExerciseCategory[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (ExerciseCategory) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getCategorySubtype() {
        final Object object = getFieldByNumber(8);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getWeightDisplayUnit() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getWktStepIndex() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(254);
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
    }
}
