/*  Copyright (C) 2025 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.stream.DoubleStream;

/**
 * This class was copied and modified from
 * https://github.com/OpenTracksApp/OSMDashboard/blob/v4.3.0/src/main/java/de/storchp/opentracks/osmplugin/dashboardapi/TrackPointsBySegments.java
 */
public record TrackPointsBySegments(List<List<TrackPoint>> segments) {

    public boolean isEmpty() {
        return segments.isEmpty();
    }

    public double calcAverageSpeed() {
        return streamTrackPointsWithSpeed().average().orElse(0.0);
    }

    public double calcMaxSpeed() {
        return streamTrackPointsWithSpeed().max().orElse(0.0);
    }

    @NonNull
    private DoubleStream streamTrackPointsWithSpeed() {
        return segments.stream().flatMap(List::stream).mapToDouble(TrackPoint::getSpeed).filter(speed -> speed > 0);
    }
}
