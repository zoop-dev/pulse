package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
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
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }

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
