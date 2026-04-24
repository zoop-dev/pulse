/*  Copyright (C) 2026 Freeyourgadget

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
public class FitConnectIqField extends RecordData {
    public FitConnectIqField(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 170) {
            throw new IllegalArgumentException("FitConnectIqField expects native messages of " + 170 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Number[] getAppId() {
        return getArrayFieldByNumber(1, Number.class);
    }

    @Nullable
    public Long getDataField() {
        return getFieldByNumber(2, Long.class);
    }

    @Nullable
    public Integer getScreenId() {
        return getFieldByNumber(100, Integer.class);
    }

    @Nullable
    public Integer getFieldBits() {
        return getFieldByNumber(101, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(170);
        }

        public Builder setAppId(final Number[] value) {
            setFieldByNumber(1, (Object[]) value);
            return this;
        }

        public Builder setDataField(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setScreenId(final Integer value) {
            setFieldByNumber(100, value);
            return this;
        }

        public Builder setFieldBits(final Integer value) {
            setFieldByNumber(101, value);
            return this;
        }

        @Override
        public FitConnectIqField build() {
            return (FitConnectIqField) super.build();
        }

        @Override
        public FitConnectIqField build(final int localMessageType) {
            return (FitConnectIqField) super.build(localMessageType);
        }
    }
}
