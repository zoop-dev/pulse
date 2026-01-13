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
public class FitEcgSummary extends RecordData {
    public FitEcgSummary(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 336) {
            throw new IllegalArgumentException("FitEcgSummary expects global messages of " + 336 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getUnknown0() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getUnknown1() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Float getUnknown2() {
        return getFieldByNumber(2, Float.class);
    }

    @Nullable
    public Float getUnknown3() {
        return getFieldByNumber(3, Float.class);
    }

    @Nullable
    public Long getEcgTimestamp() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(5, Long.class);
    }

    @Nullable
    public Integer getUnknown6() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Float getAverageHeartRate() {
        return getFieldByNumber(7, Float.class);
    }

    @Nullable
    public String getUnknown10() {
        return getFieldByNumber(10, String.class);
    }

    @Nullable
    public Integer getUnknown11() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUnknown12() {
        return getFieldByNumber(12, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(336);
        }

        public Builder setUnknown0(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setUnknown1(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setUnknown2(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setUnknown3(final Float value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setEcgTimestamp(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setUnknown6(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setAverageHeartRate(final Float value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setUnknown10(final String value) {
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

        @Override
        public FitEcgSummary build() {
            return (FitEcgSummary) super.build();
        }

        @Override
        public FitEcgSummary build(final int localMessageType) {
            return (FitEcgSummary) super.build(localMessageType);
        }
    }
}
