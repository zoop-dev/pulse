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

import java.time.LocalTime;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarmLabel.Label;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitAlarmSettings extends RecordData {
    public FitAlarmSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 222) {
            throw new IllegalArgumentException("FitAlarmSettings expects global messages of " + 222 + ", got " + globalNumber);
        }
    }

    @Nullable
    public LocalTime getTime() {
        return getFieldByNumber(0, LocalTime.class);
    }

    @Nullable
    public Long getRepeat() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Integer getEnabled() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getSound() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getBacklight() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Long getSomeTimestamp() {
        return getFieldByNumber(5, Long.class);
    }

    @Nullable
    public Integer getUnknown7() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Label getLabel() {
        return getFieldByNumber(8, Label.class);
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
            super(222);
        }

        public Builder setTime(final LocalTime value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setRepeat(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setEnabled(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSound(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setBacklight(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSomeTimestamp(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setUnknown7(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setLabel(final Label value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitAlarmSettings build() {
            return (FitAlarmSettings) super.build();
        }

        @Override
        public FitAlarmSettings build(final int localMessageType) {
            return (FitAlarmSettings) super.build(localMessageType);
        }
    }
}
