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
public class FitMesgCapabilities extends RecordData {
    public FitMesgCapabilities(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 38) {
            throw new IllegalArgumentException("FitMesgCapabilities expects global messages of " + 38 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getFile() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getMesgNum() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getCountType() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getMaxCount() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getCount() {
        return getFieldByNumber(4, Integer.class);
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
            super(38);
        }

        public Builder setFile(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setMesgNum(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setCountType(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setMaxCount(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setCount(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitMesgCapabilities build() {
            return (FitMesgCapabilities) super.build();
        }

        @Override
        public FitMesgCapabilities build(final int localMessageType) {
            return (FitMesgCapabilities) super.build(localMessageType);
        }
    }
}
