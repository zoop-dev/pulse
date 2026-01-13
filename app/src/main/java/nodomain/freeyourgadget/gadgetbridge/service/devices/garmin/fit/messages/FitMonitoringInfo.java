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
public class FitMonitoringInfo extends RecordData {
    public FitMonitoringInfo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 103) {
            throw new IllegalArgumentException("FitMonitoringInfo expects global messages of " + 103 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public Number[] getActivityType() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Number[] getStepsToDistance() {
        return getArrayFieldByNumber(3, Number.class);
    }

    @Nullable
    public Number[] getStepsToCalories() {
        return getArrayFieldByNumber(4, Number.class);
    }

    @Nullable
    public Integer getRestingMetabolicRate() {
        return getFieldByNumber(5, Integer.class);
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
            super(103);
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setActivityType(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setStepsToDistance(final Number[] value) {
            setFieldByNumber(3, (Object[]) value);
            return this;
        }

        public Builder setStepsToCalories(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setRestingMetabolicRate(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitMonitoringInfo build() {
            return (FitMonitoringInfo) super.build();
        }

        @Override
        public FitMonitoringInfo build(final int localMessageType) {
            return (FitMonitoringInfo) super.build(localMessageType);
        }
    }
}
