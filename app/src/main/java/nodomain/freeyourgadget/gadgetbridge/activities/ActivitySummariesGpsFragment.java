/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Polyline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackActivity;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager;

import static android.graphics.Bitmap.createBitmap;


public class ActivitySummariesGpsFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesGpsFragment.class);
    private final int CANVAS_SIZE = 360;
    private MapView mapView;
    private File inputFile;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gps, container, false);
        mapView = rootView.findViewById(R.id.activitygpsview);
        //mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(false);

        MapsManager mapsManager = new MapsManager(requireContext());
        mapsManager.loadMaps(mapView);

        if (inputFile != null) {
            processInBackgroundThread();
        }
        return rootView;
    }

    public void set_data(File inputFile) {
        this.inputFile = inputFile;
        if (mapView != null) { //first fragment inflate is AFTER this is called
            processInBackgroundThread();
        }
    }

    private void processInBackgroundThread() {
        new Thread(() -> {
            final ArrayList<GPSCoordinate> points = new ArrayList<>();
            if (inputFile.getName().endsWith(".gpx")) {
                try (FileInputStream inputStream = new FileInputStream(inputFile)) {
                    final GpxParser gpxParser = new GpxParser(inputStream);
                    points.addAll(gpxParser.getGpxFile().getPoints());
                } catch (final IOException e) {
                    LOG.error("Failed to open {}", inputFile, e);
                    return;
                } catch (final GpxParseException e) {
                    LOG.error("Failed to parse gpx file", e);
                    return;
                }
            } else if (inputFile.getName().endsWith(".fit")) {
                try {
                    FitFile fitFile = FitFile.parseIncoming(inputFile);
                    for (final RecordData record : fitFile.getRecords()) {
                        if (record instanceof FitRecord) {
                            final ActivityPoint activityPoint = ((FitRecord) record).toActivityPoint();
                            if (activityPoint.getLocation() != null) {
                                points.add(activityPoint.getLocation());
                            }
                        }
                    }
                } catch (final IOException e) {
                    LOG.error("Failed to open {}", inputFile, e);
                    return;
                } catch (final Exception e) {
                    LOG.error("Failed to parse fit file", e);
                    return;
                }
            } else {
                LOG.warn("Unknown file type {}", inputFile.getName());
                return;
            }

            if (!points.isEmpty()) {
                drawTrack(mapView, points);
            }
        }).start();
    }

    private void drawTrack(MapView mapView, final ArrayList<? extends GPSCoordinate> trackPoints) {
        double maxLat = (Collections.max(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double minLat = (Collections.min(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double maxLon = (Collections.max(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        double minLon = (Collections.min(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        List<LatLong> latlongs = trackPoints.stream()
                .map(p -> new LatLong(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());

        Paint p = AndroidGraphicFactory.INSTANCE.createPaint();
        p.setColor(R.color.hrv_status_char_line_color);
        Polyline polyline = new Polyline(p, AndroidGraphicFactory.INSTANCE);
        polyline.setPoints(latlongs);
        mapView.addLayer(polyline);

        mapView.setCenter(new LatLong(minLat + (maxLat - minLat) / 2, minLon + (maxLon - minLon) / 2));
        mapView.setZoomLevel((byte) 12);

        mapView.setOnTouchListener((a, b) -> {
            final Intent startIntent = new Intent(requireContext(), MapsTrackActivity.class);
            startIntent.putParcelableArrayListExtra("points", trackPoints);
            requireContext().startActivity(startIntent);
            return true;
        });
        mapView.setOnClickListener(v -> {
            final Intent startIntent = new Intent(requireContext(), MapsTrackActivity.class);
            startIntent.putParcelableArrayListExtra("points", trackPoints);
            requireContext().startActivity(startIntent);
        });
    }

    private Canvas createCanvas(ImageView imageView) {
        Bitmap bitmap = createBitmap(CANVAS_SIZE, CANVAS_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(GBApplication.getWindowBackgroundColor(requireActivity()));
        //frame around, but it doesn't look so nice
        /*
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getResources().getColor(R.color.chart_activity_light));
        canvas.drawRect(0,0,360,360,paint);
         */
        imageView.setImageBitmap(bitmap);
        imageView.setScaleY(-1f); //flip the canvas

        return canvas;
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return null;
    }
}
