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
public class FitEnduranceScore extends RecordData {
    public FitEnduranceScore(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 403) {
            throw new IllegalArgumentException("FitEnduranceScore expects global messages of " + 403 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getEnduranceScore() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getLevel() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getUnknown2() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundIntermediate() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundTrained() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundWellTrained() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundExpert() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundSuperior() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getLowerBoundElite() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getUnknown9() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getUnknown10() {
        return getFieldByNumber(10, Integer.class);
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
            super(403);
        }

        public Builder setEnduranceScore(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setLevel(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setUnknown2(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setLowerBoundIntermediate(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setLowerBoundTrained(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLowerBoundWellTrained(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setLowerBoundExpert(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setLowerBoundSuperior(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setLowerBoundElite(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setUnknown9(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setUnknown10(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitEnduranceScore build() {
            return (FitEnduranceScore) super.build();
        }

        @Override
        public FitEnduranceScore build(final int localMessageType) {
            return (FitEnduranceScore) super.build(localMessageType);
        }
    }
}
