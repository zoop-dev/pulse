package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils.GARMIN_TIME_EPOCH;

public class FieldDefinitionTimestamp extends FieldDefinition {
    public FieldDefinitionTimestamp(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, -GARMIN_TIME_EPOCH);
    }
}
