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
public class FitZonesTarget extends RecordData {
    public FitZonesTarget(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 7) {
            throw new IllegalArgumentException("FitZonesTarget expects global messages of " + 7 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getFunctionalThresholdPower() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getMaxHeartRate() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getThresholdHeartRate() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHrCalcType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getPwrCalcType() {
        return (Integer) getFieldByNumber(7);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(7);
        }

        public Builder setFunctionalThresholdPower(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setMaxHeartRate(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setThresholdHeartRate(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHrCalcType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setPwrCalcType(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitZonesTarget build() {
            return (FitZonesTarget) super.build();
        }
    }
}
