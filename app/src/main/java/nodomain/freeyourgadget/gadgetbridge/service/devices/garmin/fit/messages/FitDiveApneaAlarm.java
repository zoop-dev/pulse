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
public class FitDiveApneaAlarm extends RecordData {
    public FitDiveApneaAlarm(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 393) {
            throw new IllegalArgumentException("FitDiveApneaAlarm expects global messages of " + 393 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getDepth() {
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Long getTime() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Boolean getEnabled() {
        return (Boolean) getFieldByNumber(2);
    }

    @Nullable
    public Integer getAlarmType() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getSound() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Number[] getDiveTypes() {
        final Object object = getFieldByNumber(5);
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
    public Long getId() {
        return (Long) getFieldByNumber(6);
    }

    @Nullable
    public Boolean getPopupEnabled() {
        return (Boolean) getFieldByNumber(7);
    }

    @Nullable
    public Boolean getTriggerOnDescent() {
        return (Boolean) getFieldByNumber(8);
    }

    @Nullable
    public Boolean getTriggerOnAscent() {
        return (Boolean) getFieldByNumber(9);
    }

    @Nullable
    public Boolean getRepeating() {
        return (Boolean) getFieldByNumber(10);
    }

    @Nullable
    public Double getSpeed() {
        return (Double) getFieldByNumber(11);
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
            super(393);
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
        public FitDiveApneaAlarm build() {
            return (FitDiveApneaAlarm) super.build();
        }
    }
}
