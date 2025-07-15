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

import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventSleepStateDetection;
import nodomain.freeyourgadget.gadgetbridge.model.SleepState;
import nodomain.freeyourgadget.gadgetbridge.service.SleepAsAndroidSender;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.RealtimeSamplesAggregator;

public class ZeppOsHeartRateService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsHeartRateService.class);

    private static final short ENDPOINT = 0x001d;

    public static final byte CMD_REALTIME_SET = 0x04;
    public static final byte CMD_REALTIME_ACK = 0x05;
    public static final byte CMD_SLEEP = 0x06;

    public static final byte SLEEP_EVENT_FALL_ASLEEP = 0x01;
    public static final byte SLEEP_EVENT_WAKE_UP = 0x00;

    public static final byte REALTIME_MODE_STOP = 0x00;
    public static final byte REALTIME_MODE_START = 0x01;
    public static final byte REALTIME_MODE_CONTINUE = 0x02;

    private final Handler realtimeHandler = new Handler();
    private SleepAsAndroidSender sleepAsAndroidSender;
    private RealtimeSamplesAggregator realtimeSamplesAggregator;

    // Tracks whether realtime HR monitoring is already started, so we can just
    // send CONTINUE commands
    private boolean realtimeStarted = false;
    private boolean realtimeOneShot = false;

    public ZeppOsHeartRateService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        switch (payload[0]) {
            case CMD_REALTIME_ACK:
                // what does the status mean? Seems to be 0 on success
                LOG.info("Band acknowledged heart rate command, status = {}", payload[1]);
                return;
            case CMD_SLEEP:
                switch (payload[1]) {
                    case SLEEP_EVENT_FALL_ASLEEP:
                        LOG.info("Fell asleep");
                        evaluateGBDeviceEvent(new GBDeviceEventSleepStateDetection(SleepState.ASLEEP));
                        break;
                    case SLEEP_EVENT_WAKE_UP:
                        LOG.info("Woke up");
                        evaluateGBDeviceEvent(new GBDeviceEventSleepStateDetection(SleepState.AWAKE));
                        break;
                    default:
                        LOG.warn("Unexpected sleep byte {}", String.format("0x%02x", payload[1]));
                        break;
                }
                return;
        }

        LOG.warn("Unexpected heart rate byte {}", String.format("0x%02x", payload[0]));
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        realtimeHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void dispose() {
        realtimeHandler.removeCallbacksAndMessages(null);
    }

    public void setSleepAsAndroidSender(final SleepAsAndroidSender sleepAsAndroidSender) {
        this.sleepAsAndroidSender = sleepAsAndroidSender;
    }

    public void setRealtimeSamplesAggregator(final RealtimeSamplesAggregator realtimeSamplesAggregator) {
        this.realtimeSamplesAggregator = realtimeSamplesAggregator;
    }

    public void onHeartRateTest() {
        LOG.debug("Trigger heart rate one-shot test");

        realtimeStarted = true;
        realtimeOneShot = true;

        final ZeppOsTransactionBuilder builder = createTransactionBuilder("HeartRateTest");
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, true);
        write(builder, new byte[]{CMD_REALTIME_SET, REALTIME_MODE_START});
        builder.queue();

        realtimeHandler.removeCallbacksAndMessages(null);
        scheduleContinue();
    }

    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        LOG.debug("Enable realtime hr: {}", enable);

        if (enable == realtimeStarted) {
            // same state, ignore
            return;
        }

        final byte hrCmd = enable ? REALTIME_MODE_START : REALTIME_MODE_STOP;

        realtimeStarted = enable;
        realtimeOneShot = false;

        final ZeppOsTransactionBuilder builder = createTransactionBuilder("set realtime heart rate measurement = " + enable);
        builder.notify(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT, enable);
        write(builder, new byte[]{CMD_REALTIME_SET, hrCmd});
        builder.queue();

        realtimeHandler.removeCallbacksAndMessages(null);
        if (enable) {
            scheduleContinue();
        }
    }

    public void handleHeartRate(final byte[] value) {
        if (!realtimeOneShot && !realtimeStarted) {
            // Failsafe in case it gets out of sync, stop it
            onEnableRealtimeHeartRateMeasurement(false);
            return;
        }

        if (value.length == 2 && value[0] == 0) {
            final int hrValue = (value[1] & 0xff);
            LOG.debug("Real-time hr: {}", hrValue);
            if (realtimeSamplesAggregator != null) {
                realtimeSamplesAggregator.broadcastHeartRate(hrValue);
            }
            if (sleepAsAndroidSender != null) {
                sleepAsAndroidSender.onHrChanged(hrValue, 0);
            }
        }

        if (realtimeOneShot) {
            onEnableRealtimeHeartRateMeasurement(false);
        }
    }

    private void scheduleContinue() {
        realtimeHandler.postDelayed(() -> {
            sendContinue();
            scheduleContinue();
        }, 1000L);
    }

    private void sendContinue() {
        write("hr continue", new byte[]{CMD_REALTIME_SET, REALTIME_MODE_CONTINUE});
    }
}
