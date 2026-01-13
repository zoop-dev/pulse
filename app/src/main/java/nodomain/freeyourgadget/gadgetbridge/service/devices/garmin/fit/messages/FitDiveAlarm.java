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
public class FitDiveAlarm extends RecordData {
    public FitDiveAlarm(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 262) {
            throw new IllegalArgumentException("FitDiveAlarm expects global messages of " + 262 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getDepth() {
        return getFieldByNumber(0, Double.class);
    }

    @Nullable
    public Long getTime() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Boolean getEnabled() {
        return getFieldByNumber(2, Boolean.class);
    }

    @Nullable
    public Integer getAlarmType() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getSound() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Number[] getDiveTypes() {
        return getArrayFieldByNumber(5, Number.class);
    }

    @Nullable
    public Long getId() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public Boolean getPopupEnabled() {
        return getFieldByNumber(7, Boolean.class);
    }

    @Nullable
    public Boolean getTriggerOnDescent() {
        return getFieldByNumber(8, Boolean.class);
    }

    @Nullable
    public Boolean getTriggerOnAscent() {
        return getFieldByNumber(9, Boolean.class);
    }

    @Nullable
    public Boolean getRepeating() {
        return getFieldByNumber(10, Boolean.class);
    }

    @Nullable
    public Double getSpeed() {
        return getFieldByNumber(11, Double.class);
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
            super(262);
        }

        public Builder setDepth(final Double value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTime(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setEnabled(final Boolean value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setAlarmType(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setSound(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setDiveTypes(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setId(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setPopupEnabled(final Boolean value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setTriggerOnDescent(final Boolean value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setTriggerOnAscent(final Boolean value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setRepeating(final Boolean value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setSpeed(final Double value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitDiveAlarm build() {
            return (FitDiveAlarm) super.build();
        }

        @Override
        public FitDiveAlarm build(final int localMessageType) {
            return (FitDiveAlarm) super.build(localMessageType);
        }
    }
}
