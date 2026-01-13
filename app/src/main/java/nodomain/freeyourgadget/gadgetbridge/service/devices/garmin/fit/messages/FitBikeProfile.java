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
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Double getOdometer() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Integer getBikeSpdAntId() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getBikeCadAntId() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getBikeSpdcadAntId() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getBikePowerAntId() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Float getCustomWheelsize() {
        return getFieldByNumber(8, Float.class);
    }

    @Nullable
    public Float getAutoWheelsize() {
        return getFieldByNumber(9, Float.class);
    }

    @Nullable
    public Float getBikeWeight() {
        return getFieldByNumber(10, Float.class);
    }

    @Nullable
    public Float getPowerCalFactor() {
        return getFieldByNumber(11, Float.class);
    }

    @Nullable
    public Boolean getAutoWheelCal() {
        return getFieldByNumber(12, Boolean.class);
    }

    @Nullable
    public Boolean getAutoPowerZero() {
        return getFieldByNumber(13, Boolean.class);
    }

    @Nullable
    public Integer getId() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Boolean getSpdEnabled() {
        return getFieldByNumber(15, Boolean.class);
    }

    @Nullable
    public Boolean getCadEnabled() {
        return getFieldByNumber(16, Boolean.class);
    }

    @Nullable
    public Boolean getSpdcadEnabled() {
        return getFieldByNumber(17, Boolean.class);
    }

    @Nullable
    public Boolean getPowerEnabled() {
        return getFieldByNumber(18, Boolean.class);
    }

    @Nullable
    public Float getCrankLength() {
        return getFieldByNumber(19, Float.class);
    }

    @Nullable
    public Boolean getEnabled() {
        return getFieldByNumber(20, Boolean.class);
    }

    @Nullable
    public Integer getBikeSpdAntIdTransType() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getBikeCadAntIdTransType() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Integer getBikeSpdcadAntIdTransType() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getBikePowerAntIdTransType() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Integer getOdometerRollover() {
        return getFieldByNumber(37, Integer.class);
    }

    @Nullable
    public Integer getFrontGearNum() {
        return getFieldByNumber(38, Integer.class);
    }

    @Nullable
    public Number[] getFrontGear() {
        return getArrayFieldByNumber(39, Number.class);
    }

    @Nullable
    public Integer getRearGearNum() {
        return getFieldByNumber(40, Integer.class);
    }

    @Nullable
    public Number[] getRearGear() {
        return getArrayFieldByNumber(41, Number.class);
    }

    @Nullable
    public Boolean getShimanoDi2Enabled() {
        return getFieldByNumber(44, Boolean.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
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

        @Override
        public FitBikeProfile build(final int localMessageType) {
            return (FitBikeProfile) super.build(localMessageType);
        }
    }
}
