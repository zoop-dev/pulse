package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionSwimStyle extends FieldDefinition {

    public FieldDefinitionSwimStyle(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return SwimStyle.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof SwimStyle) {
            baseType.encode(byteBuffer, (((SwimStyle) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum SwimStyle {
        FREESTYLE(0, R.string.freestyle),
        BACKSTROKE(1, R.string.backstroke),
        BREASTSTROKE(2, R.string.breaststroke),
        BUTTERFLY(3, R.string.swim_style_butterfly),
        DRILL(4, R.string.swim_style_drill),
        MIXED(5, R.string.swim_style_mixed),
        ;

        private final int id;
        private final int nameResId;

        SwimStyle(int i, int nameResId) {
            this.id = i;
            this.nameResId = nameResId;
        }

        @Nullable
        public static SwimStyle fromId(int id) {
            for (SwimStyle language : SwimStyle.values()) {
                if (id == language.getId()) {
                    return language;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }

        @StringRes
        public int getNameResId() {
            return nameResId;
        }
    }
}
