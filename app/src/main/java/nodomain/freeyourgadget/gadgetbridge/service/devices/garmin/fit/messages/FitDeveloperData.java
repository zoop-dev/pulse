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
public class FitDeveloperData extends RecordData {
    public FitDeveloperData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 207) {
            throw new IllegalArgumentException("FitDeveloperData expects global messages of " + 207 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Number[] getDeveloperId() {
        return getArrayFieldByNumber(0, Number.class);
    }

    @Nullable
    public Number[] getApplicationId() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Integer getManufacturerId() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getDeveloperDataIndex() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Long getApplicationVersion() {
        return getFieldByNumber(4, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(207);
        }

        public Builder setDeveloperId(final Number[] value) {
            setFieldByNumber(0, (Object[]) value);
            return this;
        }

        public Builder setApplicationId(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setManufacturerId(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setDeveloperDataIndex(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setApplicationVersion(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        @Override
        public FitDeveloperData build() {
            return (FitDeveloperData) super.build();
        }

        @Override
        public FitDeveloperData build(final int localMessageType) {
            return (FitDeveloperData) super.build(localMessageType);
        }
    }
}
