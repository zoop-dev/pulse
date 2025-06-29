/*  Copyright (C) 2025 José Rebelo

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

import android.location.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProviderType;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.externalevents.opentracks.OpenTracksController;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiPhoneGpsStatus;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsActivityType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;

public class ZeppOsWorkoutService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsWorkoutService.class);

    private static final short ENDPOINT = 0x0019;

    public static final byte CMD_GPS_LOCATION = 0x04;
    public static final byte CMD_STATUS = 0x11;
    public static final byte CMD_APP_OPEN = 0x20;

    public static final byte STATUS_START = 0x01;
    public static final byte STATUS_END = 0x04;

    /**
     * Track whether the currently selected workout needs gps, so we can start the activity tracking
     * if needed in, since in there we don't know what's the current workout.
     */
    private boolean workoutNeedsGps = false;

    /**
     * Track the {@link nodomain.freeyourgadget.gadgetbridge.model.ActivityKind} that was opened,
     * for the same reasons as {@code workoutNeedsGps}.
     */
    private ActivityKind workoutActivityKind = ActivityKind.UNKNOWN;

    /**
     * Track the last time we actually sent a gps location. We need to signal that GPS as re-acquired
     * if the last update was too long ago.
     */
    private long lastPhoneGpsSent = 0;

    public ZeppOsWorkoutService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        workoutNeedsGps = false;
        workoutActivityKind = ActivityKind.UNKNOWN;
        lastPhoneGpsSent = 0;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_APP_OPEN:
                final ZeppOsActivityType activityType = ZeppOsActivityType.fromCode(payload[3]);
                final boolean workoutNeedsGps = (payload[2] == 1);
                final ActivityKind activityKind;

                if (activityType == null) {
                    LOG.warn("Unknown workout activity type {}", String.format("0x%x", payload[3]));
                    activityKind = ActivityKind.UNKNOWN;
                } else {
                    activityKind = activityType.toActivityKind();
                }

                LOG.info("Workout starting on band: {}, needs gps = {}", activityType, workoutNeedsGps);

                onWorkoutOpen(workoutNeedsGps, activityKind);
                return;
            case CMD_STATUS:
                switch (payload[1]) {
                    case STATUS_START:
                        LOG.info("Workout Start");
                        onWorkoutStart();
                        break;
                    case STATUS_END:
                        LOG.info("Workout End");
                        onWorkoutEnd();
                        break;
                    default:
                        LOG.warn("Unexpected workout status {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;
            default:
        }

        LOG.warn("Unexpected workout byte {}", String.format("0x%02x", payload[0]));
    }

    public void onSetGpsLocation(final Location location) {
        final boolean sendGpsToBand = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_SEND_GPS_TO_BAND, false);
        if (!sendGpsToBand) {
            LOG.warn("Sending GPS to band is disabled, ignoring location update");
            return;
        }

        boolean newGpsLock = System.currentTimeMillis() - lastPhoneGpsSent > 5000;
        lastPhoneGpsSent = System.currentTimeMillis();

        final HuamiPhoneGpsStatus status = newGpsLock ? HuamiPhoneGpsStatus.ACQUIRED : null;

        sendPhoneGps(status, location);
    }

    public void sendPhoneGps(final HuamiPhoneGpsStatus status, final Location location) {
        final byte[] locationBytes = HuamiUtils.encodePhoneGpsPayload(status, location);

        final ByteBuffer buf = ByteBuffer.allocate(2 + locationBytes.length);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put(CMD_GPS_LOCATION);
        buf.put((byte) 0x00); // ?
        buf.put(locationBytes);

        write("send phone gps", buf.array());
    }

    private void onWorkoutOpen(final boolean needsGps, final ActivityKind activityKind) {
        this.workoutNeedsGps = needsGps;
        this.workoutActivityKind = activityKind;
        final boolean sendGpsToBand = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_SEND_GPS_TO_BAND, false);

        if (workoutNeedsGps) {
            if (sendGpsToBand) {
                lastPhoneGpsSent = 0;
                sendPhoneGps(HuamiPhoneGpsStatus.SEARCHING, null);
                GBLocationService.start(getContext(), getSupport().getDevice(), GBLocationProviderType.GPS, 1000);
            } else {
                sendPhoneGps(HuamiPhoneGpsStatus.DISABLED, null);
            }
        }
    }

    private void onWorkoutStart() {
        final boolean startOnPhone = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_START_ON_PHONE, false);

        if (workoutNeedsGps && startOnPhone) {
            LOG.info("Starting OpenTracks recording");

            OpenTracksController.startRecording(getContext(), workoutActivityKind);
        }
    }

    private void onWorkoutEnd() {
        final boolean startOnPhone = getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_WORKOUT_START_ON_PHONE, false);

        GBLocationService.stop(getContext(), getSupport().getDevice());

        if (startOnPhone) {
            LOG.info("Stopping OpenTracks recording");
            OpenTracksController.stopRecording(getContext());
        }
    }
}
