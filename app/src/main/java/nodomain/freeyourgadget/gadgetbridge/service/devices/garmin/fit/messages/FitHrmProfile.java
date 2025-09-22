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
public class FitHrmProfile extends RecordData {
    public FitHrmProfile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 4) {
            throw new IllegalArgumentException("FitHrmProfile expects global messages of " + 4 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Boolean getEnabled() {
        return (Boolean) getFieldByNumber(0);
    }

    @Nullable
    public Integer getHrmAntId() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getLogHrv() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getHrmAntIdTransType() {
        return (Integer) getFieldByNumber(3);
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
            super(4);
        }

        public Builder setEnabled(final Boolean value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setHrmAntId(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLogHrv(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setHrmAntIdTransType(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitHrmProfile build() {
            return (FitHrmProfile) super.build();
        }
    }
}
