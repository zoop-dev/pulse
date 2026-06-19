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
public class FitSport extends RecordData {
    public FitSport(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 12) {
            throw new IllegalArgumentException("FitSport expects native messages of " + 12 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getSubSport() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(3, String.class);
    }

    @Nullable
    public Integer getColor() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getPopularityRouting() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getNavigationPrompt() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getSharpBendWarnings() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getWorkoutVideos() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getHighTrafficRoadWarnings() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Integer getRoadHazardWarnings() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getUnpavedRoadWarnings() {
        return getFieldByNumber(24, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(12);
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSubSport(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setColor(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setPopularityRouting(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setNavigationPrompt(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setSharpBendWarnings(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setWorkoutVideos(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setHighTrafficRoadWarnings(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setRoadHazardWarnings(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setUnpavedRoadWarnings(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        @Override
        public FitSport build() {
            return (FitSport) super.build();
        }

        @Override
        public FitSport build(final int localMessageType) {
            return (FitSport) super.build(localMessageType);
        }
    }
}
