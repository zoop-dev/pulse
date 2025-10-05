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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;

public class GpxFile {
    private final String name;
    private final String author;
    @Nullable
    private final Date time;

    private final List<GpxTrack> tracks;
    private final List<GpxWaypoint> waypoints;

    public GpxFile(@Nullable final String name, @Nullable final String author, @Nullable final Date time,
                   final List<GpxTrack> tracks, final List<GpxWaypoint> waypoints) {
        this.name = name;
        this.author = author;
        this.time = time;
        this.tracks = tracks;
        this.waypoints = waypoints;
    }

    @Nullable
    public Date getTime() {
        return time;
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public List<GpxTrack> getTracks() {
        return tracks;
    }

    public List<GpxWaypoint> getWaypoints() {
        return waypoints;
    }

    public List<GpxTrackPoint> getPoints() {
        final List<GpxTrackPoint> allPoints = new ArrayList<>();

        for (final GpxTrack track : tracks) {
            for (final GpxTrackSegment trackSegment : track.getTrackSegments()) {
                allPoints.addAll(trackSegment.getTrackPoints());
            }
        }

        return allPoints;
    }

    public List<ActivityPoint> getActivityPoints() {
        return tracks.stream()
                .flatMap(t -> t.getTrackSegments().stream())
                .flatMap(s -> s.getTrackPoints().stream())
                .map(GpxTrackPoint::toActivityPoint)
                .collect(Collectors.toList());
    }

    public static class Builder {
        private String name;
        private String author;
        private Date time;

        private List<GpxTrack> tracks = new ArrayList<>();
        private List<GpxWaypoint> waypoints = new ArrayList<>();

        public Builder withTime(final Date date) {
            this.time = date;
            return this;
        }

        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public Builder withAuthor(final String author) {
            this.author = author;
            return this;
        }

        public Builder withTrack(final GpxTrack track) {
            this.tracks.add(track);
            return this;
        }

        public Builder withWaypoints(final GpxWaypoint waypoint) {
            this.waypoints.add(waypoint);
            return this;
        }

        public GpxFile build() {
            return new GpxFile(name, author, time, tracks, waypoints);
        }
    }
}
