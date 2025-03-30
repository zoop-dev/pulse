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
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.Point;
import org.mapsforge.core.model.Rotation;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MultiMapDataStore;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public final class MapsManager {
    private static final Logger LOG = LoggerFactory.getLogger(MapsManager.class);

    public static final String PREF_MAPS_FOLDER = "maps_folder";

    private final Context mContext;

    private boolean isMapLoaded = false;

    public MapsManager(final Context context) {
        this.mContext = context;
    }

    public void loadMaps(final MapView mapView) {
        AndroidGraphicFactory.createInstance(GBApplication.app());
        GBPrefs prefs = GBApplication.getPrefs();

        String folderUri = prefs.getString(PREF_MAPS_FOLDER, "");
        if (folderUri.isEmpty()) {
            return;
        }

        final DocumentFile folder = DocumentFile.fromTreeUri(mContext, Uri.parse(folderUri));
        if (folder == null || folder.listFiles().length == 0) {
            return;
        }

        MultiMapDataStore multiMapDataStore = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);

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
                FileInputStream inputStream = (FileInputStream) mContext.getContentResolver().openInputStream(documentFile.getUri());
                assert inputStream != null;
                MapFile mapFile = new MapFile(inputStream, 0, null);
                multiMapDataStore.addMapDataStore(mapFile, true, true);
                isMapLoaded = true;
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        TileCache tileCache = AndroidUtil.createTileCache(mContext, "mapcache",
                mapView.getModel().displayModel.getTileSize(), 1f,
                mapView.getModel().frameBufferModel.getOverdrawFactor());

        TileRendererLayer tileRendererLayer = new TileRendererLayer(
                tileCache, multiMapDataStore,
                mapView.getModel().mapViewPosition, true, false, false, AndroidGraphicFactory.INSTANCE) {
            @Override
            public void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint, Rotation rotation) {
                if (!isMapLoaded) {
                    canvas.fillColor(GBApplication.getWindowBackgroundColor(mapView.getContext()));
                }
                super.draw(boundingBox, zoomLevel, canvas, topLeftPoint, rotation);
            }
        };
        tileRendererLayer.setXmlRenderTheme(MapTheme.DEFAULT);

        mapView.getLayerManager().getLayers().add(tileRendererLayer);
    }

    public boolean isMapLoaded() {
        return isMapLoaded;
    }
}
