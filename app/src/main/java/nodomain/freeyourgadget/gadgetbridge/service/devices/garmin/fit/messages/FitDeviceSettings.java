package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitRecordDataBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
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
    public Long getTimeOffset() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Integer getTimeMode() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getTimeZoneOffset() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Number[] getAlarmsTime() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(8);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Number[] getAlarmsUnk5() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(9);
        if (objectsArray == null)
            return null;
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
        final Object[] objectsArray = (Object[]) getFieldByNumber(28);
        if (objectsArray == null)
            return null;
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
    public Integer getDefaultPage() {
        return (Integer) getFieldByNumber(57);
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
    public Integer getBleAutoUploadEnabled() {
        return (Integer) getFieldByNumber(86);
    }

    @Nullable
    public Long getAutoActivityDetect() {
        return (Long) getFieldByNumber(90);
    }

    @Nullable
    public Number[] getAlarmsRepeat() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(92);
        if (objectsArray == null)
            return null;
        final Number[] ret = new Number[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (Number) objectsArray[i];
        }
        return ret;
    }

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

        public Builder setTimeOffset(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setTimeMode(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setTimeZoneOffset(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setAlarmsTime(final Number[] value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setAlarmsUnk5(final Number[] value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setBacklightMode(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setAlarmsEnabled(final Number[] value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setActivityTrackerEnabled(final Integer value) {
            setFieldByNumber(36, value);
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

        public Builder setDefaultPage(final Integer value) {
            setFieldByNumber(57, value);
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

        public Builder setBleAutoUploadEnabled(final Integer value) {
            setFieldByNumber(86, value);
            return this;
        }

        public Builder setAutoActivityDetect(final Long value) {
            setFieldByNumber(90, value);
            return this;
        }

        public Builder setAlarmsRepeat(final Number[] value) {
            setFieldByNumber(92, value);
            return this;
        }

        @Override
        public FitDeviceSettings build() {
            return (FitDeviceSettings) super.build();
        }
    }
}
