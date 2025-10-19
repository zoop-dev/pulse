/*  Copyright (C) 2023-2024 Alicia Hormann

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
package nodomain.freeyourgadget.gadgetbridge.model;

public interface TemperatureSample extends TimeSample {
    int TYPE_UNKNOWN = 0;
    int TYPE_BODY = 1;
    int TYPE_SKIN = 2;
    int TYPE_AMBIENT = 3;

    int LOCATION_UNKNOWN = 0;
    int LOCATION_ARMPIT = 1;
    int LOCATION_FINGER = 2;
    int LOCATION_FOREHEAD = 3;
    int LOCATION_MOUTH = 4;
    int LOCATION_RECTUM = 5;
    int LOCATION_TEMPORAL_ARTERY = 6;
    int LOCATION_TOE = 7;
    int LOCATION_EAR = 8;
    int LOCATION_WRIST = 9;

    /**
     * Returns the temperature value.
     */
    float getTemperature();

    /**
     * Returns the temperature type (the position on the body where the measurement was taken).
     */
    default int getTemperatureType() {
        return TYPE_UNKNOWN;
    }

    /**
     * Returns the temperature measurement location (the position on the body where the measurement was taken).
     */
    default int getTemperatureLocation() {
        return LOCATION_UNKNOWN;
    }
}
