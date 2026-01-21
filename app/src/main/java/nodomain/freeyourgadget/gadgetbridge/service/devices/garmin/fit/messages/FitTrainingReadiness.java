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
public class FitTrainingReadiness extends RecordData {
    public FitTrainingReadiness(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 369) {
            throw new IllegalArgumentException("FitTrainingReadiness expects global messages of " + 369 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Integer getTrainingReadiness() {
        return getFieldByNumber(0, Integer.class);
    }

    @Nullable
    public Integer getLevel() {
        return getFieldByNumber(1, Integer.class);
    }

    @Nullable
    public Integer getUnknown2() {
        return getFieldByNumber(2, Integer.class);
    }

    @Nullable
    public Integer getUnknown3() {
        return getFieldByNumber(3, Integer.class);
    }

    @Nullable
    public Integer getUnknown4() {
        return getFieldByNumber(4, Integer.class);
    }

    @Nullable
    public Integer getUnknown5() {
        return getFieldByNumber(5, Integer.class);
    }

    @Nullable
    public Integer getUnknown6() {
        return getFieldByNumber(6, Integer.class);
    }

    @Nullable
    public Integer getUnknown7() {
        return getFieldByNumber(7, Integer.class);
    }

    @Nullable
    public Integer getUnknown8() {
        return getFieldByNumber(8, Integer.class);
    }

    @Nullable
    public Integer getUnknown9() {
        return getFieldByNumber(9, Integer.class);
    }

    @Nullable
    public Integer getUnknown10() {
        return getFieldByNumber(10, Integer.class);
    }

    @Nullable
    public Integer getUnknown11() {
        return getFieldByNumber(11, Integer.class);
    }

    @Nullable
    public Integer getUnknown12() {
        return getFieldByNumber(12, Integer.class);
    }

    @Nullable
    public Integer getUnknown13() {
        return getFieldByNumber(13, Integer.class);
    }

    @Nullable
    public Integer getUnknown14() {
        return getFieldByNumber(14, Integer.class);
    }

    @Nullable
    public Integer getUnknown15() {
        return getFieldByNumber(15, Integer.class);
    }

    @Nullable
    public Integer getUnknown16() {
        return getFieldByNumber(16, Integer.class);
    }

    @Nullable
    public Integer getUnknown17() {
        return getFieldByNumber(17, Integer.class);
    }

    @Nullable
    public Integer getUnknown18() {
        return getFieldByNumber(18, Integer.class);
    }

    @Nullable
    public Integer getUnknown19() {
        return getFieldByNumber(19, Integer.class);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return getFieldByNumber(20, Long.class);
    }

    @Nullable
    public Integer getUnknown21() {
        return getFieldByNumber(21, Integer.class);
    }

    @Nullable
    public Integer getUnknown22() {
        return getFieldByNumber(22, Integer.class);
    }

    @Nullable
    public Integer getUnknown25() {
        return getFieldByNumber(25, Integer.class);
    }

    @Nullable
    public Integer getUnknown26() {
        return getFieldByNumber(26, Integer.class);
    }

    @Nullable
    public Integer getUnknown27() {
        return getFieldByNumber(27, Integer.class);
    }

    @Nullable
    public Integer getUnknown28() {
        return getFieldByNumber(28, Integer.class);
    }

    @Nullable
    public Integer getUnknown29() {
        return getFieldByNumber(29, Integer.class);
    }

    @Nullable
    public Integer getUnknown30() {
        return getFieldByNumber(30, Integer.class);
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
            super(369);
        }

        public Builder setTrainingReadiness(final Integer value) {
            setFieldByNumber(0, value);
            return this;
        }

        public Builder setLevel(final Integer value) {
            setFieldByNumber(1, value);
            return this;
        }

        public Builder setUnknown2(final Integer value) {
            setFieldByNumber(2, value);
            return this;
        }

        public Builder setUnknown3(final Integer value) {
            setFieldByNumber(3, value);
            return this;
        }

        public Builder setUnknown4(final Integer value) {
            setFieldByNumber(4, value);
            return this;
        }

        public Builder setUnknown5(final Integer value) {
            setFieldByNumber(5, value);
            return this;
        }

        public Builder setUnknown6(final Integer value) {
            setFieldByNumber(6, value);
            return this;
        }

        public Builder setUnknown7(final Integer value) {
            setFieldByNumber(7, value);
            return this;
        }

        public Builder setUnknown8(final Integer value) {
            setFieldByNumber(8, value);
            return this;
        }

        public Builder setUnknown9(final Integer value) {
            setFieldByNumber(9, value);
            return this;
        }

        public Builder setUnknown10(final Integer value) {
            setFieldByNumber(10, value);
            return this;
        }

        public Builder setUnknown11(final Integer value) {
            setFieldByNumber(11, value);
            return this;
        }

        public Builder setUnknown12(final Integer value) {
            setFieldByNumber(12, value);
            return this;
        }

        public Builder setUnknown13(final Integer value) {
            setFieldByNumber(13, value);
            return this;
        }

        public Builder setUnknown14(final Integer value) {
            setFieldByNumber(14, value);
            return this;
        }

        public Builder setUnknown15(final Integer value) {
            setFieldByNumber(15, value);
            return this;
        }

        public Builder setUnknown16(final Integer value) {
            setFieldByNumber(16, value);
            return this;
        }

        public Builder setUnknown17(final Integer value) {
            setFieldByNumber(17, value);
            return this;
        }

        public Builder setUnknown18(final Integer value) {
            setFieldByNumber(18, value);
            return this;
        }

        public Builder setUnknown19(final Integer value) {
            setFieldByNumber(19, value);
            return this;
        }

        public Builder setLocalTimestamp(final Long value) {
            setFieldByNumber(20, value);
            return this;
        }

        public Builder setUnknown21(final Integer value) {
            setFieldByNumber(21, value);
            return this;
        }

        public Builder setUnknown22(final Integer value) {
            setFieldByNumber(22, value);
            return this;
        }

        public Builder setUnknown25(final Integer value) {
            setFieldByNumber(25, value);
            return this;
        }

        public Builder setUnknown26(final Integer value) {
            setFieldByNumber(26, value);
            return this;
        }

        public Builder setUnknown27(final Integer value) {
            setFieldByNumber(27, value);
            return this;
        }

        public Builder setUnknown28(final Integer value) {
            setFieldByNumber(28, value);
            return this;
        }

        public Builder setUnknown29(final Integer value) {
            setFieldByNumber(29, value);
            return this;
        }

        public Builder setUnknown30(final Integer value) {
            setFieldByNumber(30, value);
            return this;
        }

        public Builder setTimestamp(final Long value) {
            setFieldByNumber(253, value);
            return this;
        }

        @Override
        public FitTrainingReadiness build() {
            return (FitTrainingReadiness) super.build();
        }

        @Override
        public FitTrainingReadiness build(final int localMessageType) {
            return (FitTrainingReadiness) super.build(localMessageType);
        }
    }
}
