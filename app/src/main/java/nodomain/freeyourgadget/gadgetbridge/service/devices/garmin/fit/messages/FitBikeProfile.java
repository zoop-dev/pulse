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
public class FitBikeProfile extends RecordData {
    public FitBikeProfile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 6) {
            throw new IllegalArgumentException("FitBikeProfile expects global messages of " + 6 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Double getOdometer() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Integer getBikeSpdAntId() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getBikeCadAntId() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getBikeSpdcadAntId() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getBikePowerAntId() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Float getCustomWheelsize() {
        return (Float) getFieldByNumber(8);
    }

    @Nullable
    public Float getAutoWheelsize() {
        return (Float) getFieldByNumber(9);
    }

    @Nullable
    public Float getBikeWeight() {
        return (Float) getFieldByNumber(10);
    }

    @Nullable
    public Float getPowerCalFactor() {
        return (Float) getFieldByNumber(11);
    }

    @Nullable
    public Boolean getAutoWheelCal() {
        return (Boolean) getFieldByNumber(12);
    }

    @Nullable
    public Boolean getAutoPowerZero() {
        return (Boolean) getFieldByNumber(13);
    }

    @Nullable
    public Integer getId() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Boolean getSpdEnabled() {
        return (Boolean) getFieldByNumber(15);
    }

    @Nullable
    public Boolean getCadEnabled() {
        return (Boolean) getFieldByNumber(16);
    }

    @Nullable
    public Boolean getSpdcadEnabled() {
        return (Boolean) getFieldByNumber(17);
    }

    @Nullable
    public Boolean getPowerEnabled() {
        return (Boolean) getFieldByNumber(18);
    }

    @Nullable
    public Float getCrankLength() {
        return (Float) getFieldByNumber(19);
    }

    @Nullable
    public Boolean getEnabled() {
        return (Boolean) getFieldByNumber(20);
    }

    @Nullable
    public Integer getBikeSpdAntIdTransType() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getBikeCadAntIdTransType() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Integer getBikeSpdcadAntIdTransType() {
        return (Integer) getFieldByNumber(23);
    }

    @Nullable
    public Integer getBikePowerAntIdTransType() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Integer getOdometerRollover() {
        return (Integer) getFieldByNumber(37);
    }

    @Nullable
    public Integer getFrontGearNum() {
        return (Integer) getFieldByNumber(38);
    }

    @Nullable
    public Number[] getFrontGear() {
        final Object object = getFieldByNumber(39);
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
    public Integer getRearGearNum() {
        return (Integer) getFieldByNumber(40);
    }

    @Nullable
    public Number[] getRearGear() {
        final Object object = getFieldByNumber(41);
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
    public Boolean getShimanoDi2Enabled() {
        return (Boolean) getFieldByNumber(44);
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
            super(6);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setOdometer(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setBikeSpdAntId(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setBikeCadAntId(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setBikeSpdcadAntId(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setBikePowerAntId(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setCustomWheelsize(final Float value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setAutoWheelsize(final Float value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setBikeWeight(final Float value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setPowerCalFactor(final Float value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setAutoWheelCal(final Boolean value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAutoPowerZero(final Boolean value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setId(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setSpdEnabled(final Boolean value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setCadEnabled(final Boolean value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setSpdcadEnabled(final Boolean value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setPowerEnabled(final Boolean value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setCrankLength(final Float value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setEnabled(final Boolean value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setBikeSpdAntIdTransType(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setBikeCadAntIdTransType(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setBikeSpdcadAntIdTransType(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setBikePowerAntIdTransType(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setOdometerRollover(final Integer value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setFrontGearNum(final Integer value) {
            setFieldByNumber(38, value);
            return this;
        }

        public Builder setFrontGear(final Number[] value) {
            setFieldByNumber(39, (Object[]) value);
            return this;
        }

        public Builder setRearGearNum(final Integer value) {
            setFieldByNumber(40, value);
            return this;
        }

        public Builder setRearGear(final Number[] value) {
            setFieldByNumber(41, (Object[]) value);
            return this;
        }

        public Builder setShimanoDi2Enabled(final Boolean value) {
            setFieldByNumber(44, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitBikeProfile build() {
            return (FitBikeProfile) super.build();
        }
    }
}
