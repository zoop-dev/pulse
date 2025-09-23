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
public class FitSegmentFile extends RecordData {
    public FitSegmentFile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 151) {
            throw new IllegalArgumentException("FitSegmentFile expects global messages of " + 151 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getFileUuid() {
        return (String) getFieldByNumber(1);
    }

    @Nullable
    public Integer getEnabled() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Long getUserProfilePrimaryKey() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Number[] getLeaderType() {
        final Object object = getFieldByNumber(7);
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
    public Number[] getLeaderGroupPrimaryKey() {
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
    public Number[] getLeaderActivityId() {
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
    public String[] getLeaderActivityIdString() {
        final Object object = getFieldByNumber(10);
        if (object == null)
            return null;
        if (!object.getClass().isArray()) {
            return new String[]{(String) object};
        }
        final Object[] objectsArray = (Object[]) object;
        final String[] ret = new String[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (String) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getDefaultRaceLeader() {
        return (Integer) getFieldByNumber(11);
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
            super(151);
        }

        public Builder setFileUuid(final String value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setEnabled(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setUserProfilePrimaryKey(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setLeaderType(final Number[] value) {
            setFieldByNumber(7, (Object[]) value);
            return this;
        }

        public Builder setLeaderGroupPrimaryKey(final Number[] value) {
            setFieldByNumber(8, (Object[]) value);
            return this;
        }

        public Builder setLeaderActivityId(final Number[] value) {
            setFieldByNumber(9, (Object[]) value);
            return this;
        }

        public Builder setLeaderActivityIdString(final String[] value) {
            setFieldByNumber(10, (Object[]) value);
            return this;
        }

        public Builder setDefaultRaceLeader(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSegmentFile build() {
            return (FitSegmentFile) super.build();
        }
    }
}
