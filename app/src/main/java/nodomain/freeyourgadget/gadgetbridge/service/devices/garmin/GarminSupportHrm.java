/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import android.content.Intent;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate.HeartRateProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class GarminSupportHrm extends GarminSupport {
    private final BatteryInfoProfile<GarminSupportHrm> batteryInfoProfile;
    private final HeartRateProfile<GarminSupportHrm> heartRateProfile;

    public GarminSupportHrm() {
        addSupportedService(BatteryInfoProfile.SERVICE_UUID);
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(new BatteryListener());
        addSupportedProfile(batteryInfoProfile);

        addSupportedService(GattService.UUID_SERVICE_HEART_RATE);
        heartRateProfile = new HeartRateProfile<>(this);
        heartRateProfile.addListener(new HeartRateListener());
        addSupportedProfile(heartRateProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(final TransactionBuilder builder) {
        super.initializeDevice(builder);

        if (null != getCharacteristic(BatteryInfoProfile.UUID_CHARACTERISTIC_BATTERY_LEVEL)) {
            batteryInfoProfile.requestBatteryInfo(builder);
            batteryInfoProfile.enableNotify(builder, true);

        }

        if (null != getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT)) {
            heartRateProfile.enableNotify(builder, true);
        }

        return builder;
    }

    final class BatteryListener implements IntentListener {
        @Override
        public void notify(Intent intent) {
            BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
            if (info != null) {
                GBDeviceEventBatteryInfo event = new GBDeviceEventBatteryInfo();
                event.state = BatteryState.BATTERY_NORMAL;
                event.level = info.getPercentCharged();
                handleGBDeviceEvent(event);
            }
        }
    }

    @Override
    public void onHeartRateTest() {
        // noop - device publishes HR ca every second
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(final boolean enable) {
        // noop - HR is always enabled
    }

    final class HeartRateListener implements IntentListener {
        @Override
        public void notify(Intent intent) {
            int hr = intent.getIntExtra(HeartRateProfile.EXTRA_HEART_RATE, -1);
            if (hr > 0) {
                final GarminActivitySample sample;
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();
                    final GarminActivitySampleProvider provider = new GarminActivitySampleProvider(getDevice(), session);

                    final Device device = DBHelper.getDevice(gbDevice, session);
                    final User user = DBHelper.getUser(session);
                    sample = provider.createActivitySample();

                    sample.setActiveCalories(ActivitySample.NOT_MEASURED);
                    sample.setDevice(device);
                    sample.setDistanceCm(ActivitySample.NOT_MEASURED);
                    sample.setHeartRate(hr);
                    sample.setRawIntensity(ActivitySample.NOT_MEASURED);
                    sample.setRawKind(ActivityKind.UNKNOWN.getCode());
                    sample.setSteps(ActivitySample.NOT_MEASURED);
                    sample.setTimestamp((int) (System.currentTimeMillis() / 1000));
                    sample.setUser(user);

                    provider.addGBActivitySample(sample);
                } catch (Exception e) {
                    GB.toast(getContext(), "Error saving activity samples", Toast.LENGTH_LONG, GB.ERROR, e);
                    return;
                }

                publish(sample);
            }
        }

        private void publish(final GarminActivitySample sample) {
            final Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES);
            intent.setPackage(BuildConfig.APPLICATION_ID);
            intent.putExtra(GBDevice.EXTRA_DEVICE, getDevice());
            intent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }
}
