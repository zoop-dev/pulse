package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums.GarminSport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitCourse;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileCreator;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLap;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxFile;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.model.GpxTrackPoint;

public class GpxRouteFileConverter {
    private static final Logger LOG = LoggerFactory.getLogger(GpxRouteFileConverter.class);
    final double speed = 1.4; // m/s // TODO: make this configurable (and activity dependent?)
    final int activity = GarminSport.GENERIC.getType(); //TODO: make this configurable
    private final long timestamp;
    private final GpxFile gpxFile;
    private FitFile convertedFile;
    private final String name;

    public GpxRouteFileConverter(@NonNull final GpxFile gpxFile,
                                 @NonNull final String trackName,
                                 @Nullable final Date date) {
        this.timestamp = ((date == null) ? System.currentTimeMillis() : date.getTime()) / 1000L;
        this.gpxFile = gpxFile;
        this.name = trackName;
        try {
            this.convertedFile = convertGpxToRoute(gpxFile);
        } catch (final Exception e) {
            LOG.error("Failed to convert gpx to route", e);
            this.convertedFile = null;
        }
    }

    private static FitFileCreator getFileCreatorRecordData() {
        final FitFileCreator.Builder fileCreatorBuilder = new FitFileCreator.Builder();
        fileCreatorBuilder.setSoftwareVersion(1);
        return fileCreatorBuilder.build(0x01);
    }

    public FitFile getConvertedFile() {
        return convertedFile;
    }

    public boolean isValid() {
        return this.convertedFile != null;
    }

    private FitFile convertGpxToRoute(GpxFile gpxFile) {
        if (gpxFile.getTracks().isEmpty()) {
            LOG.error("Gpx file contains no Tracks.");
            return null;
        }

        // GPX files may contain multiple tracks, we use only the first one,
        // but we use all segments (#4855)
        final List<GpxTrackPoint> gpxTrackPointList = gpxFile.getTracks().get(0)
                .getTrackSegments().stream()
                .flatMap(segment -> segment.getTrackPoints().stream())
                .collect(Collectors.toList());

        if (gpxTrackPointList.isEmpty()) {
            LOG.error("Gpx track contains no points");
            return null;
        }

        final List<RecordData> gpxPointDataRecords = new ArrayList<>();

        double totalAscent = 0;
        double totalDescent = 0;
        double totalDistance = 0;
        long runningTs = timestamp;

        GPSCoordinate prevPoint = gpxTrackPointList.get(0);

        for (GpxTrackPoint point :
                gpxTrackPointList) {
            totalAscent += point.getAscent(prevPoint);
            totalDescent += point.getDescent(prevPoint);
            totalDistance += point.getDistance(prevPoint);
            runningTs += (long) (point.getDistance(prevPoint) / speed);

            final FitRecord.Builder recordBuilder = new FitRecord.Builder();
            recordBuilder.setTimestamp(runningTs);
            recordBuilder.setLatitude(point.getLatitude());
            recordBuilder.setLongitude(point.getLongitude());
            recordBuilder.setDistance(totalDistance);

            if(point.hasAltitude()){
                recordBuilder.setAltitude((float) point.getAltitude());
            }

            final double depth = point.getDepth();
            if(Double.isFinite(depth)) {
                recordBuilder.setDepth(depth);
            }

            final float temperature = point.getTemperature();
            if(Float.isFinite(temperature)) {
                recordBuilder.setTemperature((int) temperature);
            }

            final float speed = point.getSpeed();
            if(Float.isFinite(speed) && speed > 0.0f) {
                recordBuilder.setSpeed(speed);
            }

            prevPoint = point;
            gpxPointDataRecords.add(recordBuilder.build(0x05));
        }

        final FitLap.Builder lapRecordBuilder = getLapRecordData(gpxTrackPointList);
        lapRecordBuilder.setTotalDistance(totalDistance);
        lapRecordBuilder.setTotalAscent((int) Math.round(totalAscent));
        lapRecordBuilder.setTotalDescent((int) Math.round(totalDescent));
        lapRecordBuilder.setTotalElapsedTime((double) (runningTs - timestamp));
        lapRecordBuilder.setTotalTimerTime((double) (runningTs - timestamp));

        final List<RecordData> courseFileDataRecords = new ArrayList<>();
        courseFileDataRecords.add(getFileIdRecordData());
        courseFileDataRecords.add(getFileCreatorRecordData());
        courseFileDataRecords.add(getCourseRecordData());
        courseFileDataRecords.add(lapRecordBuilder.build(0x03));

        courseFileDataRecords.add(getEventRecordData(timestamp, 0));
        courseFileDataRecords.add(getEventRecordData(runningTs, 9));

        courseFileDataRecords.addAll(gpxPointDataRecords);

        return new FitFile(courseFileDataRecords);
    }

    private FitEvent getEventRecordData(long timestamp, int eventType) {
        final FitEvent.Builder eventBuilder = new FitEvent.Builder();
        eventBuilder.setTimestamp(timestamp);
        eventBuilder.setEvent(0);
        eventBuilder.setEventGroup(0);
        eventBuilder.setEventType(eventType);
        return eventBuilder.build(0x04);
    }

    private FitLap.Builder getLapRecordData(List<GpxTrackPoint> gpxTrackPointList) {
        final GPSCoordinate first = gpxTrackPointList.get(0);
        final GPSCoordinate last = gpxTrackPointList.get(gpxTrackPointList.size() - 1);

        final FitLap.Builder lapBuilder = new FitLap.Builder();
        lapBuilder.setStartLat(first.getLatitude());
        lapBuilder.setStartLong(first.getLongitude());
        lapBuilder.setEndLat(last.getLatitude());
        lapBuilder.setEndLong(last.getLongitude());
        lapBuilder.setTimestamp(timestamp);
        lapBuilder.setMessageIndex(0);
        lapBuilder.setStartTime(timestamp);
        lapBuilder.setSport(activity);
        return lapBuilder;
    }

    private FitCourse getCourseRecordData() {
        final FitCourse.Builder courseBuilder = new FitCourse.Builder();
        courseBuilder.setSport(activity); //TODO use track.getType()
        courseBuilder.setName(name);
        return courseBuilder.build(0x02);
    }

    private FitFileId getFileIdRecordData() {
        final FitFileId.Builder fileIdBuilder = new FitFileId.Builder();
        fileIdBuilder.setType(FileType.FILETYPE.COURSES);
        fileIdBuilder.setManufacturer(1);
        fileIdBuilder.setProduct(65534);
        fileIdBuilder.setTimeCreated(timestamp);
        fileIdBuilder.setSerialNumber(1L);
        fileIdBuilder.setNumber(1);
        fileIdBuilder.setProductName("Gadgetbridge");
        return fileIdBuilder.build(0x00);
    }

}
