/*  Copyright (C) 2024 José Rebelo

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
public class FitStressLevel extends RecordData {
    public FitStressLevel(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 227) {
            throw new IllegalArgumentException("FitStressLevel expects global messages of " + 227 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getStressLevelValue() {
        return (Integer) getFieldByNumber(0);
    }

    @Nullable
    public Long getStressLevelTime() {
        return (Long) getFieldByNumber(1);
    }

    @Nullable
    public Integer getBodyEnergy() {
        return (Integer) getFieldByNumber(3);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(227);
        }

        public Builder setStressLevelValue(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setStressLevelTime(final Long value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setBodyEnergy(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        @Override
        public FitStressLevel build() {
            return (FitStressLevel) super.build();
        }
    }

    // manual changes below

    @Override
    public Long getComputedTimestamp() {
        final Long stressTime = getStressLevelTime();
        if (stressTime != null) {
            return stressTime;
        }
        return super.getComputedTimestamp();
    }
}
