/*  Copyright (C) 2024-2026 Daniele Gobbetti, José Rebelo, punchdeerflyscorpion, Thomas Kuehne

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionAlarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionArray;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionBoolean;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionDayOfWeek;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionFileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrTimeInZone;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionHrZoneHighBoundary;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTemperature;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitions;

public class FieldDefinitionFactory {
    public static FieldDefinition create(int localNumber, int size, FieldDefinitions field, BaseType baseType, String name, int scale, int offset) {
        if (null == field) {
            return new FieldDefinition(localNumber, size, baseType, name, scale, offset);
        }
        return switch (field) {
            case ALARM -> new FieldDefinitionAlarm(localNumber, size, baseType, name);
            case ARRAY -> new FieldDefinitionArray(localNumber, size, baseType, name, scale, offset);
            case BOOLEAN -> new FieldDefinitionBoolean(localNumber, size, baseType, name);
            case DAY_OF_WEEK -> new FieldDefinitionDayOfWeek(localNumber, size, baseType, name);
            case FILE_TYPE -> new FieldDefinitionFileType(localNumber, size, baseType, name);
            case HR_TIME_IN_ZONE -> new FieldDefinitionHrTimeInZone(localNumber, size, baseType, name);
            case HR_ZONE_HIGH_BOUNDARY -> new FieldDefinitionHrZoneHighBoundary(localNumber, size, baseType, name);
            case TEMPERATURE -> new FieldDefinitionTemperature(localNumber, size, baseType, name);
            case TIMESTAMP -> new FieldDefinitionTimestamp(localNumber, size, baseType, name);
            case COORDINATE -> new FieldDefinitionCoordinate(localNumber, size, baseType, name);
            default -> FieldDefinitions.create(localNumber, size, field, baseType, name, scale, offset);
        };
    }
}
