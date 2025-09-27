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
public class FitExdDataFieldConfiguration extends RecordData {
    public FitExdDataFieldConfiguration(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 201) {
            throw new IllegalArgumentException("FitExdDataFieldConfiguration expects global messages of " + 201 + ", got " + globalNumber);
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
    public Integer getDisplayType() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public String getTitle() {
        return (String) getFieldByNumber(5);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(201);
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

        public Builder setDisplayType(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTitle(final String value) {
            setFieldByNumber(5, value);
            return this;
        }

        @Override
        public FitExdDataFieldConfiguration build() {
            return (FitExdDataFieldConfiguration) super.build();
        }
    }
}
