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

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 2) {
            throw new IllegalArgumentException("FitDeviceSettings expects native messages of " + 2 + ", got " + nativeNumber);
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
    public Integer getSummerTimeMode() {
        return getFieldByNumber(3, Integer.class);
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
    public Integer getMessageTones() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getBacklightMode() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getBacklightTimeout() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getBacklight() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getContrast() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getGnssMode() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getMapOrientation() {
        return getFieldByNumber(23, Integer.class);
    }

    @Nullable
    public Integer getMapLocations() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getTimezone() {
        return getFieldByNumber(26, Integer.class);
    }

    @Nullable
    public Number[] getAlarmsEnabled() {
        return getArrayFieldByNumber(28, Number.class);
    }

    @Nullable
    public Integer getMapAutoZoom() {
        return getFieldByNumber(30, Integer.class);
    }

    @Nullable
    public Integer getStartOfWeek() {
        return getFieldByNumber(35, Integer.class);
    }

    @Nullable
    public Integer getActivityTrackerEnabled() {
        return getFieldByNumber(36, Integer.class);
    }

    @Nullable
    public Integer getWifiAutoUploadEnabled() {
        return getFieldByNumber(38, Integer.class);
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
    public Integer getAutoMaxHr() {
        return getFieldByNumber(42, Integer.class);
    }

    @Nullable
    public Integer getAutoGoal() {
        return getFieldByNumber(45, Integer.class);
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
    public Integer getOffCourseWarning() {
        return getFieldByNumber(53, Integer.class);
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
    public Integer getCourseSegments() {
        return getFieldByNumber(75, Integer.class);
    }

    @Nullable
    public Integer getMapShowTrack() {
        return getFieldByNumber(76, Integer.class);
    }

    @Nullable
    public Integer getMapTrackColor() {
        return getFieldByNumber(77, Integer.class);
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
    public Integer getGoalNotification() {
        return getFieldByNumber(101, Integer.class);
    }

    @Nullable
    public Integer getMapShowDetails() {
        return getFieldByNumber(117, Integer.class);
    }

    @Nullable
    public Integer getTapInterface() {
        return getFieldByNumber(134, Integer.class);
    }

    @Nullable
    public Integer getActivityTrueUp() {
        return getFieldByNumber(144, Integer.class);
    }

    @Nullable
    public Integer getMapShowContour() {
        return getFieldByNumber(148, Integer.class);
    }

    @Nullable
    public Integer getHrLowAlertLimit() {
        return getFieldByNumber(164, Integer.class);
    }

    @Nullable
    public Integer getTapSensitivity() {
        return getFieldByNumber(174, Integer.class);
    }

    @Nullable
    public Integer getSleepBacklightBrightness() {
        return getFieldByNumber(177, Integer.class);
    }

    @Nullable
    public Integer getSleepBacklightTimeout() {
        return getFieldByNumber(178, Integer.class);
    }

    @Nullable
    public Integer getScreenBrightness() {
        return getFieldByNumber(189, Integer.class);
    }

    @Nullable
    public Integer getTouchEnabled() {
        return getFieldByNumber(192, Integer.class);
    }

    @Nullable
    public Integer getGnssDefaultMode() {
        return getFieldByNumber(217, Integer.class);
    }

    @Nullable
    public Integer getStressAlert() {
        return getFieldByNumber(237, Integer.class);
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

        public Builder setSummerTimeMode(final Integer value) {
            setFieldByNumber(3, value);
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

        public Builder setMessageTones(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setBacklightMode(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setBacklightTimeout(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setBacklight(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setContrast(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setGnssMode(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setMapOrientation(final Integer value) {
            setFieldByNumber(23, value);
            return this;
        }

        public Builder setMapLocations(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setTimezone(final Integer value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setAlarmsEnabled(final Number[] value) {
            setFieldByNumber(28, (Object[]) value);
            return this;
        }

        public Builder setMapAutoZoom(final Integer value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setStartOfWeek(final Integer value) {
            setFieldByNumber(35, value);
            return this;
        }

        public Builder setActivityTrackerEnabled(final Integer value) {
            setFieldByNumber(36, value);
            return this;
        }

        public Builder setWifiAutoUploadEnabled(final Integer value) {
            setFieldByNumber(38, value);
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

        public Builder setAutoMaxHr(final Integer value) {
            setFieldByNumber(42, value);
            return this;
        }

        public Builder setAutoGoal(final Integer value) {
            setFieldByNumber(45, value);
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

        public Builder setOffCourseWarning(final Integer value) {
            setFieldByNumber(53, value);
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

        public Builder setCourseSegments(final Integer value) {
            setFieldByNumber(75, value);
            return this;
        }

        public Builder setMapShowTrack(final Integer value) {
            setFieldByNumber(76, value);
            return this;
        }

        public Builder setMapTrackColor(final Integer value) {
            setFieldByNumber(77, value);
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

        public Builder setGoalNotification(final Integer value) {
            setFieldByNumber(101, value);
            return this;
        }

        public Builder setMapShowDetails(final Integer value) {
            setFieldByNumber(117, value);
            return this;
        }

        public Builder setTapInterface(final Integer value) {
            setFieldByNumber(134, value);
            return this;
        }

        public Builder setActivityTrueUp(final Integer value) {
            setFieldByNumber(144, value);
            return this;
        }

        public Builder setMapShowContour(final Integer value) {
            setFieldByNumber(148, value);
            return this;
        }

        public Builder setHrLowAlertLimit(final Integer value) {
            setFieldByNumber(164, value);
            return this;
        }

        public Builder setTapSensitivity(final Integer value) {
            setFieldByNumber(174, value);
            return this;
        }

        public Builder setSleepBacklightBrightness(final Integer value) {
            setFieldByNumber(177, value);
            return this;
        }

        public Builder setSleepBacklightTimeout(final Integer value) {
            setFieldByNumber(178, value);
            return this;
        }

        public Builder setScreenBrightness(final Integer value) {
            setFieldByNumber(189, value);
            return this;
        }

        public Builder setTouchEnabled(final Integer value) {
            setFieldByNumber(192, value);
            return this;
        }

        public Builder setGnssDefaultMode(final Integer value) {
            setFieldByNumber(217, value);
            return this;
        }

        public Builder setStressAlert(final Integer value) {
            setFieldByNumber(237, value);
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
