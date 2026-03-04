package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.utils;

import java.util.List;


public class HuaweiRouteTrack {
    public static class TrackPoint {
        private final double altitude;
        private final double latitude;
        private final double longitude;

        public TrackPoint(double latitude, double longitude, double altitude) {
            this.altitude = altitude;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getAltitude() {
            return altitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }

    private boolean hasAltitude;
    private List<TrackPoint> points;
    private long totalTime;
    private String trackId;
    private String trackName;
    private int version = 2;

    public List<TrackPoint> getPoints() {
        return this.points;
    }

    public void setPoints(List<TrackPoint> list) {
        this.points = list;
    }

    public boolean isHasAltitude() {
        return this.hasAltitude;
    }

    public void setHasAltitude(boolean z) {
        this.hasAltitude = z;
    }

    public String getTrackId() {
        return this.trackId;
    }

    public void setTrackId(String str) {
        this.trackId = str;
    }

    public String getTrackName() {
        return this.trackName;
    }

    public void setTrackName(String str) {
        this.trackName = str;
    }

    public long getTotalTime() {
        return this.totalTime;
    }

    public void setTotalTime(long j) {
        this.totalTime = j;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int i) {
        this.version = i;
    }
}
