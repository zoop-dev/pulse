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
public class FitVideoClip extends RecordData {
    public FitVideoClip(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 187) {
            throw new IllegalArgumentException("FitVideoClip expects global messages of " + 187 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getClipNumber() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getStartTimestamp() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Integer getStartTimestampMs() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Long getEndTimestamp() {
        return (Long) getFieldByNumber(3);
    }

    @Nullable
    public Integer getEndTimestampMs() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getClipStart() {
        return (Long) getFieldByNumber(6);
    }

    @Nullable
    public Long getClipEnd() {
        return (Long) getFieldByNumber(7);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(187);
        }

        public Builder setClipNumber(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setStartTimestamp(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setStartTimestampMs(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setEndTimestamp(final Long value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setEndTimestampMs(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setClipStart(final Long value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setClipEnd(final Long value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitVideoClip build() {
            return (FitVideoClip) super.build();
        }
    }
}
