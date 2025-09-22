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
public class FitDiveSettings extends RecordData {
    public FitDiveSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 258) {
            throw new IllegalArgumentException("FitDiveSettings expects global messages of " + 258 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public Integer getModel() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getGfLow() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getGfHigh() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getWaterType() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Float getWaterDensity() {
        return (Float) getFieldByNumber(5);
    }

    @Nullable
    public Float getPo2Warn() {
        return (Float) getFieldByNumber(6);
    }

    @Nullable
    public Float getPo2Critical() {
        return (Float) getFieldByNumber(7);
    }

    @Nullable
    public Float getPo2Deco() {
        return (Float) getFieldByNumber(8);
    }

    @Nullable
    public Integer getSafetyStopEnabled() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Float getBottomDepth() {
        return (Float) getFieldByNumber(10);
    }

    @Nullable
    public Long getBottomTime() {
        return (Long) getFieldByNumber(11);
    }

    @Nullable
    public Integer getApneaCountdownEnabled() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Long getApneaCountdownTime() {
        return (Long) getFieldByNumber(13);
    }

    @Nullable
    public Integer getBacklightMode() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Integer getBacklightBrightness() {
        return (Integer) getFieldByNumber(15);
    }

    @Nullable
    public Integer getBacklightTimeout() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getRepeatDiveInterval() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getSafetyStopTime() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Integer getHeartRateSourceType() {
        return (Integer) getFieldByNumber(19);
    }

    @Nullable
    public Integer getHeartRateSource() {
        return (Integer) getFieldByNumber(20);
    }

    @Nullable
    public Integer getTravelGas() {
        return (Integer) getFieldByNumber(21);
    }

    @Nullable
    public Integer getCcrLowSetpointSwitchMode() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Float getCcrLowSetpoint() {
        return (Float) getFieldByNumber(23);
    }

    @Nullable
    public Double getCcrLowSetpointDepth() {
        return (Double) getFieldByNumber(24);
    }

    @Nullable
    public Integer getCcrHighSetpointSwitchMode() {
        return (Integer) getFieldByNumber(25);
    }

    @Nullable
    public Float getCcrHighSetpoint() {
        return (Float) getFieldByNumber(26);
    }

    @Nullable
    public Double getCcrHighSetpointDepth() {
        return (Double) getFieldByNumber(27);
    }

    @Nullable
    public Integer getGasConsumptionDisplay() {
        return (Integer) getFieldByNumber(29);
    }

    @Nullable
    public Integer getUpKeyEnabled() {
        return (Integer) getFieldByNumber(30);
    }

    @Nullable
    public Integer getDiveSounds() {
        return (Integer) getFieldByNumber(35);
    }

    @Nullable
    public Float getLastStopMultiple() {
        return (Float) getFieldByNumber(36);
    }

    @Nullable
    public Integer getNoFlyTimeMode() {
        return (Integer) getFieldByNumber(37);
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
            super(258);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setModel(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setGfLow(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setGfHigh(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setWaterType(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setWaterDensity(final Float value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setPo2Warn(final Float value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setPo2Critical(final Float value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setPo2Deco(final Float value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setSafetyStopEnabled(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setBottomDepth(final Float value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setBottomTime(final Long value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setApneaCountdownEnabled(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setApneaCountdownTime(final Long value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setBacklightMode(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setBacklightBrightness(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setBacklightTimeout(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setRepeatDiveInterval(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setSafetyStopTime(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setHeartRateSourceType(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setHeartRateSource(final Integer value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setTravelGas(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setCcrLowSetpointSwitchMode(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setCcrLowSetpoint(final Float value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setCcrLowSetpointDepth(final Double value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setCcrHighSetpointSwitchMode(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setCcrHighSetpoint(final Float value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setCcrHighSetpointDepth(final Double value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setGasConsumptionDisplay(final Integer value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setUpKeyEnabled(final Integer value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setDiveSounds(final Integer value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setLastStopMultiple(final Float value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setNoFlyTimeMode(final Integer value) {
            setFieldByNumber(37, value);
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
        public FitDiveSettings build() {
            return (FitDiveSettings) super.build();
        }
    }
}
