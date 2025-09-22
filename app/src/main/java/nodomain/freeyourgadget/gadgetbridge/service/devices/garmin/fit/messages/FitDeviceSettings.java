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
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getUtcOffset() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Number[] getTimeOffset() {
        final Object object = getFieldByNumber(2);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getTimeMode() {
        final Object object = getFieldByNumber(4);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getTimeZoneOffset() {
        final Object object = getFieldByNumber(5);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAlarmsTime() {
        final Object object = getFieldByNumber(8);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAlarmsUnk5() {
        final Object object = getFieldByNumber(9);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getBacklightMode() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Number[] getAlarmsEnabled() {
        final Object object = getFieldByNumber(28);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getActivityTrackerEnabled() {
        return (Integer) getFieldByNumber(36);
    }

    @Nullable
    public Long getClockTime() {
        return (Long) getFieldByNumber(39);
    }

    @Nullable
    public Number[] getPagesEnabled() {
        final Object object = getFieldByNumber(40);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getMoveAlertEnabled() {
        return (Integer) getFieldByNumber(46);
    }

    @Nullable
    public Integer getDateMode() {
        return (Integer) getFieldByNumber(47);
    }

    @Nullable
    public Integer getDisplayOrientation() {
        return (Integer) getFieldByNumber(55);
    }

    @Nullable
    public Integer getMountingSide() {
        return (Integer) getFieldByNumber(56);
    }

    @Nullable
    public Number[] getDefaultPage() {
        final Object object = getFieldByNumber(57);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getAutosyncMinSteps() {
        return (Integer) getFieldByNumber(58);
    }

    @Nullable
    public Integer getAutosyncMinTime() {
        return (Integer) getFieldByNumber(59);
    }

    @Nullable
    public Integer getLactateThresholdAutodetectEnabled() {
        return (Integer) getFieldByNumber(80);
    }

    @Nullable
    public Integer getBleAutoUploadEnabled() {
        return (Integer) getFieldByNumber(86);
    }

    @Nullable
    public Integer getAutoSyncFrequency() {
        return (Integer) getFieldByNumber(89);
    }

    @Nullable
    public Long getAutoActivityDetect() {
        return (Long) getFieldByNumber(90);
    }

    @Nullable
    public Number[] getAlarmsRepeat() {
        final Object object = getFieldByNumber(92);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new Number[]{(Number) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getNumberOfScreens() {
        return (Integer) getFieldByNumber(94);
    }

    @Nullable
    public Integer getSmartNotificationDisplayOrientation() {
        return (Integer) getFieldByNumber(95);
    }

    @Nullable
    public Integer getTapInterface() {
        return (Integer) getFieldByNumber(134);
    }

    @Nullable
    public Integer getTapSensitivity() {
        return (Integer) getFieldByNumber(174);
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
    }
}
