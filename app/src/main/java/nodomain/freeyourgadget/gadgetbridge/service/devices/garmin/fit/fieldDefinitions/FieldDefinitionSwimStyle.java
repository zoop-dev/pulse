package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.SwimStyle;

public class FieldDefinitionSwimStyle extends FieldDefinition {

    public FieldDefinitionSwimStyle(int localNumber, int size, BaseType baseType, String name, final int scale, final int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    public static SwimStyle fromId(final int id) {
        for (final SwimStyle candidate : SwimStyle.values()) {
            if (id == candidate.id) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public SwimStyle decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof SwimStyle swimStyle) {
            baseType.encode(byteBuffer, swimStyle.id, scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
