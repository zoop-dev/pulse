/*  Copyright (C) 2024 José Rebelo

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

import androidx.documentfile.provider.DocumentFile;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.overlay.Polyline;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
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

        isMapLoaded = false;

        AndroidGraphicFactory.createInstance(GBApplication.app());
        final GBPrefs prefs = GBApplication.getPrefs();

        final String folderUri = prefs.getString(PREF_MAPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            return;
        }

        final DocumentFile folder = DocumentFile.fromTreeUri(mContext, Uri.parse(folderUri));
        if (folder == null || folder.listFiles().length == 0) {
            return;
        }

        final MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);

        final DocumentFile[] documentFiles = folder.listFiles();

        LOG.debug("Got {} map files", documentFiles.length);

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

        tileRendererLayer = new MyTileRendererLayer(
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

        mapView.getLayerManager().getLayers().add(0, tileRendererLayer);
    }

    public boolean isMapLoaded() {
        return isMapLoaded;
    }

    public void setTrack(final List<LatLong> points) {
        if (polyline == null) {
            final Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
            final int trackColor = GBApplication.getPrefs().getInt(MapsManager.PREF_TRACK_COLOR, mContext.getResources().getColor(R.color.map_track_default));
            paint.setColor(trackColor);
            paint.setStrokeWidth(8);
            paint.setStyle(Style.STROKE);

            polyline = new Polyline(paint, AndroidGraphicFactory.INSTANCE);
            mapView.addLayer(polyline);
        }
        polyline.setPoints(points);
        mapView.getLayerManager().redrawLayers();
    }

    public void reload() {
        if (polyline != null) {
            final int trackColor = GBApplication.getPrefs().getInt(MapsManager.PREF_TRACK_COLOR, mContext.getResources().getColor(R.color.map_track_default));
            polyline.getPaintStroke().setColor(trackColor);
            polyline.requestRedraw();
        }

        loadMaps();
    }

    private class MyTileRendererLayer extends TileRendererLayer {
        public MyTileRendererLayer(final TileCache tileCache,
                                   final MapDataStore mapDataStore,
                                   final MapViewPosition mapViewPosition,
                                   final boolean isTransparent,
                                   final boolean renderLabels,
                                   final boolean cacheLabels,
                                   final GraphicFactory graphicFactory) {
            super(tileCache, mapDataStore, mapViewPosition, isTransparent, renderLabels, cacheLabels, graphicFactory);
        }

        @Override
        public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
            if (!isMapLoaded) {
                canvas.fillColor(GBApplication.getWindowBackgroundColor(mapView.getContext()));
            }
            super.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
        }
    }
}
