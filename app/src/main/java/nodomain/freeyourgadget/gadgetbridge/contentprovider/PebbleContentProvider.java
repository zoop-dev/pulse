/*  Copyright (C) 2016-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.contentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Comparator;
import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class PebbleContentProvider extends ContentProvider {

    public static final int COLUMN_CONNECTED = 0;
    public static final int COLUMN_APPMSG_SUPPORT = 1;
    public static final int COLUMN_DATALOGGING_SUPPORT = 2;
    public static final int COLUMN_VERSION_MAJOR = 3;
    public static final int COLUMN_VERSION_MINOR = 4;
    public static final int COLUMN_VERSION_POINT = 5;
    public static final int COLUMN_VERSION_TAG = 6;

    // this is only needed for the MatrixCursor constructor
    public static final String[] columnNames = new String[]{"0", "1", "2", "3", "4", "5", "6"};

    static final String PROVIDER_NAME = "com.getpebble.android.provider";
    static final String URL = "content://" + PROVIDER_NAME + "/state";
    static final Uri CONTENT_URI = Uri.parse(URL);

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        if (uri.equals(CONTENT_URI)) {
            MatrixCursor mc = new MatrixCursor(columnNames);
            int connected = 0;
            int pebbleKit = 0;
            String fwString = "unknown";

            final Optional<GBDevice> pebbleDeviceOpt = GBApplication.app().getDeviceManager().getSelectedDevices()
                    .stream().filter(d -> d.getType() == DeviceType.PEBBLE)
                    .sorted(Comparator.comparing(GBDevice::getState).thenComparing(GBDevice::getAliasOrName).reversed())
                    .findFirst();

            if (pebbleDeviceOpt.isPresent()) {
                final GBDevice pebbleDevice = pebbleDeviceOpt.get();
                final DevicePrefs deviceSpecificSharedPrefsrefs = GBApplication.getDevicePrefs(pebbleDevice);
                if (deviceSpecificSharedPrefsrefs.getBoolean("pebble_enable_pebblekit", false)) {
                    pebbleKit = 1;
                }
                connected = pebbleDevice.isInitialized() ? 1 : 0;
                fwString = pebbleDevice.getFirmwareVersion();
            }
            mc.addRow(new Object[]{connected, pebbleKit, pebbleKit, 3, 8, 2, fwString});

            return mc;
        } else {
            return null;
        }
    }

    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
