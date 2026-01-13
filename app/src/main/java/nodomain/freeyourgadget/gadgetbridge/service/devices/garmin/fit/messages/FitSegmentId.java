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
public class FitSegmentId extends RecordData {
    public FitSegmentId(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 148) {
            throw new IllegalArgumentException("FitSegmentId expects global messages of " + 148 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getName() {
        return getFieldByNumber(0, String.class);
    }

    @Nullable
    public String getUuid() {
        return getFieldByNumber(1, String.class);
    }

    @Nullable
    public Integer getSport() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getEnabled() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Long getUserProfilePrimaryKey() {
        return getFieldByNumber(4, Long.class);
    }

    @Nullable
    public Long getDeviceId() {
        return getFieldByNumber(5, Long.class);
    }

    @Nullable
    public Integer getDefaultRaceLeader() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getDeleteStatus() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getSelectionType() {
        return getFieldByNumber(8, Integer.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(148);
        }

        public Builder setName(final String value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setUuid(final String value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setSport(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setEnabled(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setUserProfilePrimaryKey(final Long value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setDeviceId(final Long value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setDefaultRaceLeader(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setDeleteStatus(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setSelectionType(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        @Override
        public FitSegmentId build() {
            return (FitSegmentId) super.build();
        }

        @Override
        public FitSegmentId build(final int localMessageType) {
            return (FitSegmentId) super.build(localMessageType);
        }
    }
}
