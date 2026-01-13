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
public class FitConnectivity extends RecordData {
    public FitConnectivity(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 127) {
            throw new IllegalArgumentException("FitConnectivity expects global messages of " + 127 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getBluetoothEnabled() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getBluetoothLeEnabled() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getAntEnabled() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(3, String.class);
    }

    @Nullable
    public Integer getLiveTrackingEnabled() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getWeatherConditionsEnabled() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getWeatherAlertsEnabled() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getAutoActivityUploadEnabled() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getCourseDownloadEnabled() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getWorkoutDownloadEnabled() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getGpsEphemerisDownloadEnabled() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getIncidentDetectionEnabled() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getGrouptrackEnabled() {
        return getFieldByNumber(12, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(127);
        }

        public Builder setBluetoothEnabled(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setBluetoothLeEnabled(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAntEnabled(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setName(final String value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setLiveTrackingEnabled(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setWeatherConditionsEnabled(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setWeatherAlertsEnabled(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setAutoActivityUploadEnabled(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setCourseDownloadEnabled(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setWorkoutDownloadEnabled(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setGpsEphemerisDownloadEnabled(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setIncidentDetectionEnabled(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setGrouptrackEnabled(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        @Override
        public FitConnectivity build() {
            return (FitConnectivity) super.build();
        }

        @Override
        public FitConnectivity build(final int localMessageType) {
            return (FitConnectivity) super.build(localMessageType);
        }
    }
}
