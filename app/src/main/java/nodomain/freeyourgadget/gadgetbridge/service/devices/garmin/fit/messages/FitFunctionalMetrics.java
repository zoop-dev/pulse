/*  Copyright (C) 2026 Freeyourgadget

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
public class FitFunctionalMetrics extends RecordData {
    public FitFunctionalMetrics(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 356) {
            throw new IllegalArgumentException("FitFunctionalMetrics expects global messages of " + 356 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getUnknown0() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Long getUnknown2() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Long getUnknown3() {
        return getFieldByNumber(3, Long.class);
    }

    @Nullable
    public Integer getFunctionalThresholdPower() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getUnknown5() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getUnknown6() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getRunningLactateThresholdPower() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getRunningLactateThresholdHr() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getCyclingLactaceThresholdHr() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getUnknown10() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getUnknown11() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUnknown12() {
        return getFieldByNumber(12, Integer.class);
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
            super(356);
        }

        public Builder setUnknown0(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setUnknown2(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setUnknown3(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setFunctionalThresholdPower(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setUnknown5(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setUnknown6(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setRunningLactateThresholdPower(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setRunningLactateThresholdHr(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setCyclingLactaceThresholdHr(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setUnknown10(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setUnknown11(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setUnknown12(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitFunctionalMetrics build() {
            return (FitFunctionalMetrics) super.build();
        }

        @Override
        public FitFunctionalMetrics build(final int localMessageType) {
            return (FitFunctionalMetrics) super.build(localMessageType);
        }
    }
}
