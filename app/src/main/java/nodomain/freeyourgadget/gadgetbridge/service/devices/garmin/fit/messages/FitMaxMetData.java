package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitMaxMetData extends RecordData {
    public FitMaxMetData(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 229) {
            throw new IllegalArgumentException("FitMaxMetData expects global messages of " + 229 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getUpdateTime() {
        return (Long) getFieldByNumber(0);
    }

    @Nullable
    public Float getVo2Max() {
        return (Float) getFieldByNumber(2);
    }

    @Nullable
    public Integer getSport() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Integer getSubSport() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Integer getMaxMetCategory() {
        return (Integer) getFieldByNumber(8);
    }

    @Nullable
    public Integer getCalibratedData() {
        return (Integer) getFieldByNumber(9);
    }
}
