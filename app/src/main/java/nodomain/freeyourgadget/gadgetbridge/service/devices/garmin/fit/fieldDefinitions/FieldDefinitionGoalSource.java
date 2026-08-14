package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GoalSource;

public class FieldDefinitionGoalSource extends FieldDefinition {

    public FieldDefinitionGoalSource(int localNumber, int size, BaseType baseType, String name, final int scale, final int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    public static GoalSource fromId(final int id) {
        for (final GoalSource candidate : GoalSource.values()) {
            if (id == candidate.id) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public GoalSource decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof GoalSource goalSource) {
            baseType.encode(byteBuffer, goalSource.id, scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
