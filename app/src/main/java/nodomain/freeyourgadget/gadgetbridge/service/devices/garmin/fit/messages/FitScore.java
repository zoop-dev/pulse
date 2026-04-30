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
public class FitScore extends RecordData {
    public FitScore(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 192) {
            throw new IllegalArgumentException("FitScore expects native messages of " + 192 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getHoleNumber() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getScore() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getPutts() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getFairway() {
        return getFieldByNumber(6, Integer.class);
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
            super(192);
        }

        public Builder setHoleNumber(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setScore(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setPutts(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setFairway(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitScore build() {
            return (FitScore) super.build();
        }

        @Override
        public FitScore build(final int localMessageType) {
            return (FitScore) super.build(localMessageType);
        }
    }
}
