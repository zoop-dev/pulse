/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import android.os.Handler;

import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager;

public class ZeppOsMusicService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsMusicService.class);

    private static final short ENDPOINT = 0x001b;

    private static final byte CMD_MEDIA_INFO = 0x03;
    private static final byte CMD_APP_STATE = 0x04;
    private static final byte CMD_BUTTON_PRESS = 0x05;
    private static final byte MUSIC_APP_OPEN = 0x01;
    private static final byte MUSIC_APP_CLOSE = 0x02;
    private static final byte BUTTON_PLAY = 0x00;
    private static final byte BUTTON_PAUSE = 0x01;
    private static final byte BUTTON_NEXT = 0x03;
    private static final byte BUTTON_PREVIOUS = 0x04;
    private static final byte BUTTON_VOLUME_UP = 0x05;
    private static final byte BUTTON_VOLUME_DOWN = 0x06;

    private final Handler handler = new Handler();
    @Nullable
    private MediaManager mediaManager;
    protected boolean isMusicAppStarted = false;

    public ZeppOsMusicService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_APP_STATE:
                switch (payload[1]) {
                    case MUSIC_APP_OPEN:
                        onMusicAppOpen();
                        break;
                    case MUSIC_APP_CLOSE:
                        onMusicAppClosed();
                        break;
                    default:
                        LOG.warn("Unexpected music app state {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;

            case CMD_BUTTON_PRESS:
                LOG.info("Got music button press");
                final GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                switch (payload[1]) {
                    case BUTTON_PLAY:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAY;
                        break;
                    case BUTTON_PAUSE:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PAUSE;
                        break;
                    case BUTTON_NEXT:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
                        break;
                    case BUTTON_PREVIOUS:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                        break;
                    case BUTTON_VOLUME_UP:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEUP;
                        break;
                    case BUTTON_VOLUME_DOWN:
                        deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.VOLUMEDOWN;
                        break;
                    default:
                        LOG.warn("Unexpected music button {}", String.format("0x%02x", payload[1]));
                        return;
                }
                evaluateGBDeviceEvent(deviceEventMusicControl);
                return;
            default:
                LOG.warn("Unexpected music byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        handler.removeCallbacksAndMessages(null);
        isMusicAppStarted = false;

        if (mediaManager == null) {
            mediaManager = new MediaManager(getContext());
        }
    }

    @Override
    public void dispose() {
        handler.removeCallbacksAndMessages(null);
    }

    public void onSetMusicState(final MusicStateSpec stateSpec) {
        if (mediaManager != null && mediaManager.onSetMusicState(stateSpec) && isMusicAppStarted) {
            sendMusicState(null, mediaManager.getBufferMusicStateSpec());
        }
    }

    public void onSetMusicInfo(final MusicSpec musicSpec) {
        if (mediaManager != null && mediaManager.onSetMusicInfo(musicSpec) && isMusicAppStarted) {
            sendMusicState(mediaManager.getBufferMusicSpec(), mediaManager.getBufferMusicStateSpec());
        }
    }

    private void onMusicAppOpen() {
        isMusicAppStarted = true;
        sendMusicStateDelayed();
    }

    private void onMusicAppClosed() {
        LOG.info("Music app terminated");
        isMusicAppStarted = false;
    }

    private void sendMusicState(final MusicSpec musicSpec,
                                final MusicStateSpec musicStateSpec) {
        LOG.info("Sending music: {}, {}", musicSpec, musicStateSpec);

        // TODO: Encode not playing state (flag 0x20, single 0x01 byte before volume)
        final byte[] cmd = ArrayUtils.addAll(
                new byte[]{CMD_MEDIA_INFO},
                HuamiSupport.encodeMusicState(getContext(), musicSpec, musicStateSpec, false)
        );

        write("send music state", cmd);
    }

    /**
     * Send the music state after a small delay. If we send it right as the app notifies us that it opened,
     * it won't be recognized.
     */
    private void sendMusicStateDelayed() {
        handler.postDelayed(() -> {
            if (mediaManager != null) {
                mediaManager.refresh();
                sendMusicState(mediaManager.getBufferMusicSpec(), mediaManager.getBufferMusicStateSpec());
                sendVolume(mediaManager.getPhoneVolume());
            }
        }, 100);
    }

    public void sendVolume(final float volume) {
        LOG.info("Sending volume: {}", volume);

        final byte[] cmd = ArrayUtils.addAll(
                new byte[]{CMD_MEDIA_INFO},
                HuamiSupport.encodeMusicState(getContext(), null, null, true)
        );

        write("send volume", cmd);
    }
}
