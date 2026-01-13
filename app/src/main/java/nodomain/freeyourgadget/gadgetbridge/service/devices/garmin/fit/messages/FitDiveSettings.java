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
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getModel() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getGfLow() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getGfHigh() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getWaterType() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Float getWaterDensity() {
        return getFieldByNumber(5, Float.class);
    }

    @Nullable
    public Float getPo2Warn() {
        return getFieldByNumber(6, Float.class);
    }

    @Nullable
    public Float getPo2Critical() {
        return getFieldByNumber(7, Float.class);
    }

    @Nullable
    public Float getPo2Deco() {
        return getFieldByNumber(8, Float.class);
    }

    @Nullable
    public Integer getSafetyStopEnabled() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Float getBottomDepth() {
        return getFieldByNumber(10, Float.class);
    }

    @Nullable
    public Long getBottomTime() {
        return getFieldByNumber(11, Long.class);
    }

    @Nullable
    public Integer getApneaCountdownEnabled() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Long getApneaCountdownTime() {
        return getFieldByNumber(13, Long.class);
    }

    @Nullable
    public Integer getBacklightMode() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getBacklightBrightness() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getBacklightTimeout() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getRepeatDiveInterval() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getSafetyStopTime() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getHeartRateSourceType() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Integer getHeartRateSource() {
        return getFieldByNumber(20, Integer.class);
    }

    @Nullable
    public Integer getTravelGas() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getCcrLowSetpointSwitchMode() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Float getCcrLowSetpoint() {
        return getFieldByNumber(23, Float.class);
    }

    @Nullable
    public Double getCcrLowSetpointDepth() {
        return getFieldByNumber(24, Double.class);
    }

    @Nullable
    public Integer getCcrHighSetpointSwitchMode() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Float getCcrHighSetpoint() {
        return getFieldByNumber(26, Float.class);
    }

    @Nullable
    public Double getCcrHighSetpointDepth() {
        return getFieldByNumber(27, Double.class);
    }

    @Nullable
    public Integer getGasConsumptionDisplay() {
        return getFieldByNumber(29, Integer.class);
    }

    @Nullable
    public Integer getUpKeyEnabled() {
        return getFieldByNumber(30, Integer.class);
    }

    @Nullable
    public Integer getDiveSounds() {
        return getFieldByNumber(35, Integer.class);
    }

    @Nullable
    public Float getLastStopMultiple() {
        return getFieldByNumber(36, Float.class);
    }

    @Nullable
    public Integer getNoFlyTimeMode() {
        return getFieldByNumber(37, Integer.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
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

        @Override
        public FitDiveSettings build(final int localMessageType) {
            return (FitDiveSettings) super.build(localMessageType);
        }
    }
}
