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
public class FitSleepRestlessMoments extends RecordData {
    public FitSleepRestlessMoments(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 382) {
            throw new IllegalArgumentException("FitSleepRestlessMoments expects global messages of " + 382 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getUnknown0() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public Integer getRestlessMomentsCount() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Number[] getUnknown2() {
        return getArrayFieldByNumber(2, Number.class);
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
            super(382);
        }

        public Builder setUnknown0(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setRestlessMomentsCount(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setUnknown2(final Number[] value) {
            setFieldByNumber(2, (Object[]) value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitSleepRestlessMoments build() {
            return (FitSleepRestlessMoments) super.build();
        }

        @Override
        public FitSleepRestlessMoments build(final int localMessageType) {
            return (FitSleepRestlessMoments) super.build(localMessageType);
        }
    }
}
