/*  Copyright (C) 2023-2024 José Rebelo

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

import java.util.ArrayList;
import java.util.List;

public class GpxTrackSegment {
    private final List<GpxTrackPoint> trackPoints;
    private final List<GpxNavigationPoint> navigationPoints;

    public GpxTrackSegment(final List<GpxTrackPoint> trackPoints, final List<GpxNavigationPoint> navigationPoints) {
        this.trackPoints = trackPoints;
        this.navigationPoints = navigationPoints;
    }

    public List<GpxTrackPoint> getTrackPoints() {
        return trackPoints;
    }

    public List<GpxNavigationPoint> getNavigationPoints() {
        return navigationPoints;
    }

    public static class Builder {
        private final List<GpxTrackPoint> trackPoints = new ArrayList<>();
        private final List<GpxNavigationPoint> navigationPoints = new ArrayList<>();

        public Builder withTrackPoint(final GpxTrackPoint trackPoint) {
            trackPoints.add(trackPoint);
            return this;
        }

        public Builder withNavigationPoint(final GpxNavigationPoint navigationPoint) {
            navigationPoints.add(navigationPoint);
            return this;
        }

        public boolean isEmpty() {
            return trackPoints.isEmpty();
        }

        public GpxTrackSegment build() {
            return new GpxTrackSegment(trackPoints, navigationPoints);
        }

        public GpxTrackPoint getTrackPointAtIndex(final int index) {
            return trackPoints.get(index);
        }

        public List<GpxTrackPoint> getTrackPoints() {
            return trackPoints;
        }
    }
}
