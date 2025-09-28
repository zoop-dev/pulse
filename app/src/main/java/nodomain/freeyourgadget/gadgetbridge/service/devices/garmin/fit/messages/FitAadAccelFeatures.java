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
public class FitAadAccelFeatures extends RecordData {
    public FitAadAccelFeatures(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 289) {
            throw new IllegalArgumentException("FitAadAccelFeatures expects global messages of " + 289 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTime() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getEnergyTotal() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Integer getZeroCrossCnt() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getInstance() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getTimeAboveThreshold() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(289);
        }

        public Builder setTime(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setEnergyTotal(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setZeroCrossCnt(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setInstance(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTimeAboveThreshold(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitAadAccelFeatures build() {
            return (FitAadAccelFeatures) super.build();
        }
    }
}
