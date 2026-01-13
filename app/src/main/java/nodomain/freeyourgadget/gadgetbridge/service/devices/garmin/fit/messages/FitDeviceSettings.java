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
public class FitDeviceSettings extends RecordData {
    public FitDeviceSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 2) {
            throw new IllegalArgumentException("FitDeviceSettings expects global messages of " + 2 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getActiveTimeZone() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Long getUtcOffset() {
        return getFieldByNumber(1, Long.class);
    }

    @Nullable
    public Number[] getTimeOffset() {
        return getArrayFieldByNumber(2, Number.class);
    }

    @Nullable
    public Number[] getTimeMode() {
        return getArrayFieldByNumber(4, Number.class);
    }

    @Nullable
    public Number[] getTimeZoneOffset() {
        return getArrayFieldByNumber(5, Number.class);
    }

    @Nullable
    public Number[] getAlarmsTime() {
        return getArrayFieldByNumber(8, Number.class);
    }

    @Nullable
    public Number[] getAlarmsUnk5() {
        return getArrayFieldByNumber(9, Number.class);
    }

    @Nullable
    public Integer getBacklightMode() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Number[] getAlarmsEnabled() {
        return getArrayFieldByNumber(28, Number.class);
    }

    @Nullable
    public Integer getActivityTrackerEnabled() {
        return getFieldByNumber(36, Integer.class);
    }

    @Nullable
    public Long getClockTime() {
        return getFieldByNumber(39, Long.class);
    }

    @Nullable
    public Number[] getPagesEnabled() {
        return getArrayFieldByNumber(40, Number.class);
    }

    @Nullable
    public Integer getMoveAlertEnabled() {
        return getFieldByNumber(46, Integer.class);
    }

    @Nullable
    public Integer getDateMode() {
        return getFieldByNumber(47, Integer.class);
    }

    @Nullable
    public Integer getDisplayOrientation() {
        return getFieldByNumber(55, Integer.class);
    }

    @Nullable
    public Integer getMountingSide() {
        return getFieldByNumber(56, Integer.class);
    }

    @Nullable
    public Number[] getDefaultPage() {
        return getArrayFieldByNumber(57, Number.class);
    }

    @Nullable
    public Integer getAutosyncMinSteps() {
        return getFieldByNumber(58, Integer.class);
    }

    @Nullable
    public Integer getAutosyncMinTime() {
        return getFieldByNumber(59, Integer.class);
    }

    @Nullable
    public Integer getLactateThresholdAutodetectEnabled() {
        return getFieldByNumber(80, Integer.class);
    }

    @Nullable
    public Integer getBleAutoUploadEnabled() {
        return getFieldByNumber(86, Integer.class);
    }

    @Nullable
    public Integer getAutoSyncFrequency() {
        return getFieldByNumber(89, Integer.class);
    }

    @Nullable
    public Long getAutoActivityDetect() {
        return getFieldByNumber(90, Long.class);
    }

    @Nullable
    public Number[] getAlarmsRepeat() {
        return getArrayFieldByNumber(92, Number.class);
    }

    @Nullable
    public Integer getNumberOfScreens() {
        return getFieldByNumber(94, Integer.class);
    }

    @Nullable
    public Integer getSmartNotificationDisplayOrientation() {
        return getFieldByNumber(95, Integer.class);
    }

    @Nullable
    public Integer getTapInterface() {
        return getFieldByNumber(134, Integer.class);
    }

    @Nullable
    public Integer getTapSensitivity() {
        return getFieldByNumber(174, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(2);
        }

        public Builder setActiveTimeZone(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setUtcOffset(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setTimeOffset(final Number[] value) {
            setFieldByNumber(2, (Object[]) value);
            return this;
        }

        public Builder setTimeMode(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setTimeZoneOffset(final Number[] value) {
            setFieldByNumber(5, (Object[]) value);
            return this;
        }

        public Builder setAlarmsTime(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setAlarmsUnk5(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setBacklightMode(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAlarmsEnabled(final Number[] value) {
            setFieldByNumber(28, (Object[]) value);
            return this;
        }

        public Builder setActivityTrackerEnabled(final Integer value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setClockTime(final Long value) {
            setFieldByNumber(39, value);
            return this;
        }

        public Builder setPagesEnabled(final Number[] value) {
            setFieldByNumber(40, (Object[]) value);
            return this;
        }

        public Builder setMoveAlertEnabled(final Integer value) {
            setFieldByNumber(46, value);
            return this;
        }

        public Builder setDateMode(final Integer value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setDisplayOrientation(final Integer value) {
            setFieldByNumber(55, value);
            return this;
        }

        public Builder setMountingSide(final Integer value) {
            setFieldByNumber(56, value);
            return this;
        }

        public Builder setDefaultPage(final Number[] value) {
            setFieldByNumber(57, (Object[]) value);
            return this;
        }

        public Builder setAutosyncMinSteps(final Integer value) {
            setFieldByNumber(58, value);
            return this;
        }

        public Builder setAutosyncMinTime(final Integer value) {
            setFieldByNumber(59, value);
            return this;
        }

        public Builder setLactateThresholdAutodetectEnabled(final Integer value) {
            setFieldByNumber(80, value);
            return this;
        }

        public Builder setBleAutoUploadEnabled(final Integer value) {
            setFieldByNumber(86, value);
            return this;
        }

        public Builder setAutoSyncFrequency(final Integer value) {
            setFieldByNumber(89, value);
            return this;
        }

        public Builder setAutoActivityDetect(final Long value) {
            setFieldByNumber(90, value);
            return this;
        }

        public Builder setAlarmsRepeat(final Number[] value) {
            setFieldByNumber(92, (Object[]) value);
            return this;
        }

        public Builder setNumberOfScreens(final Integer value) {
            setFieldByNumber(94, value);
            return this;
        }

        public Builder setSmartNotificationDisplayOrientation(final Integer value) {
            setFieldByNumber(95, value);
            return this;
        }

        public Builder setTapInterface(final Integer value) {
            setFieldByNumber(134, value);
            return this;
        }

        public Builder setTapSensitivity(final Integer value) {
            setFieldByNumber(174, value);
            return this;
        }

        @Override
        public FitDeviceSettings build() {
            return (FitDeviceSettings) super.build();
        }

        @Override
        public FitDeviceSettings build(final int localMessageType) {
            return (FitDeviceSettings) super.build(localMessageType);
        }
    }
}
