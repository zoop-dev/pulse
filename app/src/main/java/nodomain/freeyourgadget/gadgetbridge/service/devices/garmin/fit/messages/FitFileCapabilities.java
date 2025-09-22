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
public class FitFileCapabilities extends RecordData {
    public FitFileCapabilities(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 37) {
            throw new IllegalArgumentException("FitFileCapabilities expects global messages of " + 37 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getType() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Integer getFlags() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public String getDirectory() {
        return (String) getFieldByNumber(2);
    }

    @Nullable
    public Integer getMaxCount() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Long getMaxSize() {
        return (Long) getFieldByNumber(4);
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
            super(37);
        }

        public Builder setType(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setFlags(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setDirectory(final String value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setMaxCount(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setMaxSize(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitFileCapabilities build() {
            return (FitFileCapabilities) super.build();
        }
    }
}
