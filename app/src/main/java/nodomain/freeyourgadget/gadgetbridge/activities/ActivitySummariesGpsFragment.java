/*  Copyright (C) 2020-2024 José Rebelo, Petr Vaněk, a0z

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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsSettingsFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.maps.MapsTrackActivity;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitRecord;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParseException;
import nodomain.freeyourgadget.gadgetbridge.util.gpx.GpxParser;
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager;



public class ActivitySummariesGpsFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesGpsFragment.class);

    private MapView mapView;
    private File inputFile;
    private MapsManager mapsManager;

    private boolean mapValid = true;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (MapsSettingsFragment.ACTION_SETTING_CHANGE.equals(intent.getAction())) {
                if (!isResumed()) {
                    mapValid = false;
                } else {
                    mapsManager.reload();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gps, container, false);
        TextView gpsWarning = rootView.findViewById(R.id.gpsWarning);
        mapView = rootView.findViewById(R.id.activitygpsview);
        mapView.setBuiltInZoomControls(false);

        mapsManager = new MapsManager(requireContext(), mapView);
        mapsManager.loadMaps();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(MapsSettingsFragment.ACTION_SETTING_CHANGE);
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(mReceiver, intentFilter);

        if (mapsManager.isMapLoaded()) {
            gpsWarning.setVisibility(View.GONE);
        }

        if (inputFile != null) {
            processInBackgroundThread();
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!mapValid) {
            mapsManager.reload();
        }
    }

    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(mReceiver);
        mapsManager.onDestroy();
        super.onDestroyView();
    }

    public void set_data(File inputFile) {
        this.inputFile = inputFile;
        if (mapView != null) { //first fragment inflate is AFTER this is called
            processInBackgroundThread();
        }
    }

    private void processInBackgroundThread() {
        new Thread(() -> {
            final List<GPSCoordinate> points = getActivityPoints(inputFile)
                    .stream()
                    .map(ActivityPoint::getLocation)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!points.isEmpty()) {
                drawTrack(mapView, (ArrayList<? extends GPSCoordinate>) points);
            }
        }).start();
    }

    public static List<ActivityPoint> getActivityPoints(final File trackFile) {
        final List<ActivityPoint> points = new ArrayList<>();
        if (trackFile == null) {
            return points;
        }
        if (trackFile.getName().endsWith(".gpx")) {
            try (FileInputStream inputStream = new FileInputStream(trackFile)) {
                final GpxParser gpxParser = new GpxParser(inputStream);
                points.addAll(gpxParser.getGpxFile().getActivityPoints());
            } catch (final IOException e) {
                LOG.error("Failed to open {}", trackFile, e);
            } catch (final GpxParseException e) {
                LOG.error("Failed to parse gpx file", e);
            }
        } else if (trackFile.getName().endsWith(".fit")) {
            try {
                FitFile fitFile = FitFile.parseIncoming(trackFile);
                for (final RecordData record : fitFile.getRecords()) {
                    if (record instanceof FitRecord) {
                        points.add(((FitRecord) record).toActivityPoint());
                    }
                }
            } catch (final IOException e) {
                LOG.error("Failed to open {}", trackFile, e);
            } catch (final Exception e) {
                LOG.error("Failed to parse fit file", e);
            }
        } else {
            LOG.warn("Unknown file type {}", trackFile.getName());
        }

        return points;
    }

    private void drawTrack(MapView mapView, final ArrayList<? extends GPSCoordinate> trackPoints) {
        double maxLat = (Collections.max(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double minLat = (Collections.min(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double maxLon = (Collections.max(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        double minLon = (Collections.min(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        List<LatLong> latlongs = trackPoints.stream()
                .map(p -> new LatLong(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());

        mapsManager.setTrack(latlongs);

        final Model model = mapView.getModel();
        // FIXME: We need to offset the min latitude so the track gets centered - not sure why
        final BoundingBox boundingBox = new BoundingBox(minLat - (maxLat - minLat) / 4, minLon, maxLat, maxLon);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int height = (int) (300 * displayMetrics.density);
        byte zoom = LatLongUtils.zoomForBounds(new Dimension(screenWidth, height), boundingBox, model.displayModel.getTileSize());
        model.mapViewPosition.setMapPosition(new MapPosition(boundingBox.getCenterPoint(), zoom));

        final View.OnTouchListener controlTouchListener = new View.OnTouchListener() {
            private float startX, startY;
            private static final int TAP_THRESHOLD = 10;
            private static final int LONG_PRESS_TIME = 300;
            private long pressTime;
            private boolean isDrag = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isDrag = false;
                        startX = event.getX();
                        startY = event.getY();
                        pressTime = System.currentTimeMillis();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (Math.abs(startX - event.getX()) > TAP_THRESHOLD || Math.abs(startY - event.getY()) > TAP_THRESHOLD) {
                            isDrag = true;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (!isDrag && System.currentTimeMillis() - pressTime < LONG_PRESS_TIME) {
                            openMapActivity();
                        }
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return true;
            }

            private void openMapActivity() {
                Intent intent = new Intent(getContext(), MapsTrackActivity.class);
                intent.putExtra("file", inputFile);
                startActivity(intent);
            }
        };
        mapView.setOnTouchListener(controlTouchListener);

        mapView.invalidate();
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return null;
    }

}
