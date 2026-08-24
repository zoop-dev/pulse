/*  Copyright (C) 2024-2026 Daniele Gobbetti, José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionTemperature extends FieldDefinition {

    public FieldDefinitionTemperature(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        // Don't hardcode the fake Kelvin to Celsius transformation (-273 instead of -273.15) for
        // all temperature fields. Only do the transformation where it is actually needed
        // (e.g. FitWeather.Builder) to limit the 0.15/0.85 error.
        super(localNumber, size, baseType, name, scale, offset);
    }
}
