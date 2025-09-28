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
public class FitThreeDSensorCalibration extends RecordData {
    public FitThreeDSensorCalibration(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 167) {
            throw new IllegalArgumentException("FitThreeDSensorCalibration expects global messages of " + 167 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSensorType() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getCalibrationFactor() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Long getCalibrationDivisor() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Long getLevelShift() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Number[] getOffsetCal() {
        final Object object = getFieldByNumber(4);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getOrientationMatrix() {
        final Object object = getFieldByNumber(5);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
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
            super(167);
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

        public Builder setOffsetCal(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setOrientationMatrix(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitThreeDSensorCalibration build() {
            return (FitThreeDSensorCalibration) super.build();
        }
    }
}
