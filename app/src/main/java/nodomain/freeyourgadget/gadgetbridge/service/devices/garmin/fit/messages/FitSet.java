package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionExerciseCategory.ExerciseCategory;

//
// WARNING: This class was auto-generated, please avoid modifying it directly.
// See nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.codegen.FitCodeGen
//
public class FitSet extends RecordData {
    public FitSet(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);

        final int globalNumber = recordDefinition.getGlobalFITMessage().getNumber();
        if (globalNumber != 225) {
            throw new IllegalArgumentException("FitSet expects global messages of " + 225 + ", got " + globalNumber);
        }
    }

    @Nullable
    public Double getDuration() {
        return (Double) getFieldByNumber(0);
    }

    @Nullable
    public Integer getRepetitions() {
        return (Integer) getFieldByNumber(3);
    }

    @Nullable
    public Float getWeight() {
        return (Float) getFieldByNumber(4);
    }

    @Nullable
    public Integer getSetType() {
        return (Integer) getFieldByNumber(5);
    }

    @Nullable
    public Long getStartTime() {
        return (Long) getFieldByNumber(6);
    }

    @Nullable
    public ExerciseCategory[] getCategory() {
        final Object[] objectsArray = (Object[]) getFieldByNumber(7);
        if (objectsArray == null)
            return null;
        final ExerciseCategory[] ret = new ExerciseCategory[objectsArray.length];
        for (int i = 0; i < objectsArray.length; i++) {
            ret[i] = (ExerciseCategory) objectsArray[i];
        }
        return ret;
    }

    @Nullable
    public Integer getMessageIndex() {
        return (Integer) getFieldByNumber(10);
    }

    @Nullable
    public Long getTimestamp() {
        return (Long) getFieldByNumber(254);
    }
}
