/*  Copyright (C) 2022-2024 Noodlez

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
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import android.content.Context;
import android.media.AudioManager;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;

/**
 * An adapter class for the media commands sent by AsteroidOS
 */
public class AsteroidOSMediaCommand {
    public static final byte COMMAND_PREVIOUS = 0x0;
    public static final byte COMMAND_NEXT = 0x1;
    public static final byte COMMAND_PLAY = 0x2;
    public static final byte COMMAND_PAUSE = 0x3;
    public static final byte COMMAND_VOLUME = 0x4;

    public byte command;
    public byte[] raw_values;
    public Context context;

    public AsteroidOSMediaCommand(byte[] values, Context device_context) {
        command = values[0];
        raw_values = values;
        context = device_context;
    }

    /**
     * Convert the MediaCommand to a music control event
     *
     * @return the matching music control event
     */
    public GBDeviceEventMusicControl toMusicControlEvent() {
        GBDeviceEventMusicControl event = new GBDeviceEventMusicControl();
        switch (command) {
            case COMMAND_PREVIOUS:
                event.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            case COMMAND_NEXT:
                event.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case COMMAND_PLAY:
                event.event = GBDeviceEventMusicControl.Event.PLAY;
                break;
            case COMMAND_PAUSE:
                event.event = GBDeviceEventMusicControl.Event.PAUSE;
                break;
            case COMMAND_VOLUME:
                setVolume(raw_values[1]);
                event = null;
                break;
            default:
                event.event = GBDeviceEventMusicControl.Event.UNKNOWN;
        }
        return event;
    }

    private void setVolume(byte volume) {
        final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        final int volumeMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        final int finalVol = (int) Math.round((volume * volumeMax) / 100f);
        if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) != finalVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (int) Math.round((volume * volumeMax) / 100f), AudioManager.FLAG_SHOW_UI);
    }
}
