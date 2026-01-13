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
public class FitWorkout extends RecordData {
    public FitWorkout(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 26) {
            throw new IllegalArgumentException("FitWorkout expects global messages of " + 26 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Long getCapabilities() {
        return getFieldByNumber(5, Long.class);
    }

    @Nullable
    public Integer getNumValidSteps() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(8, String.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Float getPoolLength() {
        return getFieldByNumber(14, Float.class);
    }

    @Nullable
    public Integer getPoolLengthUnit() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public String getNotes() {
        return getFieldByNumber(17, String.class);
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
            super(26);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setCapabilities(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setNumValidSteps(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setPoolLength(final Float value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setPoolLengthUnit(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setNotes(final String value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitWorkout build() {
            return (FitWorkout) super.build();
        }

        @Override
        public FitWorkout build(final int localMessageType) {
            return (FitWorkout) super.build(localMessageType);
        }
    }
}
