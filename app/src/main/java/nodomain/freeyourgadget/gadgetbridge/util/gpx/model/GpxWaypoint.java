/*  Copyright (C) 2023-2025 José Rebelo, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.util.gpx.model;

import androidx.annotation.Nullable;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public class GpxWaypoint extends GPSCoordinate {
    private final float depth;

    @Nullable
    private final String description;

    @Nullable
    private final String name;

    @Nullable
    private final String symbol;

    private final float temperature;

    @Nullable
    private final Date time;

    public GpxWaypoint(final double longitude, final double latitude, final double altitude,
                       @Nullable final Date time, @Nullable final String name,
                       @Nullable final String description, @Nullable final String symbol,
                       final double hdop, final double vdop, final double pdop,
                       final float temperature, final float depth) {
        super(longitude, latitude, altitude, hdop, vdop, pdop);
        this.depth = depth;
        this.description = description;
        this.name = name;
        this.symbol = symbol;
        this.time = time;
        this.temperature = temperature;
    }

    public float getDepth() {
        return depth;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getSymbol() {
        return symbol;
    }

    public float getTemperature() {
        return temperature;
    }

    @Nullable
    public Date getTime() {
        return time;
    }
}
