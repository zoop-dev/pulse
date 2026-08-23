package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionDayOfWeek extends FieldDefinition {

    public FieldDefinitionDayOfWeek(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj instanceof final Number number) {
            final int raw = number.intValue();
            return DayOfWeek.of(raw == 0 ? 7 : raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof final Number number) {
            long epoc = number.longValue();
            Instant instant = Instant.ofEpochSecond(epoc);
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zonedDateTime = instant.atZone(zoneId);
            o = zonedDateTime.getDayOfWeek();
        }

        if (o instanceof final DayOfWeek dayOfWeek) {
            o = dayOfWeek.getValue() % 7;
        } else {
            o = null;
        }

        baseType.encode(byteBuffer, o, scale, offset);
    }
}
