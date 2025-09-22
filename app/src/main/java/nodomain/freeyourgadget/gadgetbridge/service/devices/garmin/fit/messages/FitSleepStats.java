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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getAwakeTimeScore() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getAwakeningsCountScore() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getDeepSleepScore() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getSleepDurationScore() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getLightSleepScore() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getOverallSleepScore() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getSleepQualityScore() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getSleepRecoveryScore() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getRemSleepScore() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getSleepRestlessnessScore() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getAwakeningsCount() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getUnk12() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Integer getUnk13() {
        return (Integer) getFieldByNumber(13);
    }

    @Nullable
    public Integer getInterruptionsScore() {
        return (Integer) getFieldByNumber(14);
    }

    @Nullable
    public Float getAverageStressDuringSleep() {
        return (Float) getFieldByNumber(15);
    }

    @Nullable
    public Integer getUnk16() {
        return (Integer) getFieldByNumber(16);
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
    }
}
