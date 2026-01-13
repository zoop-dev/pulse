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
public class FitMapLayer extends RecordData {
    public FitMapLayer(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 70) {
            throw new IllegalArgumentException("FitMapLayer expects global messages of " + 70 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getReliefShading() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getOrientation() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUserLocations() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getAutoZoom() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getGuideText() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getTrackLog() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getCourses() {
        return getFieldByNumber(20, Integer.class);
    }

    @Nullable
    public Integer getSpotSoundings() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getLightSectors() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Integer getSegments() {
        return getFieldByNumber(27, Integer.class);
    }

    @Nullable
    public Integer getContours() {
        return getFieldByNumber(28, Integer.class);
    }

    @Nullable
    public Integer getPopularity() {
        return getFieldByNumber(31, Integer.class);
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
            super(70);
        }

        public Builder setReliefShading(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setOrientation(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setUserLocations(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setAutoZoom(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setGuideText(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setTrackLog(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setCourses(final Integer value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setSpotSoundings(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setLightSectors(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setSegments(final Integer value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setContours(final Integer value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setPopularity(final Integer value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitMapLayer build() {
            return (FitMapLayer) super.build();
        }

        @Override
        public FitMapLayer build(final int localMessageType) {
            return (FitMapLayer) super.build(localMessageType);
        }
    }
}
