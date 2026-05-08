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
public class FitClubs extends RecordData {
    public FitClubs(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 173) {
            throw new IllegalArgumentException("FitClubs expects native messages of " + 173 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Double getAverageDistance() {
        return getFieldByNumber(6, Double.class);
    }

    @Nullable
    public Double getMaxDistance() {
        return getFieldByNumber(19, Double.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(173);
        }

        public Builder setAverageDistance(final Double value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setMaxDistance(final Double value) {
            setFieldByNumber(19, value);
            return this;
        }

        @Override
        public FitClubs build() {
            return (FitClubs) super.build();
        }

        @Override
        public FitClubs build(final int localMessageType) {
            return (FitClubs) super.build(localMessageType);
        }
    }
}
