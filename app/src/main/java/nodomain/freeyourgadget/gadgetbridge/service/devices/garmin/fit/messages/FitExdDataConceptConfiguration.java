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
public class FitExdDataConceptConfiguration extends RecordData {
    public FitExdDataConceptConfiguration(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 202) {
            throw new IllegalArgumentException("FitExdDataConceptConfiguration expects global messages of " + 202 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getScreenIndex() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getConceptField() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getFieldId() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getConceptCount() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getDataPage() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getConceptKey() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getScaling() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getDataUnits() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getQualifier() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getDescriptor() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Boolean getIsSigned() {
        return (Boolean) getFieldByNumber(11);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(202);
        }

        public Builder setScreenIndex(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setConceptField(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setFieldId(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setConceptCount(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setDataPage(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setConceptKey(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setScaling(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setDataUnits(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setQualifier(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setDescriptor(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setIsSigned(final Boolean value) {
            setFieldByNumber(11, value);
            return this;
        }

        @Override
        public FitExdDataConceptConfiguration build() {
            return (FitExdDataConceptConfiguration) super.build();
        }
    }
}
