package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.utils;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HuaweiConvertTrack {

    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;
    private static final double DEG_90_RADIAN = Math.PI / 2.0;
    private static final double METERS_PER_DEGREE = 1111949.375; // Approx meters per degree at equator
    private static final double EARTH_RADIUS_METERS = 6371393.0;

    private static final float COORDINATE_TOLERANCE = 0.0001F;  // ~11 meters at equator

    private static final int POINT_INDEX_THRESHOLD = 30;
    private static final int POINT_DISTANCE_THRESHOLD = 50;
    private static final int DISTANCE_THRESHOLD = 200;



    private static class TrackPoint {
        double lon = 0;
        double lat = 0;
        int deltaXMeters = 0;
        int deltaYMeters = 0;
        int pointIndex = 0;
        int distance = 0;
        byte type = 0;

        public TrackPoint(TrackPoint point) {
            setFromPoint(point);
        }

        public void setFromPoint(final TrackPoint point) {
            this.lon = point.lon;
            this.lat = point.lat;
            this.deltaXMeters = point.deltaXMeters;
            this.deltaYMeters = point.deltaYMeters;
            this.pointIndex = point.pointIndex;
            this.distance = point.distance;
            this.type = point.type;
        }

        public TrackPoint() {
        }

        @NonNull
        @Override
        public String toString() {
            return "TrackPoint{" + "lon=" + lon +
                    ", lat=" + lat +
                    ", deltaXMeters=" + deltaXMeters +
                    ", deltaYMeters=" + deltaYMeters +
                    ", pointIndex=" + pointIndex +
                    ", distance=" + distance +
                    ", type=" + type +
                    '}';
        }
    }

    private final List<TrackPoint> simplifiedTrackPoints = new ArrayList<>();

    private final TrackPoint baseTrackPoint = new TrackPoint();
    private final TrackPoint currentTrackPoint = new TrackPoint();
    private final TrackPoint previousTrackPoint = new TrackPoint();
    private final TrackPoint refTrackPoint = new TrackPoint();
    private final TrackPoint intermediateTrackPoint = new TrackPoint();

    int maxDeviationFromLine = 0;

    float slope = 0.0f;
    float intercept = 0.0f;
    int accumulatedSegmentDistance = 0;

    private int maxPoints;

    private double total3DDistance = 0;
    private double totalDistance;

    private double[] lat_arr = null;
    private double[] lon_arr = null;
    private double[] alt_arr = null;
    private double[] cumulativeDistanceArr = null;
    private double[] cumulative3DDistanceArr = null;
    private double[] cumulativeAscentArr = null;
    private double[] cumulativeDescentArr = null;

    private byte[] altitudeResultBuffer = null;

    private byte[] serializeTrackForDevice(HuaweiRouteTrack trackInfo) {
        if (trackInfo.getVersion() != 1) {
            int savePoints = Math.min(maxPoints, simplifiedTrackPoints.size());

            ByteBuffer header = ByteBuffer.allocate(0x108);
            header.order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(-1); // magic
            header.putInt(trackInfo.getVersion()); // version
            header.putInt((int) trackInfo.getTotalTime());
            header.putFloat((float) totalDistance);
            header.putInt(savePoints);
            header.put(Arrays.copyOf(trackInfo.getTrackId().getBytes(), 0x24));
            header.put(Arrays.copyOf(trackInfo.getTrackName().getBytes(), 0x80));
            header.putFloat((float) total3DDistance);
            header.putInt(trackInfo.isHasAltitude() ? 1 : 0);
            header.putInt(0); // TODO: unknown
            header.putInt(0);  // TODO: unknown
            header.put((byte) 0);  // TODO: unknown
            header.put((byte) 0);
            header.putShort((short) 0);

            int resultSize = savePoints * 0x38 + 0x108;
            if (trackInfo.isHasAltitude())
                resultSize += 0xd38;
            ByteBuffer result = ByteBuffer.allocate(resultSize);
            result.order(ByteOrder.LITTLE_ENDIAN);

            result.put(header.array());
            for (int i = 0; i < savePoints; i++) {
                TrackPoint pt = simplifiedTrackPoints.get(i);
                result.putDouble(pt.lon);
                result.putDouble(pt.lat);
                result.putInt(pt.deltaXMeters);
                result.putInt(pt.deltaYMeters);
                result.putInt(pt.pointIndex);
                result.putInt((int) Math.round(cumulativeDistanceArr[pt.pointIndex]));
                result.putShort((short) 0); // unknown
                result.put(pt.type);
                result.put((byte) 0); // unknown
                result.putInt((int) Math.round(alt_arr[pt.pointIndex]));
                result.putInt((int) Math.round(cumulative3DDistanceArr[pt.pointIndex]));
                result.putInt((int) Math.round(cumulativeAscentArr[pt.pointIndex]));
                result.putInt((int) Math.round(Math.abs(cumulativeDescentArr[pt.pointIndex])));
                result.putInt(0); // unknown
            }
            if (trackInfo.isHasAltitude() && altitudeResultBuffer != null) {
                result.put(altitudeResultBuffer);
            }
            return result.array();
        } else {
            int savePoints = simplifiedTrackPoints.size();
            int resultSize = savePoints * 0x28 + 0xc;

            ByteBuffer header = ByteBuffer.allocate(0xc);
            header.order(ByteOrder.LITTLE_ENDIAN);
            header.putInt((int) trackInfo.getTotalTime());
            header.putFloat((float) totalDistance);
            header.putInt(savePoints);

            ByteBuffer result = ByteBuffer.allocate(resultSize);
            result.order(ByteOrder.LITTLE_ENDIAN);

            result.put(header.array());

            for (int i = 0; i < savePoints; i++) {
                TrackPoint pt = simplifiedTrackPoints.get(i);
                result.putDouble(pt.lon);
                result.putDouble(pt.lat);
                result.putInt(pt.deltaXMeters);
                result.putInt(pt.deltaYMeters);
                result.putInt(pt.pointIndex);
                result.putInt(pt.distance);
                result.putShort((short) 0); // unknown
                result.put(pt.type);
                result.put((byte) 0); // unknown
                result.put((byte) 0); // unknown
                result.put((byte) 0); // unknown
                result.put((byte) 0); // unknown
                result.put((byte) 0); // unknown
            }

            return result.array();
        }
    }

    void calculateLineEquation(final TrackPoint p1, final TrackPoint p2) {
        if ((p1 == null) || (p2 == null)) {
            return;
        }
        int deltaX = p1.deltaXMeters - p2.deltaXMeters;
        if (Math.abs(deltaX) < 1) {
            slope = (float) Math.tan(DEG_90_RADIAN);
        } else {
            slope = (float) (p1.deltaYMeters - p2.deltaYMeters) / (float) deltaX;
        }
        intercept = (float) p1.deltaYMeters - slope * (float) p1.deltaXMeters;
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt((double) (x1 - x2) * (double) (x1 - x2) + (double) (y1 - y2) * (double) (y1 - y2));
    }

    private int getDistanceBetweenPoints(TrackPoint p1, TrackPoint p2) {
        return (int) calculateDistance(p1.deltaXMeters, p1.deltaYMeters, p2.deltaXMeters, p2.deltaYMeters);
    }

    private long getDistanceFromLine(TrackPoint point) {
        //double numerator = Math.abs((slope * point.deltaXMeters - point.deltaYMeters) + intercept);
        double numerator = Math.abs(-slope * point.deltaXMeters + point.deltaYMeters - intercept);
        double denominator = Math.sqrt(slope * slope + 1.0);
        return Math.round(numerator / denominator);
    }

    void addAndSimplifyPoint(final TrackPoint point, byte pointType) {
        TrackPoint toAdd = new TrackPoint(point);
        toAdd.type = pointType;

        simplifiedTrackPoints.add(toAdd);

        int size = simplifiedTrackPoints.size();
        if (size > maxPoints && size > 3) {
            int selIdx = -1;
            int minDistance = Integer.MAX_VALUE;
            for (int i = 1; i < size - 2; i++) {
                TrackPoint p1 = simplifiedTrackPoints.get(i);
                TrackPoint p2 = simplifiedTrackPoints.get(i + 1);

                int dist = getDistanceBetweenPoints(p1, p2);
                if (dist < minDistance) {
                    selIdx = i;
                    minDistance = dist;
                }
            }

            if ((selIdx > -1) && (selIdx < size - 1)) {
                TrackPoint p1 = simplifiedTrackPoints.get(selIdx);
                TrackPoint p2 = simplifiedTrackPoints.get(selIdx + 1);

                byte type = (p1.type == 1) ? p1.type : p2.type;

                p1.lon = (p1.lon + p2.lon) / 2;
                p1.lat = (p1.lat + p2.lat) / 2;
                p1.deltaXMeters = (p2.deltaXMeters + p1.deltaXMeters) / 2;
                p1.deltaYMeters = (p2.deltaYMeters + p1.deltaYMeters) / 2;
                p1.pointIndex = (p1.pointIndex + p2.pointIndex) / 2;
                p1.distance = (p1.distance + p2.distance) / 2;
                p1.type = type;
                simplifiedTrackPoints.remove(selIdx + 1);
            }
        }
    }

    void processTrackSegment() {
        if (((currentTrackPoint.pointIndex - previousTrackPoint.pointIndex) > POINT_INDEX_THRESHOLD) &&
                (getDistanceBetweenPoints(previousTrackPoint, currentTrackPoint) > POINT_DISTANCE_THRESHOLD)) {

            if (getDistanceBetweenPoints(intermediateTrackPoint, previousTrackPoint) > DISTANCE_THRESHOLD) {
                if (getDistanceBetweenPoints(refTrackPoint, intermediateTrackPoint) > DISTANCE_THRESHOLD) {
                    calculateLineEquation(refTrackPoint, intermediateTrackPoint);
                    refTrackPoint.setFromPoint(intermediateTrackPoint);
                    addAndSimplifyPoint(intermediateTrackPoint, (byte) 3);
                }
            }
            calculateLineEquation(refTrackPoint, previousTrackPoint);
            refTrackPoint.setFromPoint(previousTrackPoint);
            addAndSimplifyPoint(refTrackPoint, (byte) 8);

            calculateLineEquation(refTrackPoint, currentTrackPoint);
            refTrackPoint.setFromPoint(currentTrackPoint);
            addAndSimplifyPoint(refTrackPoint, (byte) 9);

            intermediateTrackPoint.setFromPoint(currentTrackPoint);
            maxDeviationFromLine = 0;
            return;
        }
        int distanceFromReference = getDistanceBetweenPoints(currentTrackPoint, refTrackPoint);
        if (maxDeviationFromLine - distanceFromReference > DISTANCE_THRESHOLD) {
            calculateLineEquation(refTrackPoint, intermediateTrackPoint);
            refTrackPoint.setFromPoint(intermediateTrackPoint);
            addAndSimplifyPoint(intermediateTrackPoint, (byte) 4);

            calculateLineEquation(intermediateTrackPoint, currentTrackPoint);
            maxDeviationFromLine = getDistanceBetweenPoints(intermediateTrackPoint, currentTrackPoint);
            intermediateTrackPoint.setFromPoint(currentTrackPoint);
            return;
        }
        if (maxDeviationFromLine < distanceFromReference) {
            intermediateTrackPoint.setFromPoint(currentTrackPoint);
            maxDeviationFromLine = distanceFromReference;
        }
        if (getDistanceFromLine(currentTrackPoint) <= POINT_DISTANCE_THRESHOLD) {
            return;
        }
        if (getDistanceBetweenPoints(currentTrackPoint, intermediateTrackPoint) > POINT_DISTANCE_THRESHOLD) {
            calculateLineEquation(refTrackPoint, intermediateTrackPoint);
            refTrackPoint.setFromPoint(intermediateTrackPoint);
            addAndSimplifyPoint(intermediateTrackPoint, (byte) 3);
        }

        calculateLineEquation(refTrackPoint, currentTrackPoint);
        refTrackPoint.setFromPoint(currentTrackPoint);
        addAndSimplifyPoint(currentTrackPoint, (byte) 2);

        intermediateTrackPoint.setFromPoint(currentTrackPoint);
        maxDeviationFromLine = 0;
    }

    public static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double prevLatRad = lat1 * DEGREES_TO_RADIANS;
        double curLatRad = lat2 * DEGREES_TO_RADIANS;
        double deltaLatHalfSin = Math.sin((prevLatRad - curLatRad) / 2);
        double deltaLonHalfSin = Math.sin(((lon1 * DEGREES_TO_RADIANS) - (lon2 * DEGREES_TO_RADIANS)) / 2);
        double a = deltaLatHalfSin * deltaLatHalfSin + Math.cos(prevLatRad) * Math.cos(curLatRad) * deltaLonHalfSin * deltaLonHalfSin;
        double centralAngle = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return centralAngle * EARTH_RADIUS_METERS;
    }

    public boolean isLatLonNotValid(double lat, double lon) {
        return (lat < -90.0) || (lat > 90.0) || (lon < -180.0) || (lon > 180.0);
    }

    public byte[] convertTrackToBin(HuaweiRouteTrack track, int maxPointNumber) {
        if (track.getPoints() == null) {
            return new byte[0];
        }

        int pointsNum = track.getPoints().size();

        if (pointsNum < 2) {
            return new byte[0];
        }

        if ((track.getVersion() == 1) && track.isHasAltitude()) {
            return new byte[0];
        }

        this.maxPoints = maxPointNumber;

        // validate and set base point as a first point on track
        if (track.getPoints().get(0).getLatitude() == 0.0) {
            return new byte[0];
        }

        if (isLatLonNotValid(track.getPoints().get(0).getLatitude(), track.getPoints().get(0).getLongitude())) {
            return new byte[0];
        }

        baseTrackPoint.lon = track.getPoints().get(0).getLongitude();
        baseTrackPoint.lat = track.getPoints().get(0).getLatitude();
        baseTrackPoint.deltaXMeters = 0;
        baseTrackPoint.deltaYMeters = 0;
        baseTrackPoint.pointIndex = 0;
        baseTrackPoint.distance = 0;

        previousTrackPoint.setFromPoint(baseTrackPoint);
        currentTrackPoint.setFromPoint(baseTrackPoint);

        accumulatedSegmentDistance = 0;

        int processingState = 1;

        for (int i = 1; i < pointsNum; i++) {
            double currentLat = track.getPoints().get(i).getLatitude();
            double currentLon = track.getPoints().get(i).getLongitude();
            if (isLatLonNotValid(currentLat, currentLon)) {
                continue;
            }
            double lonDiff = Math.abs(currentLon - previousTrackPoint.lon);
            // Skip points that have invalid longitude jumps
            // this check does not work on Poles :)
            if (Math.min(lonDiff, 360.0 - lonDiff) > 10.0) {
                continue;  // Skip if actual jump > 10 degrees
            }

            previousTrackPoint.setFromPoint(currentTrackPoint);

            double deltaLong = currentLon - baseTrackPoint.lon;
            double deltaLat = currentLat - baseTrackPoint.lat;

            currentTrackPoint.lon = currentLon;
            currentTrackPoint.lat = currentLat;
            currentTrackPoint.deltaXMeters = (int) (deltaLong * METERS_PER_DEGREE * Math.cos(baseTrackPoint.lat * DEGREES_TO_RADIANS));
            currentTrackPoint.deltaYMeters = (int) (deltaLat * METERS_PER_DEGREE);
            currentTrackPoint.pointIndex = i;
            accumulatedSegmentDistance += getDistanceBetweenPoints(previousTrackPoint, currentTrackPoint);
            currentTrackPoint.distance = (accumulatedSegmentDistance + 5) / 10; // Rounded distance

            if (processingState == 1) {
                if (Math.abs(baseTrackPoint.lon - currentLon) <= COORDINATE_TOLERANCE / Math.cos(currentLat * DEGREES_TO_RADIANS)) {
                    if (Math.abs(baseTrackPoint.lat - currentLat) <= COORDINATE_TOLERANCE) {
                        continue;
                    }
                    slope = (float) Math.tan(DEG_90_RADIAN);
                } else {
                    slope = (float) ((baseTrackPoint.lat - currentLat) / (baseTrackPoint.lon - currentLon));
                }

                addAndSimplifyPoint(baseTrackPoint, (byte) 1);
                intercept = 0;
                maxDeviationFromLine = 0;

                refTrackPoint.setFromPoint(baseTrackPoint);
                intermediateTrackPoint.setFromPoint(baseTrackPoint);
                processingState = 2;
            }
            processTrackSegment();
        }

        if (currentTrackPoint.pointIndex != 0) {
            addAndSimplifyPoint(currentTrackPoint, (byte) 4);
            // last point can be simplified. Update distance.
            if (!simplifiedTrackPoints.isEmpty()) {
                simplifiedTrackPoints.get(simplifiedTrackPoints.size() - 1).distance = currentTrackPoint.distance;
            }
        }

        lat_arr = new double[pointsNum];
        lon_arr = new double[pointsNum];
        alt_arr = new double[pointsNum];
        cumulativeDistanceArr = new double[pointsNum];
        cumulative3DDistanceArr = new double[pointsNum];
        cumulativeAscentArr = new double[pointsNum];
        cumulativeDescentArr = new double[pointsNum];

        double prevLon = 0.0;
        double prevLat = 0.0;

        cumulativeDistanceArr[0] = 0.0;

        for (int i = 0; i < pointsNum; i++) {
            HuaweiRouteTrack.TrackPoint rp = track.getPoints().get(i);
            double curLon = rp.getLongitude();
            double curLat = rp.getLatitude();
            lat_arr[i] = curLat;
            lon_arr[i] = curLon;
            alt_arr[i] = rp.getAltitude();

            if (i > 0) {
                cumulativeDistanceArr[i] = cumulativeDistanceArr[i - 1] + haversineDistance(prevLat, prevLon, curLat, curLon);
            }
            prevLon = curLon;
            prevLat = curLat;
        }

        // TODO: filter and/or interpolate lat_arr, lon_arr and alt_arr. Remove broken points.

        totalDistance = cumulativeDistanceArr[pointsNum - 1];
        total3DDistance = totalDistance;

        if ((track.getVersion() != 1) && track.isHasAltitude()) {

            cumulative3DDistanceArr[0] = 0.0;
            cumulativeAscentArr[0] = 0.0;
            cumulativeDescentArr[0] = 0.0;
            for (int i = 1; i < pointsNum; i++) {
                double dist = cumulativeDistanceArr[i] - cumulativeDistanceArr[i - 1];
                double deltaAlt = alt_arr[i] - alt_arr[i - 1];
                double dist3D = Math.sqrt(dist * dist + deltaAlt * deltaAlt);
                cumulative3DDistanceArr[i] = cumulative3DDistanceArr[i - 1] + dist3D;
                if (deltaAlt <= 0.0) {
                    cumulativeDescentArr[i] = cumulativeDescentArr[i - 1] + deltaAlt;
                } else {
                    cumulativeAscentArr[i] = cumulativeAscentArr[i - 1] + deltaAlt;
                }
            }
            total3DDistance = cumulative3DDistanceArr[pointsNum - 1];

            double totalAscent = cumulativeAscentArr[pointsNum - 1];
            double totalDescent = cumulativeDescentArr[pointsNum - 1];

            ByteBuffer buf = ByteBuffer.allocate(3384);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            buf.putInt(0); //unknown
            buf.putInt((int) (totalDistance * 10.0));
            buf.putInt((int) (total3DDistance * 10.0));
            buf.putInt((int) (totalAscent * 10.0));
            buf.putInt((int) (totalDescent * -10.0));
            buf.putInt(0); //unknown

            buf.putInt((int) Math.round(alt_arr[0] * 10.0));
            buf.putInt(0);  //unknown
            buf.putDouble(lon_arr[0]);
            buf.putDouble(lat_arr[0]);
            buf.putInt((int) (cumulativeDistanceArr[0] * 10.0));
            buf.putInt((int) (cumulative3DDistanceArr[0] * 10.0));
            buf.putInt((int) (cumulativeAscentArr[0] * 10.0));
            buf.putInt((int) (cumulativeDescentArr[0] * -10.0));

            final int TARGET_POINTS = 83;

            int currentElevation = (int) (int) Math.round(alt_arr[0] * 10.0);
            double currentDistance = cumulativeDistanceArr[0];
            double segmentIncrement = (totalDistance / TARGET_POINTS) / 10.0;

            int currentPoint = 1;
            double currentLat = lat_arr[0];
            double currentLon = lon_arr[0];
            int currentDist = (int) (cumulativeDistanceArr[0] * 10.0);
            int currentDist3d = (int) (cumulative3DDistanceArr[0] * 10.0);
            int currentAsc = (int) (cumulativeAscentArr[0] * 10.0);
            int currentDesc = (int) (cumulativeDescentArr[0] * -10.0);

            for (int i = 1; i < TARGET_POINTS; i++) {
                double targetDistance = (totalDistance / TARGET_POINTS) * (double) i;
                int closestIdx = currentPoint;
                int nextIdx = pointsNum - 1;
                // Find the closest point in route to target distance
                if (currentPoint < pointsNum) {
                    double minDiff = Double.MAX_VALUE;//Math.abs(currentDistance - targetDistance);
                    for (int searchIdx = currentPoint; searchIdx < pointsNum; searchIdx++) {
                        double searchIdxDist = cumulativeDistanceArr[searchIdx];
                        double diff = Math.abs(searchIdxDist - targetDistance);

                        if (diff < minDiff) {
                            closestIdx = searchIdx;
                            minDiff = diff;
                        }

                        if (searchIdxDist > targetDistance) {
                            nextIdx = searchIdx;
                            break;
                        }
                    }
                    currentDistance = cumulativeDistanceArr[closestIdx];
                }

                // Check if interpolation is needed
                if ((targetDistance - segmentIncrement > currentDistance) ||
                        (segmentIncrement + targetDistance < currentDistance)) {
                    // Linear interpolation between points
                    double distDiff = cumulativeDistanceArr[nextIdx] * 10.0 - (double) currentDist;
                    double interpolation_ratio = (targetDistance * 10.0 - (double) currentDist) / distDiff;

                    // Interpolate all values
                    currentLat += (lat_arr[nextIdx] - currentLat) * interpolation_ratio;
                    currentLon += (lon_arr[nextIdx] - currentLon) * interpolation_ratio;

                    currentElevation += (int) ((alt_arr[nextIdx] * 10.0 - (double) currentElevation) * interpolation_ratio);
                    currentDist += (int) (distDiff * interpolation_ratio);
                    currentDist3d += (int) (interpolation_ratio * (cumulative3DDistanceArr[nextIdx] * 10.0 - (double) currentDist3d));
                    currentAsc += (int) (interpolation_ratio * (cumulativeAscentArr[nextIdx] * 10.0 - (double) currentAsc));
                    currentDesc += (int) (interpolation_ratio * (cumulativeDescentArr[nextIdx] * -10.0 - (double) currentDesc));

                } else {
                    // Use exact point
                    currentLat = lat_arr[closestIdx];
                    currentLon = lon_arr[closestIdx];
                    currentPoint = closestIdx + 1;

                    currentElevation = (int) Math.round(alt_arr[closestIdx] * 10.0);
                    currentDist = (int) (cumulativeDistanceArr[closestIdx] * 10.0);
                    currentDist3d = (int) (cumulative3DDistanceArr[closestIdx] * 10.0);
                    currentAsc = (int) (cumulativeAscentArr[closestIdx] * 10.0);
                    currentDesc = (int) (cumulativeDescentArr[closestIdx] * -10.0);
                }

                // Store data
                buf.putInt(currentElevation);
                buf.putInt(0); // unknown
                buf.putDouble(currentLon);
                buf.putDouble(currentLat);
                buf.putInt(currentDist);
                buf.putInt(currentDist3d);
                buf.putInt(currentAsc);
                buf.putInt(currentDesc);

                currentDistance = cumulativeDistanceArr[closestIdx];
            }
            // Last point
            buf.putInt((int) Math.round(alt_arr[pointsNum - 1] * 10.0));
            buf.putInt(0); // unknown
            buf.putDouble(lon_arr[pointsNum - 1]);
            buf.putDouble(lat_arr[pointsNum - 1]);
            buf.putInt((int) (totalDistance * 10.0));
            buf.putInt((int) (total3DDistance * 10.0));
            buf.putInt((int) (totalAscent * 10.0));
            buf.putInt((int) (totalDescent * -10.0));
            altitudeResultBuffer = buf.array();
        }

        return serializeTrackForDevice(track);
    }

}