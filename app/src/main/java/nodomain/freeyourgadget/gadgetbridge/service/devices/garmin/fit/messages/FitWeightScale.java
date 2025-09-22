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
        return (Float) getFieldByNumber(0);
    }

    @Nullable
    public Float getPercentFat() {
        return (Float) getFieldByNumber(1);
    }

    @Nullable
    public Float getPercentHydration() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Float getVisceralFatMass() {
        return (Float) getFieldByNumber(3);
    }

    @Nullable
    public Float getBoneMass() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Float getMuscleMass() {
        return (Float) getFieldByNumber(5);
    }

    @Nullable
    public Float getBasalMet() {
        return (Float) getFieldByNumber(7);
    }

    @Nullable
    public Integer getPhysiqueRating() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Float getActiveMet() {
        return (Float) getFieldByNumber(9);
    }

    @Nullable
    public Integer getMetabolicAge() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Integer getVisceralFatRating() {
        return (Integer) getFieldByNumber(11);
    }

    @Nullable
    public Integer getUserProfileIndex() {
        return (Integer) getFieldByNumber(12);
    }

    @Nullable
    public Float getBmi() {
        return (Float) getFieldByNumber(13);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
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
    }
}
