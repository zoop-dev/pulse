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
public class FitTimeInZone extends RecordData {
    public FitTimeInZone(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 216) {
            throw new IllegalArgumentException("FitTimeInZone expects global messages of " + 216 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getReferenceMessage() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getReferenceIndex() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Double[] getTimeInZone() {
        final Object object = getFieldByNumber(2);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Double[]{(Double) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Double[] ret = new Double[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Double) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getTimeInSpeedZone() {
        final Object object = getFieldByNumber(3);
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
    public Number[] getTimeInCadenceZone() {
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
    public Number[] getTimeInPowerZone() {
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
    public Integer[] getHrZoneHighBoundary() {
        final Object object = getFieldByNumber(6);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Integer[]{(Integer) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Integer[] ret = new Integer[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Integer) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getSpeedZoneHighBoundary() {
        final Object object = getFieldByNumber(7);
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
    public Number[] getCadenceZoneHighBoundary() {
        final Object object = getFieldByNumber(8);
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
    public Number[] getPowerZoneHighBoundary() {
        final Object object = getFieldByNumber(9);
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
    public Integer getHrCalcType() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getThresholdHeartRate() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getPwrCalcType() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Integer getFunctionalThresholdPower() {
        return (Integer) getFieldByNumber(15);
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
            super(216);
        }

        public Builder setReferenceMessage(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setReferenceIndex(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setTimeInZone(final Double[] value) {
            setFieldByNumber(2, (Object[]) value);
            return this;
        }

        public Builder setTimeInSpeedZone(final Number[] value) {
            setFieldByNumber(3, (Object[]) value);
            return this;
        }

        public Builder setTimeInCadenceZone(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setTimeInPowerZone(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setHrZoneHighBoundary(final Integer[] value) {
            setFieldByNumber(6, (Object[]) value);
            return this;
        }

        public Builder setSpeedZoneHighBoundary(final Number[] value) {
            setFieldByNumber(7, (Object[]) value);
            return this;
        }

        public Builder setCadenceZoneHighBoundary(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setPowerZoneHighBoundary(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setHrCalcType(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setRestingHeartRate(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setThresholdHeartRate(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setPwrCalcType(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setFunctionalThresholdPower(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitTimeInZone build() {
            return (FitTimeInZone) super.build();
        }
    }
}
