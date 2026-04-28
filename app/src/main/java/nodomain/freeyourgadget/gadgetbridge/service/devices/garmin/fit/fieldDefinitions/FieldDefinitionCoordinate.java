package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionCoordinate extends FieldDefinition {

    public FieldDefinitionCoordinate(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawValue = baseType.decode(byteBuffer, 1, 0);
        if (rawValue == null) {
            return null;
        }
        return GarminUtils.semicirclesToDegrees(((Number) rawValue).intValue());
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        baseType.encode(byteBuffer, GarminUtils.degreesToSemicircles((double) o), 1, 0);
    }


}
