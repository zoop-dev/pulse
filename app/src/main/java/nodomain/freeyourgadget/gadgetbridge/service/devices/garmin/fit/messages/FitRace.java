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
public class FitRace extends RecordData {
    public FitRace(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 310) {
            throw new IllegalArgumentException("FitRace expects native messages of " + 310 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Double getGoalTime() {
        return getFieldByNumber(3, Double.class);
    }

    @Nullable
    public Float getGoalSpeed() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Double getDistance() {
        return getFieldByNumber(5, Double.class);
    }

    @Nullable
    public Double getSplitDistance() {
        return getFieldByNumber(7, Double.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(310);
        }

        public Builder setGoalTime(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setGoalSpeed(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setDistance(final Double value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setSplitDistance(final Double value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitRace build() {
            return (FitRace) super.build();
        }

        @Override
        public FitRace build(final int localMessageType) {
            return (FitRace) super.build(localMessageType);
        }
    }
}
