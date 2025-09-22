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
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionLanguage.Language;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionMeasurementSystem.Type;

/**
 * WARNING: This class was auto-generated, please avoid modifying it directly.
 * See {@link nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen}
 *
 * @noinspection unused
 */
public class FitUserProfile extends RecordData {
    public FitUserProfile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 3) {
            throw new IllegalArgumentException("FitUserProfile expects global messages of " + 3 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getFriendlyName() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public Integer getGender() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getAge() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHeight() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Float getWeight() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Language getLanguage() {
        return (Language) getFieldByNumber(5);
    }

    @Nullable
    public Type getElevSetting() {
        return (Type) getFieldByNumber(6);
    }

    @Nullable
    public Type getWeightSetting() {
        return (Type) getFieldByNumber(7);
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getDefaultMaxRunningHeartRate() {
        return (Integer) getFieldByNumber(9);
    }

    @Nullable
    public Integer getDefaultMaxBikingHeartRate() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getDefaultMaxHeartRate() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getHrSetting() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Type getSpeedSetting() {
        return (Type) getFieldByNumber(13);
    }

    @Nullable
    public Type getDistSetting() {
        return (Type) getFieldByNumber(14);
    }

    @Nullable
    public Integer getPowerSetting() {
        return (Integer) getFieldByNumber(16);
    }

    @Nullable
    public Integer getActivityClass() {
        return (Integer) getFieldByNumber(17);
    }

    @Nullable
    public Integer getPositionSetting() {
        return (Integer) getFieldByNumber(18);
    }

    @Nullable
    public Type getTemperatureSetting() {
        return (Type) getFieldByNumber(21);
    }

    @Nullable
    public Integer getLocalId() {
        return (Integer) getFieldByNumber(22);
    }

    @Nullable
    public Number[] getGlobalId() {
        final Object object = getFieldByNumber(23);
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
    public Integer getYearOfBirth() {
        return (Integer) getFieldByNumber(24);
    }

    @Nullable
    public Long getWakeTime() {
        return (Long) getFieldByNumber(28);
    }

    @Nullable
    public Long getSleepTime() {
        return (Long) getFieldByNumber(29);
    }

    @Nullable
    public Type getHeightSetting() {
        return (Type) getFieldByNumber(30);
    }

    @Nullable
    public Integer getUserRunningStepLength() {
        return (Integer) getFieldByNumber(31);
    }

    @Nullable
    public Integer getUserWalkingStepLength() {
        return (Integer) getFieldByNumber(32);
    }

    @Nullable
    public Float getLtspeed() {
        return (Float) getFieldByNumber(37);
    }

    @Nullable
    public Long getTimeLastLthrUpdate() {
        return (Long) getFieldByNumber(41);
    }

    @Nullable
    public Type getDepthSetting() {
        return (Type) getFieldByNumber(47);
    }

    @Nullable
    public Long getDiveCount() {
        return (Long) getFieldByNumber(49);
    }

    @Nullable
    public Integer getGenderX() {
        return (Integer) getFieldByNumber(62);
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(254);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(3);
        }

        public Builder setFriendlyName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setGender(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setAge(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHeight(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setWeight(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLanguage(final Language value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setElevSetting(final Type value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setWeightSetting(final Type value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setRestingHeartRate(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setDefaultMaxRunningHeartRate(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setDefaultMaxBikingHeartRate(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setDefaultMaxHeartRate(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setHrSetting(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setSpeedSetting(final Type value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setDistSetting(final Type value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setPowerSetting(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setActivityClass(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setPositionSetting(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setTemperatureSetting(final Type value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setLocalId(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setGlobalId(final Number[] value) {
            setFieldByNumber(23, (Object[]) value);
            return this;
        }

        public Builder setYearOfBirth(final Integer value) {
            setFieldByNumber(24, value);
            return this;
        }

        public Builder setWakeTime(final Long value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setSleepTime(final Long value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setHeightSetting(final Type value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setUserRunningStepLength(final Integer value) {
            setFieldByNumber(31, value);
            return this;
        }

        public Builder setUserWalkingStepLength(final Integer value) {
            setFieldByNumber(32, value);
            return this;
        }

        public Builder setLtspeed(final Float value) {
            setFieldByNumber(37, value);
            return this;
        }

        public Builder setTimeLastLthrUpdate(final Long value) {
            setFieldByNumber(41, value);
            return this;
        }

        public Builder setDepthSetting(final Type value) {
            setFieldByNumber(47, value);
            return this;
        }

        public Builder setDiveCount(final Long value) {
            setFieldByNumber(49, value);
            return this;
        }

        public Builder setGenderX(final Integer value) {
            setFieldByNumber(62, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitUserProfile build() {
            return (FitUserProfile) super.build();
        }
    }
}
