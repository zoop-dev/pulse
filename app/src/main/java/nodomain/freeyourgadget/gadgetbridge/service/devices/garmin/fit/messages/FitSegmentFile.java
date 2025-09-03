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
 * @noinspection unused
 */
public class FitSegmentFile extends RecordData {
    public FitSegmentFile(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 151) {
            throw new IllegalArgumentException("FitSegmentFile expects global messages of " + 151 + ", got " + globalNumber);
        }
    }

    @Nullable
    public String getFileUuid() {
        return (String) getFieldByNumber(1);
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
    public Integer getLeaderType() {
        return (Integer) getFieldByNumber(7);
    }

    @Nullable
    public Long getLeaderGroupPrimaryKey() {
        return (Long) getFieldByNumber(8);
    }

    @Nullable
    public Long getLeaderActivityId() {
        return (Long) getFieldByNumber(9);
    }

    @Nullable
    public String getLeaderActivityIdString() {
        return (String) getFieldByNumber(10);
    }

    @Nullable
    public Integer getDefaultRaceLeader() {
        return (Integer) getFieldByNumber(11);
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
            super(151);
        }

        public Builder setFileUuid(final String value) {
            setFieldByNumber(1, value);
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

        public Builder setLeaderType(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setLeaderGroupPrimaryKey(final Long value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setLeaderActivityId(final Long value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setLeaderActivityIdString(final String value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setDefaultRaceLeader(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setMessageIndex(final Integer value) {
            setFieldByNumber(254, value);
            return this;
        }

        @Override
        public FitSegmentFile build() {
            return (FitSegmentFile) super.build();
        }
    }
}
