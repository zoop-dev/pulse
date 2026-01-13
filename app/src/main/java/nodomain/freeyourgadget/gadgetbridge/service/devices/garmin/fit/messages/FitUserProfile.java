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
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getGender() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getAge() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getHeight() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Float getWeight() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Language getLanguage() {
        return getFieldByNumber(5, Language.class);
    }

    @Nullable
    public Type getElevSetting() {
        return getFieldByNumber(6, Type.class);
    }

    @Nullable
    public Type getWeightSetting() {
        return getFieldByNumber(7, Type.class);
    }

    @Nullable
    public Integer getRestingHeartRate() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getDefaultMaxRunningHeartRate() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getDefaultMaxBikingHeartRate() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getDefaultMaxHeartRate() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getHrSetting() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Type getSpeedSetting() {
        return getFieldByNumber(13, Type.class);
    }

    @Nullable
    public Type getDistSetting() {
        return getFieldByNumber(14, Type.class);
    }

    @Nullable
    public Integer getPowerSetting() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getActivityClass() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getPositionSetting() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Type getTemperatureSetting() {
        return getFieldByNumber(21, Type.class);
    }

    @Nullable
    public Integer getLocalId() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Number[] getGlobalId() {
        return getArrayFieldByNumber(23, Number.class);
    }

    @Nullable
    public Integer getYearOfBirth() {
        return getFieldByNumber(24, Integer.class);
    }

    @Nullable
    public Long getWakeTime() {
        return getFieldByNumber(28, Long.class);
    }

    @Nullable
    public Long getSleepTime() {
        return getFieldByNumber(29, Long.class);
    }

    @Nullable
    public Type getHeightSetting() {
        return getFieldByNumber(30, Type.class);
    }

    @Nullable
    public Integer getUserRunningStepLength() {
        return getFieldByNumber(31, Integer.class);
    }

    @Nullable
    public Integer getUserWalkingStepLength() {
        return getFieldByNumber(32, Integer.class);
    }

    @Nullable
    public Float getLtspeed() {
        return getFieldByNumber(37, Float.class);
    }

    @Nullable
    public Long getTimeLastLthrUpdate() {
        return getFieldByNumber(41, Long.class);
    }

    @Nullable
    public Type getDepthSetting() {
        return getFieldByNumber(47, Type.class);
    }

    @Nullable
    public Long getDiveCount() {
        return getFieldByNumber(49, Long.class);
    }

    @Nullable
    public Integer getGenderX() {
        return getFieldByNumber(62, Integer.class);
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

        @Override
        public FitUserProfile build(final int localMessageType) {
            return (FitUserProfile) super.build(localMessageType);
        }
    }
}
