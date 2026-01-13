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
public class FitCourse extends RecordData {
    public FitCourse(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 31) {
            throw new IllegalArgumentException("FitCourse expects global messages of " + 31 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(5, String.class);
    }

    @Nullable
    public Long getCapabilities() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(7, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(31);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setCapabilities(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitCourse build() {
            return (FitCourse) super.build();
        }

        @Override
        public FitCourse build(final int localMessageType) {
            return (FitCourse) super.build(localMessageType);
        }
    }
}
