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
public class FitWeightScale extends RecordData {
    public FitWeightScale(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 30) {
            throw new IllegalArgumentException("FitWeightScale expects global messages of " + 30 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Float getWeight() {
        return getFieldByNumber(0, Float.class);
    }

    @Nullable
    public Float getPercentFat() {
        return getFieldByNumber(1, Float.class);
    }

    @Nullable
    public Float getPercentHydration() {
        return getFieldByNumber(2, Float.class);
    }

    @Nullable
    public Float getVisceralFatMass() {
        return getFieldByNumber(3, Float.class);
    }

    @Nullable
    public Float getBoneMass() {
        return getFieldByNumber(4, Float.class);
    }

    @Nullable
    public Float getMuscleMass() {
        return getFieldByNumber(5, Float.class);
    }

    @Nullable
    public Float getBasalMet() {
        return getFieldByNumber(7, Float.class);
    }

    @Nullable
    public Integer getPhysiqueRating() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Float getActiveMet() {
        return getFieldByNumber(9, Float.class);
    }

    @Nullable
    public Integer getMetabolicAge() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getVisceralFatRating() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUserProfileIndex() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Float getBmi() {
        return getFieldByNumber(13, Float.class);
    }

    @Nullable
    public Long getTimestamp() {
        return getFieldByNumber(253, Long.class);
    }

    /**
     * @noinspection unused
     */
    public static class Builder extends FitRecordDataBuilder {
        public Builder() {
            super(30);
        }

        public Builder setWeight(final Float value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setPercentFat(final Float value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setPercentHydration(final Float value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setVisceralFatMass(final Float value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setBoneMass(final Float value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setMuscleMass(final Float value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setBasalMet(final Float value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setPhysiqueRating(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setActiveMet(final Float value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setMetabolicAge(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setVisceralFatRating(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setUserProfileIndex(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setBmi(final Float value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitWeightScale build() {
            return (FitWeightScale) super.build();
        }

        @Override
        public FitWeightScale build(final int localMessageType) {
            return (FitWeightScale) super.build(localMessageType);
        }
    }
}
