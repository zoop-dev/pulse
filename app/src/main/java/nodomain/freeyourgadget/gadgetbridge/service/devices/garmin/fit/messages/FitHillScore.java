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
public class FitHillScore extends RecordData {
    public FitHillScore(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 402) {
            throw new IllegalArgumentException("FitHillScore expects global messages of " + 402 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getHillScore() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getHillStrength() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getHillEndurance() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getUnknown3() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getUnknown4() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getUnknown5() {
        return getFieldByNumber(5, Integer.class);
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
            super(402);
        }

        public Builder setHillScore(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setHillStrength(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setHillEndurance(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setUnknown3(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setUnknown4(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setUnknown5(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitHillScore build() {
            return (FitHillScore) super.build();
        }

        @Override
        public FitHillScore build(final int localMessageType) {
            return (FitHillScore) super.build(localMessageType);
        }
    }
}
