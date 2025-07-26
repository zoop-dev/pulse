package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;
import java.time.LocalTime;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionAlarm extends FieldDefinition {
    public FieldDefinitionAlarm(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawValue = baseType.decode(byteBuffer, scale, offset);
        if (rawValue == null) {
            return null;
        }
        final int value = (int) rawValue;
        return LocalTime.of(value / 60, value % 60);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof LocalTime localTime) {
            baseType.encode(byteBuffer, localTime.getHour() * 60 + localTime.getMinute(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
