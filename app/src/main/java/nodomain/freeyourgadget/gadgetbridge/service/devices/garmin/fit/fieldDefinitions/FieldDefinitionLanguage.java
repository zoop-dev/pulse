package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.Language;

public class FieldDefinitionLanguage extends FieldDefinition {

    public FieldDefinitionLanguage(int localNumber, int size, BaseType baseType, String name, final int scale, final int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    public static Language fromId(final int id) {
        for (final Language candidate : Language.values()) {
            if (id == candidate.id) {
                return candidate;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public Language decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number raw) {
            final int id = raw.intValue();
            return fromId(id);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Language language) {
            baseType.encode(byteBuffer, language.id, scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

}
