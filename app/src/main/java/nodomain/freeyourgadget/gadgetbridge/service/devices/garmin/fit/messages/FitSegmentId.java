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
        return (String) getFieldByNumber(0);
    }

    @Nullable
    public String getUuid() {
        return (String) getFieldByNumber(1);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getEnabled() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Long getUserProfilePrimaryKey() {
        return (Long) getFieldByNumber(4);
    }

    @Nullable
    public Long getDeviceId() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Integer getDefaultRaceLeader() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getDeleteStatus() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Integer getSelectionType() {
        return (Integer) getFieldByNumber(8);
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
    }
}
