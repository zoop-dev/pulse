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
public class FitRouting extends RecordData {
    public FitRouting(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 71) {
            throw new IllegalArgumentException("FitRouting expects native messages of " + 71 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public Integer getRoutingMode() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getCalculationMethod() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getLockOnRoad() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getAvoidances() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getRouteRecalculation() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getType() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getCourseRecalculation() {
        return getFieldByNumber(7, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(71);
        }

        public Builder setRoutingMode(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setCalculationMethod(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setLockOnRoad(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setAvoidances(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setRouteRecalculation(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setType(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setCourseRecalculation(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        @Override
        public FitRouting build() {
            return (FitRouting) super.build();
        }

        @Override
        public FitRouting build(final int localMessageType) {
            return (FitRouting) super.build(localMessageType);
        }
    }
}
