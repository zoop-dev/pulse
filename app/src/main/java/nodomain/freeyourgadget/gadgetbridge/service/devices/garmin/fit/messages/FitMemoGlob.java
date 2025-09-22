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
public class FitMemoGlob extends RecordData {
    public FitMemoGlob(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 145) {
            throw new IllegalArgumentException("FitMemoGlob expects global messages of " + 145 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Number[] getMemo() {
        final Object object = getFieldByNumber(0);
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
    public Integer getMesgNum() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getParentIndex() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getFieldNum() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Number[] getData() {
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
    public Long getPartIndex() {
        return (Long) getFieldByNumber(250);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(145);
        }

        public Builder setMemo(final Number[] value) {
            setFieldByNumber(0, (Object[]) value);
            return this;
        }

        public Builder setMesgNum(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setParentIndex(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setFieldNum(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setData(final Number[] value) {
            setFieldByNumber(4, (Object[]) value);
            return this;
        }

        public Builder setPartIndex(final Long value) {
            setFieldByNumber(250, value);
            return this;
        }

        @Override
        public FitMemoGlob build() {
            return (FitMemoGlob) super.build();
        }
    }
}
