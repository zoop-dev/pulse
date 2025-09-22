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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalSource.Source;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionGoalType.Type;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitGoals extends RecordData {
    public FitGoals(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 15) {
            throw new IllegalArgumentException("FitGoals expects global messages of " + 15 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getStartDate() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Long getEndDate() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Type getType() {
        return (Type) getFieldByNumber(4);
    }

    @Nullable
    public Long getValue() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getRepeat() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Long getTargetValue() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Integer getRecurrence() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getRecurrenceValue() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getEnabled() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Source getSource() {
        return (Source) getFieldByNumber(11);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(15);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStartDate(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setEndDate(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setType(final Type value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setValue(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setRepeat(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTargetValue(final Long value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setRecurrence(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setRecurrenceValue(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setEnabled(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setSource(final Source value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitGoals build() {
            return (FitGoals) super.build();
        }
    }
}
