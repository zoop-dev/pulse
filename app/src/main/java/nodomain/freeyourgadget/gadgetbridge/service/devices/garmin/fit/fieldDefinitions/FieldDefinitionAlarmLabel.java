package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionAlarmLabel extends FieldDefinition {

    public FieldDefinitionAlarmLabel(final int localNumber, final int size, final BaseType baseType, final String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return Label.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(final ByteBuffer byteBuffer, final Object o) {
        if (o instanceof Label) {
            baseType.encode(byteBuffer, (((Label) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Label {
        NONE(0),
        WAKE_UP(1),
        WORKOUT(2),
        REMINDER(3),
        APPOINTMENT(4),
        TRAINING(5),
        CLASS(6),
        MEDITATE(7),
        BEDTIME(8),
        ;

        private final int id;

        Label(final int i) {
            id = i;
        }

        @Nullable
        public static Label fromId(final int id) {
            for (Label label : Label.values()) {
                if (id == label.getId()) {
                    return label;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }
}
