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
import android.text.TextUtils;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.activities.musicmanager.MusicManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusic;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceMusicPlaylist;

public class GBDeviceMusicData extends GBDeviceEvent {
    public int type = 0; // 1 - sync start, 2 - music list, 10 - end sync
    public List<GBDeviceMusic> list = null;
    public List<GBDeviceMusicPlaylist> playlists = null;
    public String deviceInfo = null;
    public int maxMusicCount = 0;
    public int maxPlaylistCount = 0;

    @Override
    public void evaluate(final Context context, final GBDevice device) {
        final Intent intent = new Intent(MusicManagerActivity.ACTION_MUSIC_DATA);

        intent.putExtra("type", this.type);

        if (this.list != null) {
            final ArrayList<GBDeviceMusic> list = new ArrayList<>(this.list);
            intent.putExtra("musicList", list);
        }

        if (this.playlists != null) {
            final ArrayList<GBDeviceMusicPlaylist> list = new ArrayList<>(this.playlists);
            intent.putExtra("musicPlaylist", list);
        }

        if (!TextUtils.isEmpty(this.deviceInfo)) {
            intent.putExtra("deviceInfo", this.deviceInfo);
        }

        if (this.maxMusicCount > 0) {
            intent.putExtra("maxMusicCount", this.maxMusicCount);
        }
        if (this.maxPlaylistCount > 0) {
            intent.putExtra("maxPlaylistCount", this.maxPlaylistCount);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
