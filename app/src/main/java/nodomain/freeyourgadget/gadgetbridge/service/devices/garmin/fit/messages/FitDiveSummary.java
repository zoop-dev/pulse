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
public class FitDiveSummary extends RecordData {
    public FitDiveSummary(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 268) {
            throw new IllegalArgumentException("FitDiveSummary expects global messages of " + 268 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getReferenceMesg() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getReferenceIndex() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Double getAvgDepth() {
        return (Double) getFieldByNumber(2);
    }

    @Nullable
    public Double getMaxDepth() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Long getSurfaceInterval() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Integer getStartCns() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getEndCns() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getStartN2() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getEndN2() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getO2Toxicity() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Long getDiveNumber() {
        return (Long) getFieldByNumber(10);
    }

    @Nullable
    public Double getBottomTime() {
        return (Double) getFieldByNumber(11);
    }

    @Nullable
    public Float getAvgPressureSac() {
        return (Float) getFieldByNumber(12);
    }

    @Nullable
    public Float getAvgVolumeSac() {
        return (Float) getFieldByNumber(13);
    }

    @Nullable
    public Float getAvgRmv() {
        return (Float) getFieldByNumber(14);
    }

    @Nullable
    public Double getDescentTime() {
        return (Double) getFieldByNumber(15);
    }

    @Nullable
    public Double getAscentTime() {
        return (Double) getFieldByNumber(16);
    }

    @Nullable
    public Double getAvgAscentRate() {
        return (Double) getFieldByNumber(17);
    }

    @Nullable
    public Double getAvgDescentRate() {
        return (Double) getFieldByNumber(22);
    }

    @Nullable
    public Double getMaxAscentRate() {
        return (Double) getFieldByNumber(23);
    }

    @Nullable
    public Double getMaxDescentRate() {
        return (Double) getFieldByNumber(24);
    }

    @Nullable
    public Double getHangTime() {
        return (Double) getFieldByNumber(25);
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
            super(268);
        }

        public Builder setReferenceMesg(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setReferenceIndex(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAvgDepth(final Double value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setMaxDepth(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setSurfaceInterval(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setStartCns(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setEndCns(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setStartN2(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setEndN2(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setO2Toxicity(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setDiveNumber(final Long value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setBottomTime(final Double value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setAvgPressureSac(final Float value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAvgVolumeSac(final Float value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setAvgRmv(final Float value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setDescentTime(final Double value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setAscentTime(final Double value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setAvgAscentRate(final Double value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setAvgDescentRate(final Double value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setMaxAscentRate(final Double value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setMaxDescentRate(final Double value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setHangTime(final Double value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitDiveSummary build() {
            return (FitDiveSummary) super.build();
        }
    }
}
