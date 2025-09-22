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
public class FitVideo extends RecordData {
    public FitVideo(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 184) {
            throw new IllegalArgumentException("FitVideo expects global messages of " + 184 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getUrl() {
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public String getHostingProvider() {
        return (String) getFieldByNumber(1);
    }

    @Nullable
    public Long getDuration() {
        return (Long) getFieldByNumber(2);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(184);
        }

        public Builder setUrl(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setHostingProvider(final String value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setDuration(final Long value) {
            setFieldByNumber(2, value);
            return this;
        }

        @Override
        public FitVideo build() {
            return (FitVideo) super.build();
        }
    }
}
