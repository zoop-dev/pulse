package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;
import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionAlarm extends FieldDefinition {
    public FieldDefinitionAlarm(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, raw / 60);
            calendar.set(Calendar.MINUTE, raw % 60);
            return calendar;
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Calendar) {
            baseType.encode(byteBuffer, ((Calendar) o).get(Calendar.HOUR_OF_DAY) * 60 + ((Calendar) o).get(Calendar.MINUTE), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }
}
