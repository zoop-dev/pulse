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
public class FitMtbCx extends RecordData {
    public FitMtbCx(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 309) {
            throw new IllegalArgumentException("FitMtbCx expects native messages of " + 309 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getGritFlowJumpRecording() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getJumpAlerts() {
        return getFieldByNumber(2, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(309);
        }

        public Builder setGritFlowJumpRecording(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setJumpAlerts(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        @Override
        public FitMtbCx build() {
            return (FitMtbCx) super.build();
        }

        @Override
        public FitMtbCx build(final int localMessageType) {
            return (FitMtbCx) super.build(localMessageType);
        }
    }
}
