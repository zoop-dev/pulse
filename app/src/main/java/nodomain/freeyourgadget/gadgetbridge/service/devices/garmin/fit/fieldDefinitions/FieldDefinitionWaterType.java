package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionWaterType extends FieldDefinition {

    public FieldDefinitionWaterType(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return WaterType.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof WaterType) {
            baseType.encode(byteBuffer, (((WaterType) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum WaterType {
        Fresh(0, R.string.water_type_fresh),
        Salt(1, R.string.water_type_salt),
        En13319(2, R.string.water_type_en13319),
        Custom(3, R.string.water_type_custom),
        ;

        private final int id;
        @StringRes
        private final int stringRes;

        WaterType(int i, @StringRes int text) {
            id = i;
            stringRes = text;
        }

        @Nullable
        public static WaterType fromId(int id) {
            for (WaterType waterType : values()) {
                if (id == waterType.id) {
                    return waterType;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }

        public String toString(@NonNull Context context) {
            return context.getString(stringRes);
        }
    }
}
