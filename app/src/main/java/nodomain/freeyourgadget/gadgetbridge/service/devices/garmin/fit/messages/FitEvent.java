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
public class FitEvent extends RecordData {
    public FitEvent(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 21) {
            throw new IllegalArgumentException("FitEvent expects global messages of " + 21 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getEvent() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getEventType() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getData16() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Long getData() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public Integer getEventGroup() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getScore() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getOpponentScore() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getFrontGearNum() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getFrontGear() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getRearGearNum() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getRearGear() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getDeviceIndex() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getActivityType() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Long getStartTimestamp() {
        return getFieldByNumber(15, Long.class);
    }

    @Nullable
    public Integer getRadarThreatLevelMax() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getRadarThreatCount() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Float getRadarThreatAvgApproachSpeed() {
        return getFieldByNumber(23, Float.class);
    }

    @Nullable
    public Float getRadarThreatMaxApproachSpeed() {
        return getFieldByNumber(24, Float.class);
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
            super(21);
        }

        public Builder setEvent(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setEventType(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setData16(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setData(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setEventGroup(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setScore(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setOpponentScore(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setFrontGearNum(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setFrontGear(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setRearGearNum(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setRearGear(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setDeviceIndex(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setActivityType(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setStartTimestamp(final Long value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setRadarThreatLevelMax(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setRadarThreatCount(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setRadarThreatAvgApproachSpeed(final Float value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setRadarThreatMaxApproachSpeed(final Float value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitEvent build() {
            return (FitEvent) super.build();
        }

        @Override
        public FitEvent build(final int localMessageType) {
            return (FitEvent) super.build(localMessageType);
        }
    }
}
