/*  Copyright (C) 2024 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.activities.musicmanager.MusicManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class GBDeviceMusicUpdate extends GBDeviceEvent {
    public boolean success = false;
    public int operation = -1;
    public int playlistIndex = -1;
    public String playlistName;
    public ArrayList<Integer> musicIds = null;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        final Intent intent = new Intent(MusicManagerActivity.ACTION_MUSIC_UPDATE);

        intent.putExtra("success", success);
        intent.putExtra("operation", operation);
        intent.putExtra("playlistIndex", playlistIndex);
        intent.putExtra("playlistName", playlistName);
        intent.putExtra("musicIds", musicIds);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
