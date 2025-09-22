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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getBluetoothLeEnabled() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getAntEnabled() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public String getName() {
        return (String) getFieldByNumber(3);
    }

    @Nullable
    public Integer getLiveTrackingEnabled() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getWeatherConditionsEnabled() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getWeatherAlertsEnabled() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getAutoActivityUploadEnabled() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getCourseDownloadEnabled() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getWorkoutDownloadEnabled() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getGpsEphemerisDownloadEnabled() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getIncidentDetectionEnabled() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getGrouptrackEnabled() {
        return (Integer) getFieldByNumber(12);
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
    }
}
