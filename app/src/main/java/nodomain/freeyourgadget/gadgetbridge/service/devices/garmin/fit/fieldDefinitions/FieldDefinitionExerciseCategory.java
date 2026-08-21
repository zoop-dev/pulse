package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.ExerciseCategory;

public class FieldDefinitionExerciseCategory extends FieldDefinition {

    public FieldDefinitionExerciseCategory(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    public static ExerciseCategory fromId(final int id) {
        for (final ExerciseCategory candidate : ExerciseCategory.values()) {
            if (id == candidate.num) {
                return candidate;
            }
        }
        return ExerciseCategory.CATEGORY_UNKNOWN;
    }

    @Nullable
    @Override
    public ExerciseCategory decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof ExerciseCategory exerciseCategory) {
            baseType.encode(byteBuffer, exerciseCategory.num, scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
