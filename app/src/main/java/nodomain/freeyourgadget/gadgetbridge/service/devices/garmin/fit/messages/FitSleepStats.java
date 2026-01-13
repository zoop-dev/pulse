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
public class FitSleepStats extends RecordData {
    public FitSleepStats(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 346) {
            throw new IllegalArgumentException("FitSleepStats expects global messages of " + 346 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getCombinedAwakeScore() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getAwakeTimeScore() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getAwakeningsCountScore() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getDeepSleepScore() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getSleepDurationScore() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getLightSleepScore() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getOverallSleepScore() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getSleepQualityScore() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getSleepRecoveryScore() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getRemSleepScore() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getSleepRestlessnessScore() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getAwakeningsCount() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUnk12() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getUnk13() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getInterruptionsScore() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Float getAverageStressDuringSleep() {
        return getFieldByNumber(15, Float.class);
    }

    @Nullable
    public Integer getUnk16() {
        return getFieldByNumber(16, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(346);
        }

        public Builder setCombinedAwakeScore(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setAwakeTimeScore(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAwakeningsCountScore(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setDeepSleepScore(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setSleepDurationScore(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLightSleepScore(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setOverallSleepScore(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setSleepQualityScore(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setSleepRecoveryScore(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setRemSleepScore(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setSleepRestlessnessScore(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setAwakeningsCount(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setUnk12(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setUnk13(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setInterruptionsScore(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setAverageStressDuringSleep(final Float value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setUnk16(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        @Override
        public FitSleepStats build() {
            return (FitSleepStats) super.build();
        }

        @Override
        public FitSleepStats build(final int localMessageType) {
            return (FitSleepStats) super.build(localMessageType);
        }
    }
}
