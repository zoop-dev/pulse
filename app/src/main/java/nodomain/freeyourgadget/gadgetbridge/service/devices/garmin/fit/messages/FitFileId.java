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

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType.FILETYPE;
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
public class FitFileId extends RecordData {
    public FitFileId(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 0) {
            throw new IllegalArgumentException("FitFileId expects global messages of " + 0 + ", got " + globalNumber);
        }
    }

    @Nullable
    public FILETYPE getType() {
        return (FILETYPE) getFieldByNumber(0);
    }

    @Nullable
    public Integer getManufacturer() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getProduct() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getSerialNumber() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Long getTimeCreated() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Integer getNumber() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getManufacturerPartner() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public String getProductName() {
        return (String) getFieldByNumber(8);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(0);
        }

        public Builder setType(final FILETYPE value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setManufacturer(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setProduct(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSerialNumber(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTimeCreated(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setNumber(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setManufacturerPartner(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setProductName(final String value) {
            setFieldByNumber(8, value);
            return this;
        }

        @Override
        public FitFileId build() {
            return (FitFileId) super.build();
        }
    }
}
