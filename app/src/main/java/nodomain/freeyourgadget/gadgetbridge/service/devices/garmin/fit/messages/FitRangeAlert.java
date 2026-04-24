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
public class FitRangeAlert extends RecordData {
    public FitRangeAlert(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 17) {
            throw new IllegalArgumentException("FitRangeAlert expects native messages of " + 17 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getMetric() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getLowStatus() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getLowValue() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getHighStatus() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getHighValue() {
        return getFieldByNumber(5, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(17);
        }

        public Builder setMetric(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLowStatus(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setLowValue(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setHighStatus(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setHighValue(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        @Override
        public FitRangeAlert build() {
            return (FitRangeAlert) super.build();
        }

        @Override
        public FitRangeAlert build(final int localMessageType) {
            return (FitRangeAlert) super.build(localMessageType);
        }
    }
}
