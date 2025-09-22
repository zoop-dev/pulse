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
public class FitSdmProfile extends RecordData {
    public FitSdmProfile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 5) {
            throw new IllegalArgumentException("FitSdmProfile expects global messages of " + 5 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Boolean getEnabled() {
        return (Boolean) getFieldByNumber(0);
    }

    @Nullable
    public Integer getSdmAntId() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Float getSdmCalFactor() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Double getOdometer() {
        return (Double) getFieldByNumber(3);
    }

    @Nullable
    public Integer getSpeedSource() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSdmAntIdTransType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getOdometerRollover() {
        return (Integer) getFieldByNumber(7);
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
            super(5);
        }

        public Builder setEnabled(final Boolean value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setSdmAntId(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setSdmCalFactor(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setOdometer(final Double value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setSpeedSource(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setSdmAntIdTransType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setOdometerRollover(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSdmProfile build() {
            return (FitSdmProfile) super.build();
        }
    }
}
