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
public class FitAviationAttitude extends RecordData {
    public FitAviationAttitude(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 178) {
            throw new IllegalArgumentException("FitAviationAttitude expects global messages of " + 178 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTimestampMs() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Number[] getSystemTime() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Number[] getPitch() {
        return getArrayFieldByNumber(2, Number.class);
    }

    @Nullable
    public Number[] getRoll() {
        return getArrayFieldByNumber(3, Number.class);
    }

    @Nullable
    public Number[] getAccelLateral() {
        return getArrayFieldByNumber(4, Number.class);
    }

    @Nullable
    public Number[] getAccelNormal() {
        return getArrayFieldByNumber(5, Number.class);
    }

    @Nullable
    public Number[] getTurnRate() {
        return getArrayFieldByNumber(6, Number.class);
    }

    @Nullable
    public Number[] getStage() {
        return getArrayFieldByNumber(7, Number.class);
    }

    @Nullable
    public Number[] getAttitudeStageComplete() {
        return getArrayFieldByNumber(8, Number.class);
    }

    @Nullable
    public Number[] getTrack() {
        return getArrayFieldByNumber(9, Number.class);
    }

    @Nullable
    public Number[] getValidity() {
        return getArrayFieldByNumber(10, Number.class);
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
            super(178);
        }

        public Builder setTimestampMs(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSystemTime(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setPitch(final Number[] value) {
            setFieldByNumber(2, (Object[]) value);
            return this;
        }

        public Builder setRoll(final Number[] value) {
            setFieldByNumber(3, (Object[]) value);
            return this;
        }

        public Builder setAccelLateral(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setAccelNormal(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setTurnRate(final Number[] value) {
            setFieldByNumber(6, (Object[]) value);
            return this;
        }

        public Builder setStage(final Number[] value) {
            setFieldByNumber(7, (Object[]) value);
            return this;
        }

        public Builder setAttitudeStageComplete(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setTrack(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setValidity(final Number[] value) {
            setFieldByNumber(10, (Object[]) value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitAviationAttitude build() {
            return (FitAviationAttitude) super.build();
        }

        @Override
        public FitAviationAttitude build(final int localMessageType) {
            return (FitAviationAttitude) super.build(localMessageType);
        }
    }
}
