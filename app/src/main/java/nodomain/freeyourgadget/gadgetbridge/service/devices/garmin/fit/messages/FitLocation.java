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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLocationSymbol.LocationSymbol;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitLocation extends RecordData {
    public FitLocation(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 29) {
            throw new IllegalArgumentException("FitLocation expects global messages of " + 29 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public Double getPositionLat() {
        return (Double) getFieldByNumber(1);
    }

    @Nullable
    public Double getPositionLong() {
        return (Double) getFieldByNumber(2);
    }

    @Nullable
    public LocationSymbol getSymbol() {
        return (LocationSymbol) getFieldByNumber(3);
    }

    @Nullable
    public Float getAltitude() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Integer getEnhancedAltitude() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public String getDescription() {
        return (String) getFieldByNumber(6);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
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
            super(29);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setPositionLat(final Double value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setPositionLong(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSymbol(final LocationSymbol value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setAltitude(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setEnhancedAltitude(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setDescription(final String value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitLocation build() {
            return (FitLocation) super.build();
        }
    }
}
