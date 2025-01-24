package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitActivity extends RecordData {
    public FitActivity(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 34) {
            throw new IllegalArgumentException("FitActivity expects global messages of " + 34 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Long getTotalTimerTime() {
        return (Long) getFieldByNumber(0);
    }

    @Nullable
    public Integer getNumSessions() {
        return (Integer) getFieldByNumber(1);
    }

    @Nullable
    public Integer getType() {
        return (Integer) getFieldByNumber(2);
    }

    @Nullable
    public Integer getEvent() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Integer getEventType() {
        return (Integer) getFieldByNumber(4);
    }

    @Nullable
    public Long getLocalTimestamp() {
        return (Long) getFieldByNumber(5);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(253);
    }
}
