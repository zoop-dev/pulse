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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getFieldDefinitionNumber() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getFitBaseTypeId() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public String getFieldName() {
        return (String) getFieldByNumber(3);
    }

    @Nullable
    public Integer getArray() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public String getComponents() {
        return (String) getFieldByNumber(5);
    }

    @Nullable
    public Integer getScale() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getOffset() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public String getUnits() {
        return (String) getFieldByNumber(8);
    }

    @Nullable
    public String getBits() {
        return (String) getFieldByNumber(9);
    }

    @Nullable
    public String getAccumulate() {
        return (String) getFieldByNumber(10);
    }

    @Nullable
    public Integer getFitBaseUnitId() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getNativeMesgNum() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Integer getNativeFieldNum() {
        return (Integer) getFieldByNumber(15);
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
    }
}
