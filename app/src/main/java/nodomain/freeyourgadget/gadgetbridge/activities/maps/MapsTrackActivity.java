/*  Copyright (C) 2024 José Rebelo, a0z

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
package nodomain.freeyourgadget.gadgetbridge.activities.maps;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.overlay.Polyline;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesGpsFragment;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.maps.MapsManager;

public class MapsTrackActivity extends AbstractGBActivity {
    private MapView mapView;
    private File file;
    public static boolean isActivityOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps_track);

        if (isActivityOpen) {
            finish();
            return;
        }
        isActivityOpen = true;

        mapView = findViewById(R.id.activitygpsview);
        MapsManager mapsManager = new MapsManager(this);
        mapsManager.loadMaps(mapView);

        file = (File) getIntent().getExtras().get("file");
        final List<GPSCoordinate> trackPoints = ActivitySummariesGpsFragment.getActivityPoints(file)
                .stream()
                .map(ActivityPoint::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (trackPoints.isEmpty()) {
            return;
        }

        double maxLat = (Collections.max(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double minLat = (Collections.min(trackPoints, new GPSCoordinate.compareLatitude())).getLatitude();
        double maxLon = (Collections.max(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        double minLon = (Collections.min(trackPoints, new GPSCoordinate.compareLongitude())).getLongitude();
        List<LatLong> latlongs = trackPoints.stream()
                .map(p -> new LatLong(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());

        Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(getResources().getColor(R.color.hrv_status_low));
        paint.setStrokeWidth(8);
        paint.setStyle(Style.STROKE);
        Polyline polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);
        polyline.setPoints(latlongs);
        mapView.addLayer(polyline);
        mapView.getLayerManager().redrawLayers();

        mapView.setCenter(new LatLong(minLat + (maxLat - minLat) / 2, minLon + (maxLon - minLon) / 2));
        byte zoom = LatLongUtils.zoomForBounds(new Dimension(this.getResources().getDisplayMetrics().widthPixels, this.getResources().getDisplayMetrics().heightPixels), new BoundingBox(minLat, minLon, maxLat, maxLon), mapView.getModel().displayModel.getTileSize());
        mapView.setZoomLevel(zoom);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle state) {
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        synchronized (MapsTrackActivity.class) {
            isActivityOpen = false;
        }
    }

}
