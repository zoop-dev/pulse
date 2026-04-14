package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionBatteryStatus extends FieldDefinition {

    public FieldDefinitionBatteryStatus(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Nullable
    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final int raw = (int) rawObj;
            return BatteryStatus.fromId(raw);
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof BatteryStatus) {
            baseType.encode(byteBuffer, (((BatteryStatus) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum BatteryStatus {
        New(1, R.string.battery_status_new),
        Good(2, R.string.battery_status_good),
        Ok(3, R.string.battery_status_ok),
        Low(4, R.string.battery_status_low),
        Critical(5, R.string.battery_status_critical),
        Charging(6, R.string.battery_status_charging),
        Unknown(7, R.string.battery_status_unknown),
        ;

        private final int id;
        @StringRes
        private final int stringRes;

        BatteryStatus(int i, @StringRes int text) {
            id = i;
            stringRes = text;
        }

        @Nullable
        public static BatteryStatus fromId(int id) {
            for (BatteryStatus batteryStatus : values()) {
                if (id == batteryStatus.id) {
                    return batteryStatus;
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
