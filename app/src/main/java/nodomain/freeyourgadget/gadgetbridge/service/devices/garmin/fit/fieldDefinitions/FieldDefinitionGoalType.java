package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GoalType;

public class FieldDefinitionGoalType extends FieldDefinition {

    public FieldDefinitionGoalType(final int localNumber, final int size, final BaseType baseType, final String name, final int scale, final int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }


    @Nullable
    public static GoalType fromId(final int id) {
        for (final GoalType candidate : GoalType.values()) {
            if (id == candidate.id) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public GoalType decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof GoalType goalType) {
            baseType.encode(byteBuffer, goalType.id, scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

}
