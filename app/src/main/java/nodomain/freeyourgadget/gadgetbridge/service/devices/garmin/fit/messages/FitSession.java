/*  Copyright (C) 2024-2026 José Rebelo, Daniele Gobbetti, Sebastian Dröge, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitSession extends AbstractFitSession {
    public FitSession(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);
    }

    public List<ActivityPoint> toActivityPoints() {
        final List<ActivityPoint> activityPoints = new ArrayList<ActivityPoint>();

        if(getComputedTimestamp() == null) {
            // some sessions - especially from Strava, have no time stamp
            return activityPoints;
        }

        final ActivityPoint startActivityPoint = new ActivityPoint();
        startActivityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getStartLatitude() != null && getStartLongitude() != null) {
            startActivityPoint.setLocation(new GPSCoordinate(
                    getStartLongitude(),
                    getStartLatitude(),
                    GPSCoordinate.UNKNOWN_ALTITUDE
            ));
            activityPoints.add(startActivityPoint);
        }
        final ActivityPoint endActivityPoint = new ActivityPoint();
        endActivityPoint.setTime(new Date(getComputedTimestamp() * 1000L));
        if (getEndLatitude() != null && getEndLongitude() != null) {
            endActivityPoint.setLocation(new GPSCoordinate(
                    getEndLongitude(),
                    getEndLatitude(),
                    GPSCoordinate.UNKNOWN_ALTITUDE
            ));
            activityPoints.add(endActivityPoint);
        }

        return activityPoints;
    }
}
