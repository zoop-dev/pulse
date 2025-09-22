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
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getCapabilities() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getNumValidSteps() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Float getPoolLength() {
        return (Float) getFieldByNumber(14);
    }

    @Nullable
    public Integer getPoolLengthUnit() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public String getNotes() {
        return (String) getFieldByNumber(17);
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
    }
}
