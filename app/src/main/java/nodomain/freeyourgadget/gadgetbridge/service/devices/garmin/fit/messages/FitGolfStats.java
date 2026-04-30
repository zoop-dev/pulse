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
public class FitGolfStats extends RecordData {
    public FitGolfStats(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 191) {
            throw new IllegalArgumentException("FitGolfStats expects native messages of " + 191 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getOut() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getIn() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getTotal() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getFairwayHit() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getGir() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getPutts() {
        return getFieldByNumber(9, Integer.class);
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
            super(191);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setOut(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setIn(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setTotal(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setFairwayHit(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setGir(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setPutts(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitGolfStats build() {
            return (FitGolfStats) super.build();
        }

        @Override
        public FitGolfStats build(final int localMessageType) {
            return (FitGolfStats) super.build(localMessageType);
        }
    }
}
