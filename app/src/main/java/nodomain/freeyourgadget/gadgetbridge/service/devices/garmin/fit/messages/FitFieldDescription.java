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
public class FitFieldDescription extends RecordData {
    public FitFieldDescription(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 206) {
            throw new IllegalArgumentException("FitFieldDescription expects global messages of " + 206 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getDeveloperDataIndex() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getFieldDefinitionNumber() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getFitBaseTypeId() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public String getFieldName() {
        return getFieldByNumber(3, String.class);
    }

    @Nullable
    public Integer getArray() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public String getComponents() {
        return getFieldByNumber(5, String.class);
    }

    @Nullable
    public Integer getScale() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getOffset() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public String getUnits() {
        return getFieldByNumber(8, String.class);
    }

    @Nullable
    public String getBits() {
        return getFieldByNumber(9, String.class);
    }

    @Nullable
    public String getAccumulate() {
        return getFieldByNumber(10, String.class);
    }

    @Nullable
    public Integer getFitBaseUnitId() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getNativeMesgNum() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getNativeFieldNum() {
        return getFieldByNumber(15, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(206);
        }

        public Builder setDeveloperDataIndex(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setFieldDefinitionNumber(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setFitBaseTypeId(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setFieldName(final String value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setArray(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setComponents(final String value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setScale(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setOffset(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setUnits(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setBits(final String value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setAccumulate(final String value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setFitBaseUnitId(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setNativeMesgNum(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setNativeFieldNum(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        @Override
        public FitFieldDescription build() {
            return (FitFieldDescription) super.build();
        }

        @Override
        public FitFieldDescription build(final int localMessageType) {
            return (FitFieldDescription) super.build(localMessageType);
        }
    }
}
