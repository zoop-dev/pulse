package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitNap extends RecordData {
    public FitNap(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 412) {
            throw new IllegalArgumentException("FitNap expects global messages of " + 412 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getStartTimestamp() {
        return (Long) getFieldByNumber(0);
    }

    @Nullable
    public Integer getUnknown1() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Long getEndTimestamp() {
        return (Long) getFieldByNumber(2);
    }

    @Nullable
    public Integer getUnknown3() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getUnknown4() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Integer getUnknown6() {
        return (Integer) getFieldByNumber(6);
    }

    @Nullable
    public Long getTimestamp7() {
        return (Long) getFieldByNumber(7);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
