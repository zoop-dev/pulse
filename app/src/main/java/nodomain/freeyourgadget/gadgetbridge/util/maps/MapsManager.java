/*  Copyright (C) 2024-2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.maps;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Dimension;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;
import nodomain.freeyourgadget.gadgetbridge.util.Accumulator;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public final class MapsManager {
    private static final Logger LOG = LoggerFactory.getLogger(MapsManager.class);

    public static final String PREF_MAPS_FOLDER = "maps_folder";
    public static final String PREF_TRACK_COLOR = "maps_track_color";
    public static final String PREF_MAP_THEME = "maps_theme";

    private final Context mContext;
    private final MapView mapView;
    private Polyline polyline;

    private TileRendererLayer tileRendererLayer;
    private boolean tileRenderedAdded;

    private boolean isMapLoaded = false;

    public MapsManager(final Context context, final MapView mapView) {
        this.mContext = context;
        this.mapView = mapView;
    }

    public void loadMaps() {
        if (tileRendererLayer != null) {
            mapView.getLayerManager().getLayers().remove(tileRendererLayer);
            tileRendererLayer.onDestroy();
            tileRendererLayer.getTileCache().purge();
            tileRendererLayer = null;
        }

        mapView.getModel().displayModel.setBackgroundColor(GBApplication.getWindowBackgroundColor(mapView.getContext()));

        isMapLoaded = false;

        AndroidGraphicFactory.createInstance(GBApplication.app());
        final GBPrefs prefs = GBApplication.getPrefs();

        final DocumentFile[] documentFiles;
        final String folderUri = prefs.getString(PREF_MAPS_FOLDER, "");
        if (!folderUri.isEmpty()) {
            final DocumentFile folder = DocumentFile.fromTreeUri(mContext, Uri.parse(folderUri));
            documentFiles = folder != null ? folder.listFiles() : new DocumentFile[0];
        } else {
            documentFiles = new DocumentFile[0];
        }

        LOG.debug("Got {} map files", documentFiles.length);

        final MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);

        for (final DocumentFile documentFile : documentFiles) {
            if (!documentFile.canRead()) {
                continue;
            }
            assert documentFile.getName() != null;
            if (!documentFile.getName().endsWith(".map")) {
                continue;
            }

            LOG.debug("Loading {}", documentFile.getName());

            try {
                final FileInputStream inputStream = (FileInputStream) mContext.getContentResolver().openInputStream(documentFile.getUri());
                if (inputStream == null) {
                    throw new IOException("Failed to open input stream for " + documentFile.getName());
                }
                final MapFile mapFile = new MapFile(inputStream, 0, null);
                multiMapDataStore.addMapDataStore(mapFile, true, true);
                isMapLoaded = true;
            } catch (final Exception e) {
                LOG.error("Failed to load map file", e);
            }
        }

        final TileCache tileCache = AndroidUtil.createTileCache(
                mContext,
                "mapcache",
                mapView.getModel().displayModel.getTileSize(),
                1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor()
        );

        tileRendererLayer = new TileRendererLayer(
                tileCache,
                multiMapDataStore,
                mapView.getModel().mapViewPosition,
                true,
                false,
                false,
                AndroidGraphicFactory.INSTANCE
        );

        final String themePrefValue = prefs.getString(PREF_MAP_THEME, "default").toUpperCase(Locale.ROOT);
        MapTheme theme;
        try {
            theme = MapTheme.valueOf(themePrefValue);
        } catch (final Exception e) {
            LOG.error("Failed to find theme {}", themePrefValue, e);
            theme = MapTheme.DEFAULT;
        }
        tileRendererLayer.setXmlRenderTheme(theme);
        // Do not add the tile renderer layer before setting the bounding box in setTrack,
        // otherwise we load the entire map to memory and might crash with OOM
    }

    public boolean isMapLoaded() {
        return isMapLoaded;
    }

    public void onDestroy() {
        if (polyline != null) {
            mapView.getLayerManager().getLayers().remove(polyline);
            polyline.onDestroy();
            polyline = null;
        }

        if (tileRendererLayer != null) {
            if (tileRenderedAdded) {
                mapView.getLayerManager().getLayers().remove(tileRendererLayer);
            }
            tileRendererLayer.onDestroy();
            tileRendererLayer.getTileCache().purge();
            tileRendererLayer = null;
        }

        isMapLoaded = false;
    }

    public void setTrack(final List<? extends GPSCoordinate> trackPoints) {
        final Accumulator latitudeAccumulator = new Accumulator();
        final Accumulator longitudeAccumulator = new Accumulator();
        for (GPSCoordinate trackPoint : trackPoints) {
            latitudeAccumulator.add(trackPoint.getLatitude());
            longitudeAccumulator.add(trackPoint.getLongitude());
        }
        final double maxLat = latitudeAccumulator.getMax();
        final double minLat = latitudeAccumulator.getMin();
        final double maxLon = longitudeAccumulator.getMax();
        final double minLon = longitudeAccumulator.getMin();

        final List<LatLong> points = trackPoints.stream()
                .map(p -> new LatLong(p.getLatitude(), p.getLongitude()))
                .collect(Collectors.toList());

        if (polyline == null) {
            final Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
            final int trackColor = GBApplication.getPrefs().getInt(MapsManager.PREF_TRACK_COLOR, ContextCompat.getColor(mContext, R.color.map_track_default));
            paint.setColor(trackColor);
            paint.setStrokeWidth(8);
            paint.setStyle(Style.STROKE);

            polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);
            mapView.addLayer(polyline);
        }
        polyline.setPoints(points);

        mapView.setCenter(new LatLong(minLat + (maxLat - minLat) / 2, minLon + (maxLon - minLon) / 2));
        final byte zoom = LatLongUtils.zoomForBounds(
                new Dimension(mapView.getWidth(), mapView.getHeight()),
                new BoundingBox(minLat, minLon, maxLat, maxLon),
                mapView.getModel().displayModel.getTileSize()
        );
        mapView.setZoomLevel(zoom);

        if (!tileRenderedAdded) {
            mapView.getLayerManager().getLayers().add(0, tileRendererLayer);
            tileRenderedAdded = true;
        }
    }

    public void reload() {
        if (polyline != null) {
            final int trackColor = GBApplication.getPrefs().getInt(MapsManager.PREF_TRACK_COLOR, ContextCompat.getColor(mContext, R.color.map_track_default));
            polyline.getPaintStroke().setColor(trackColor);
            polyline.requestRedraw();
        }

        loadMaps();
    }
}
