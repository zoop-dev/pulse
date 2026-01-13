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
public class FitOneDSensorCalibration extends RecordData {
    public FitOneDSensorCalibration(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 210) {
            throw new IllegalArgumentException("FitOneDSensorCalibration expects global messages of " + 210 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSensorType() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Long getCalibrationFactor() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Long getCalibrationDivisor() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Long getLevelShift() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public Long getOffsetCal() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(210);
        }

        public Builder setSensorType(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setCalibrationFactor(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setCalibrationDivisor(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setLevelShift(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setOffsetCal(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitOneDSensorCalibration build() {
            return (FitOneDSensorCalibration) super.build();
        }

        @Override
        public FitOneDSensorCalibration build(final int localMessageType) {
            return (FitOneDSensorCalibration) super.build(localMessageType);
        }
    }
}
