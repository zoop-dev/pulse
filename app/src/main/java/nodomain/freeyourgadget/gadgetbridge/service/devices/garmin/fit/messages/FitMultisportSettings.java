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
public class FitMultisportSettings extends RecordData {
    public FitMultisportSettings(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int nativeNumber = recordDefinition.getNativeFITMessage().getNumber();
        if (nativeNumber != 143) {
            throw new IllegalArgumentException("FitMultisportSettings expects native messages of " + 143 + ", got " + nativeNumber);
        }
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public Integer getTransitions() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getNumberOfActivities() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getAutoPause() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getAlerts() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getAutoLap() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getPowerSaveTimeout() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getAutoScroll() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getRepeat() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getSportChange() {
        return getFieldByNumber(10, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(143);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setTransitions(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setNumberOfActivities(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setAutoPause(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setAlerts(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setAutoLap(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setPowerSaveTimeout(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setAutoScroll(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setRepeat(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setSportChange(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        @Override
        public FitMultisportSettings build() {
            return (FitMultisportSettings) super.build();
        }

        @Override
        public FitMultisportSettings build(final int localMessageType) {
            return (FitMultisportSettings) super.build(localMessageType);
        }
    }
}
