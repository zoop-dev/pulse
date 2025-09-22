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
public class FitBloodPressure extends RecordData {
    public FitBloodPressure(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 51) {
            throw new IllegalArgumentException("FitBloodPressure expects global messages of " + 51 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getSystolicPressure() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getDiastolicPressure() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getMeanArterialPressure() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getMap3SampleMean() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getMapMorningValues() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getMapEveningValues() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getHeartRate() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getHeartRateType() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getStatus() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getUserProfileIndex() {
        return (Integer) getFieldByNumber(9);
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
            super(51);
        }

        public Builder setSystolicPressure(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDiastolicPressure(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setMeanArterialPressure(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setMap3SampleMean(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setMapMorningValues(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMapEveningValues(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setHeartRate(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setHeartRateType(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setStatus(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setUserProfileIndex(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitBloodPressure build() {
            return (FitBloodPressure) super.build();
        }
    }
}
