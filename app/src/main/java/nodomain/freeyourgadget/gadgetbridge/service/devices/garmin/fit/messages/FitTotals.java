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
public class FitTotals extends RecordData {
    public FitTotals(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 33) {
            throw new IllegalArgumentException("FitTotals expects global messages of " + 33 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getTimerTime() {
        return getFieldByNumber(0, Long.class);
    }

    @Nullable
    public Long getDistance() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Long getCalories() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Long getElapsedTime() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Integer getSessions() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Long getActiveTime() {
        return getFieldByNumber(6, Long.class);
    }

    @Nullable
    public Integer getSportIndex() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public String getActivityProfile() {
        return getFieldByNumber(10, String.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    @Nullable
    public Integer getMessageIndex() {
        return getFieldByNumber(254, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(33);
        }

        public Builder setTimerTime(final Long value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setDistance(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setCalories(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setElapsedTime(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSessions(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setActiveTime(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setSportIndex(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setActivityProfile(final String value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitTotals build() {
            return (FitTotals) super.build();
        }

        @Override
        public FitTotals build(final int localMessageType) {
            return (FitTotals) super.build(localMessageType);
        }
    }
}
