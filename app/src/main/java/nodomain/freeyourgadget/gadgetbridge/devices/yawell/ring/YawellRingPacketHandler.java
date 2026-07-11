/*  Copyright (C) 2024-2026 Arjan Schrijver, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring;

import static nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils.formatIso8601;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiHeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiHrvValueSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiSleepSessionSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiSleepStageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.ColmiTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.yawell.ring.samples.ColmiActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiHrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepSessionSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSleepStageSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiSpo2Sample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiStressSample;
import nodomain.freeyourgadget.gadgetbridge.entities.ColmiTemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.yawell.ring.YawellRingDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public final class YawellRingPacketHandler {
    private static final Logger LOG = LoggerFactory.getLogger(YawellRingPacketHandler.class);

    private YawellRingPacketHandler() {
        throw new UnsupportedOperationException("only static members");
    }

    public static void hrIntervalSettings(YawellRingDeviceSupport support, byte[] value) {
        if (value[1] == YawellRingConstants.PREF_WRITE) return;  // ignore empty response when writing setting
        boolean enabled = value[2] == 0x01;
        int minutes = value[3];
        LOG.info("Received HR interval preference: {} minutes, enabled={}", minutes, enabled);
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_MEASUREMENT_INTERVAL,
                String.valueOf(minutes * 60)
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void spo2Settings(YawellRingDeviceSupport support, byte[] value) {
        boolean enabled = value[2] == 0x01;
        LOG.info("Received SpO2 preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void stressSettings(YawellRingDeviceSupport support, byte[] value) {
        boolean enabled = value[2] == 0x01;
        LOG.info("Received stress preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HEARTRATE_STRESS_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void hrvSettings(YawellRingDeviceSupport support, byte[] value) {
        boolean enabled = value[2] == 0x01;
        LOG.info("Received HRV preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_HRV_ALL_DAY_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void tempSettings(YawellRingDeviceSupport support, byte[] value) {
        boolean enabled = value[3] == 0x01;
        LOG.info("Received temperature preference: {}", enabled ? "enabled" : "disabled");
        GBDeviceEventUpdatePreferences eventUpdatePreferences = new GBDeviceEventUpdatePreferences();
        eventUpdatePreferences.withPreference(
                DeviceSettingsPreferenceConst.PREF_TEMPERATURE_ALL_DAY_MONITORING,
                enabled
        );
        support.evaluateGBDeviceEvent(eventUpdatePreferences);
    }

    public static void goalsSettings(byte[] value) {
        int steps = BLETypeConversions.toUint32(value[2], value[3], value[4], (byte) 0);
        int calories = BLETypeConversions.toUint32(value[5], value[6], value[7], (byte) 0);
        int distance = BLETypeConversions.toUint32(value[8], value[9], value[10], (byte) 0);
        int sport = BLETypeConversions.toUint16(value[11], value[12]);
        int sleep = BLETypeConversions.toUint16(value[13], value[14]);
        LOG.info("Received goals preferences: {} steps, {} calories, {}m distance, {}min sport, {}min sleep", steps, calories, distance, sport, sleep);
    }

    public static void liveHeartRate(@NonNull final GBDevice device,
                                     @NonNull final Context context,
                                     @NonNull final byte[] value) {
        int errorCode = value[2];
        int hrResponse = value[3] & 0xff;
        switch (errorCode) {
            case 0:
                LOG.info("Received live heart rate response: {} bpm", hrResponse);
                break;
            case 1:
                GB.toast(context.getString(R.string.smart_ring_measurement_error_worn_incorrectly), Toast.LENGTH_LONG, GB.ERROR);
                LOG.warn("Live HR error code {} received from ring", errorCode);
                return;
            case 2:
                LOG.warn("Live HR error 2 (temporary error / missing data) received");
                return;
            default:
                GB.toast(String.format(context.getString(R.string.smart_ring_measurement_error_unknown), errorCode), Toast.LENGTH_LONG, GB.ERROR);
                LOG.warn("Live HR error code {} received from ring", errorCode);
                return;
        }
        if (hrResponse > 0) {
            try (DBHandler db = GBApplication.acquireDB()) {
                // Build sample object and save in database
                ColmiHeartRateSampleProvider sampleProvider = new ColmiHeartRateSampleProvider(device, db.getDaoSession());
                ColmiHeartRateSample gbSample = new ColmiHeartRateSample();
                gbSample.setTimestamp(Calendar.getInstance().getTimeInMillis());
                gbSample.setHeartRate(hrResponse);
                sampleProvider.persistSamples(gbSample, context);
                // Send local intent with sample for listeners like the heart rate dialog
                Intent liveIntent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES);
                liveIntent.putExtra(GBDevice.EXTRA_DEVICE, device);
                liveIntent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, gbSample);
                LocalBroadcastManager.getInstance(context)
                        .sendBroadcast(liveIntent);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording heart rate samples", e);
            }
        }
    }

    public static void realtimeHeartRate(@NonNull final GBDevice device,
                                         @NonNull final Context context,
                                         @NonNull final YawellRingLiveActivityContext hrmContext,
                                         @NonNull final byte[] value) {
        int hrResponse = value[1] & 0xff;
        LOG.info("Received realtime heart rate response: {} bpm", hrResponse);

        // Ignore realtime heart rate data if it arrives too fast
        Calendar calendar = Calendar.getInstance();
        int sampleTimestamp = (int)(calendar.getTimeInMillis() / 1000);
        if (sampleTimestamp <= hrmContext.getLastRealtimeHeartRateTimestamp()) {
            LOG.info("Ignoring realtime heart rate data with same timestamp as last packet");
            return;
        }

        hrmContext.setLastRealtimeHeartRateTimestamp(sampleTimestamp);

        if (hrResponse > 0) {
            // Build sample object, send intent and save in database
            try (DBHandler db = GBApplication.acquireDB()) {
                // Build heart rate sample object and save in database
                ColmiHeartRateSampleProvider heartRateSampleProvider = new ColmiHeartRateSampleProvider(device, db.getDaoSession());
                ColmiHeartRateSample heartRateSample = heartRateSampleProvider.createSample();
                heartRateSample.setTimestamp(calendar.getTimeInMillis());
                heartRateSample.setHeartRate(hrResponse);
                heartRateSampleProvider.persistSamples(heartRateSample, context);

                // Send local intent with sample for listeners like the live activity tab
                Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                        .putExtra(GBDevice.EXTRA_DEVICE, device)
                        .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, heartRateSample);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording heart rate samples", e);
            }
        }
    }

    public static void liveActivity(GBDevice device, Context context, YawellRingLiveActivityContext liveActivityContext, byte[] value) {
        // Live activity will report cumulative values over the day
        int steps = BLETypeConversions.toUint32(value[4], value[3], value[2], (byte) 0);
        int calories = BLETypeConversions.toUint32(value[7], value[6], value[5], (byte) 0) / 10;
        int distance = BLETypeConversions.toUint32(value[10], value[9], value[8], (byte) 0);
        LOG.info("Received live activity notification: {} steps, {} calories, {}m distance", steps, calories, distance);


        // Calculate difference to last values
        if (liveActivityContext.getLastTotalSteps() == 0) liveActivityContext.setLastTotalSteps(steps);
        if (liveActivityContext.getLastTotalCalories() == 0) liveActivityContext.setLastTotalCalories(calories);
        if (liveActivityContext.getLastTotalDistance() == 0) liveActivityContext.setLastTotalDistance(distance);

        int deltaSteps = steps - liveActivityContext.getLastTotalSteps();
        int deltaCalories = calories - liveActivityContext.getLastTotalCalories();
        int deltaDistance = distance - liveActivityContext.getLastTotalDistance();

        liveActivityContext.setLastTotalSteps(steps);
        liveActivityContext.setLastTotalCalories(calories);
        liveActivityContext.setLastTotalDistance(distance);


        // Buffer live activity data
        liveActivityContext.setBufferedSteps(liveActivityContext.getBufferedSteps() + deltaSteps);
        liveActivityContext.setBufferedCalories(liveActivityContext.getBufferedCalories() + deltaCalories);
        liveActivityContext.setBufferedDistance(liveActivityContext.getBufferedDistance() + deltaDistance);

        LOG.info("Buffered live activity data: {} steps (+{}), {} calories (+{}), {}m distance (+{})", liveActivityContext.getBufferedSteps(), deltaSteps, liveActivityContext.getBufferedCalories(), deltaCalories, liveActivityContext.getBufferedDistance(), deltaDistance);
    }

    @NonNull
    public static Runnable liveActivityPulse(@NonNull final GBDevice device,
                                             @NonNull final Context context,
                                             @NonNull final YawellRingLiveActivityContext liveActivityContext) {
        return () -> {
            Calendar calendar = Calendar.getInstance();
            int sampleTimestamp = (int) (calendar.getTimeInMillis() / 1000);

            try (DBHandler db = GBApplication.acquireDB()) {
                Long userId = DBHelper.getUser(db.getDaoSession()).getId();
                Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();

                // Build activity sample object
                ColmiActivitySampleProvider sampleProvider = new ColmiActivitySampleProvider(device, db.getDaoSession());
                ColmiActivitySample activitySample = sampleProvider.createActivitySample();
                activitySample.setProvider(sampleProvider);
                activitySample.setDeviceId(deviceId);
                activitySample.setUserId(userId);
                activitySample.setRawKind(ActivityKind.ACTIVITY.getCode());
                activitySample.setTimestamp(sampleTimestamp);
                activitySample.setCalories(liveActivityContext.getBufferedCalories());
                activitySample.setSteps(liveActivityContext.getBufferedSteps());
                activitySample.setDistance(liveActivityContext.getBufferedDistance());

                // Send local intent with sample for listeners like the live activity tab
                Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                        .putExtra(GBDevice.EXTRA_DEVICE, device)
                        .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, activitySample);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
                LOG.info("Sent live activity notification: {} steps, {} calories, {}m distance", liveActivityContext.getBufferedSteps(), liveActivityContext.getBufferedCalories(), liveActivityContext.getBufferedDistance());

                // Reset buffered data
                liveActivityContext.setBufferedSteps(0);
                liveActivityContext.setBufferedCalories(0);
                liveActivityContext.setBufferedDistance(0);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording activity samples", e);
            }
        };
    }

    public static void historicalActivity(@NonNull final GBDevice device,
                                          @NonNull final Context context,
                                          @NonNull final byte[] value) {
        if ((value[1] & 0xff) == 0xff) {
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(context);
            LOG.info("Empty activity history, sync aborted");
        } else if ((value[1] & 0xff) == 0xf0) {
            // initial packet, doesn't contain anything interesting
        } else {
            // Unpack timestamp and data
            Calendar sampleCal = Calendar.getInstance();
            // The code below converts the raw hex value to a date. That seems wrong, but is correct,
            // because this date is for some reason transmitted as ints used as literal bytes:
            // A date like 2024-08-18 would be transmitted as 0x24 0x08 0x18.
            sampleCal.set(Calendar.YEAR, 2000 + Integer.valueOf(String.format("%02x", value[1])));
            sampleCal.set(Calendar.MONTH, Integer.valueOf(String.format("%02x", value[2])) - 1);
            sampleCal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(String.format("%02x", value[3])));
            sampleCal.set(Calendar.HOUR_OF_DAY, value[4] / 4);  // And the hour is transmitted as nth quarter of the day...
            sampleCal.set(Calendar.MINUTE, 0);
            sampleCal.set(Calendar.SECOND, 0);
            sampleCal.set(Calendar.MILLISECOND, 0);
            int calories = BLETypeConversions.toUint16(value[7], value[8]);
            int steps = BLETypeConversions.toUint16(value[9], value[10]);
            int distance = BLETypeConversions.toUint16(value[11], value[12]);
            LOG.info("Received activity sample: {} - {} calories, {} steps, {} distance", formatIso8601(sampleCal), calories, steps, distance);
            // Build sample object and save in database
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiActivitySampleProvider sampleProvider = new ColmiActivitySampleProvider(device, db.getDaoSession());
                ColmiActivitySample gbSample = sampleProvider.createActivitySample();
                gbSample.setRawKind(ActivityKind.ACTIVITY.getCode());
                gbSample.setTimestamp((int) (sampleCal.getTimeInMillis() / 1000));
                gbSample.setCalories(calories);
                gbSample.setSteps(steps);
                gbSample.setDistance(distance);
                sampleProvider.persistSamples(gbSample, context);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording activity samples", e);
            }
            // Determine if this sync is done
            int currentActivityPacket = value[5];
            int totalActivityPackets = value[6];
            if (currentActivityPacket == totalActivityPackets - 1) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(context);
            }
        }
    }

    public static void historicalStress(@NonNull final GBDevice device,
                                        @NonNull final Context context,
                                        @NonNull final byte[] value) {
        final ArrayList<ColmiStressSample> stressSamples = new ArrayList<>();
        int stressPacketNr = value[1] & 0xff;
        if (stressPacketNr == 0xff) {
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(context);
            LOG.info("Empty stress history, sync aborted");
        } else if (stressPacketNr == 0) {
            LOG.info("Received initial stress history response");
        } else {
            Calendar sampleCal = Calendar.getInstance();
            sampleCal.set(Calendar.SECOND, 0);
            sampleCal.set(Calendar.MILLISECOND, 0);
            int startValue = stressPacketNr == 1 ? 3 : 2;  // packet 1 data starts at byte 3, others at byte 2
            int minutesInPreviousPackets = 0;
            if (stressPacketNr > 1) {
                // 30 is the interval in minutes between values/measurements
                minutesInPreviousPackets = 12 * 30;  // 12 values in packet 1
                minutesInPreviousPackets += (stressPacketNr - 2) * 13 * 30;  // 13 values per packet
            }
            for (int i = startValue; i < value.length - 1; i++) {
                final int stress = value[i] & 0xFF;
                if (stress != 0x00) {
                    // Determine time of day
                    int minuteOfDay = minutesInPreviousPackets + (i - startValue) * 30;
                    sampleCal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
                    sampleCal.set(Calendar.MINUTE, minuteOfDay % 60);
                    LOG.info("Stress level is {} at {}", stress, formatIso8601(sampleCal));
                    // Build sample object and save in database
                    ColmiStressSample gbSample = new ColmiStressSample();
                    gbSample.setTimestamp(sampleCal.getTimeInMillis());
                    gbSample.setStress(stress);
                    stressSamples.add(gbSample);
                }
            }
            if (!stressSamples.isEmpty()) {
                try (DBHandler db = GBApplication.acquireDB()) {
                    ColmiStressSampleProvider sampleProvider = new ColmiStressSampleProvider(device, db.getDaoSession());
                    sampleProvider.persistSamples(stressSamples, context);
                } catch (Exception e) {
                    LOG.error("Error acquiring database for recording stress samples", e);
                }
            }
            if (stressPacketNr == 4) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(context);
            }
        }
    }

    public static void historicalSpo2(@NonNull final GBDevice device,
                                      @NonNull final Context context,
                                      @NonNull final byte[] value) {
        ArrayList<ColmiSpo2Sample> spo2Samples = new ArrayList<>();
        int length = BLETypeConversions.toUint16(value[2], value[3]);
        int index = 6; // start of data (day nr, followed by values)
        int spo2_days_ago = -1;
        while (spo2_days_ago != 0 && index - 6 < length) {
            spo2_days_ago = value[index];
            Calendar syncingDay = Calendar.getInstance();
            syncingDay.add(Calendar.DAY_OF_MONTH, 0 - spo2_days_ago);
            syncingDay.set(Calendar.MINUTE, 0);
            syncingDay.set(Calendar.SECOND, 0);
            syncingDay.set(Calendar.MILLISECOND, 0);
            index++;
            for (int hour=0; hour<=23; hour++) {
                syncingDay.set(Calendar.HOUR_OF_DAY, hour);
                float spo2_min = value[index];
                index++;
                float spo2_max = value[index];
                index++;
                if (spo2_min > 0 && spo2_max > 0) {
                    LOG.info("Received SpO2 data from {} days ago at {}:00: min={}, max={}", spo2_days_ago, hour, spo2_min, spo2_max);
                    ColmiSpo2Sample spo2Sample = new ColmiSpo2Sample();
                    spo2Sample.setTimestamp(syncingDay.getTimeInMillis());
                    spo2Sample.setSpo2(Math.round((spo2_min + spo2_max) / 2.0f));
                    spo2Samples.add(spo2Sample);
                }
                if (index - 6 >= length) {
                    break;
                }
            }
        }
        if (!spo2Samples.isEmpty()) {
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiSpo2SampleProvider sampleProvider = new ColmiSpo2SampleProvider(device, db.getDaoSession());
                sampleProvider.persistSamples(spo2Samples, context);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording SpO2 samples", e);
            }
        }
    }

    public static void historicalSleep(@NonNull final GBDevice gbDevice,
                                       @NonNull final Context context,
                                       @NonNull final byte[] value) {
        int packetLength = BLETypeConversions.toUint16(value[2], value[3]);
        if (packetLength < 2) {
            LOG.info("Received empty sleep data packet: {}", StringUtils.bytesToHex(value));
        } else {
            int daysInPacket = value[6];
            LOG.debug("Received sleep data packet for {} days: {}", daysInPacket, StringUtils.bytesToHex(value));
            int index = 7;
            for (int i = 1; i <= daysInPacket; i++) {
                // Parse sleep session
                int daysAgo = value[index];
                index++;
                int dayBytes = value[index];
                index++;
                // sleepStart is received as "minutes after midnight"
                int sleepStart = BLETypeConversions.toUint16(value[index], value[index + 1]);
                index += 2;
                // sleepEnd is received as "minutes after midnight"
                int sleepEnd = BLETypeConversions.toUint16(value[index], value[index + 1]);
                index += 2;
                // Calculate sleep start timestamp
                LOG.info("Sleep session daysAgo={}, dayBytes={}, sleepStart={}, sleepEnd={}", daysAgo, dayBytes, sleepStart, sleepEnd);
                Calendar sessionStart = Calendar.getInstance();
                sessionStart.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
                sessionStart.set(Calendar.HOUR_OF_DAY, 0);
                sessionStart.set(Calendar.MINUTE, 0);
                sessionStart.set(Calendar.SECOND, 0);
                sessionStart.set(Calendar.MILLISECOND, 0);
                if (sleepStart > sleepEnd) {
                    // Sleep started a day earlier, so before midnight
                    sessionStart.add(Calendar.MINUTE, sleepStart - 1440);
                } else {
                    // Sleep started this day, so after midnight
                    sessionStart.add(Calendar.MINUTE, sleepStart);
                }
                // Calculate sleep end timestamp
                Calendar sessionEnd = Calendar.getInstance();
                sessionEnd.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
                sessionEnd.set(Calendar.HOUR_OF_DAY, 0);
                sessionEnd.set(Calendar.MINUTE, sleepEnd);
                sessionEnd.set(Calendar.SECOND, 0);
                sessionEnd.set(Calendar.MILLISECOND, 0);
                LOG.info("Sleep session starts at {} and ends at {}", formatIso8601(sessionStart), formatIso8601(sessionEnd));
                // Build sample object to persist
                final ColmiSleepSessionSample sessionSample = new ColmiSleepSessionSample();
                sessionSample.setTimestamp(sessionStart.getTimeInMillis());
                sessionSample.setWakeupTime(sessionEnd.getTimeInMillis());
                // Handle sleep stages
                final List<ColmiSleepStageSample> stageSamples = new ArrayList<>();
                Calendar sleepStage = (Calendar) sessionStart.clone();
                for (int j = 4; j < dayBytes; j += 2) {
                    int sleepMinutes = value[index + 1];
                    final ColmiSleepStageSample sample = new ColmiSleepStageSample();
                    sample.setTimestamp(sleepStage.getTimeInMillis());
                    sample.setDuration(value[index + 1]);
                    sample.setStage(value[index]);
                    if (sleepMinutes > 0) {
                        LOG.info("Sleep stage type={} starts at {} and lasts for {} minutes", value[index], formatIso8601(sleepStage.getTime()), sleepMinutes);
                        if (sleepStage.getTimeInMillis() + sleepMinutes * 60 * 1000 > sessionEnd.getTimeInMillis()) {
                            LOG.warn("Warning: sleep stage exceeds end of sleep session, received data may be corrupt");
                        }
                        stageSamples.add(sample);
                        sleepStage.add(Calendar.MINUTE, sleepMinutes);
                    } else {
                        LOG.info("Ignoring sleep stage type={} starts at {} and lasts for {} minutes", value[index], formatIso8601(sleepStage.getTime()), sleepMinutes);
                    }
                    // Prepare for next sample
                    index += 2;
                }
                // Persist sleep session
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();

                    final ColmiSleepSessionSampleProvider sampleProvider = new ColmiSleepSessionSampleProvider(gbDevice, session);

                    LOG.debug("Will persist 1 sleep session sample from {} to {}", sessionSample.getTimestamp(), sessionSample.getWakeupTime());
                    sampleProvider.persistSamples(sessionSample, context);
                } catch (final Exception e) {
                    GB.toast(context, "Error saving sleep session sample", Toast.LENGTH_LONG, GB.ERROR, e);
                }
                // Persist sleep stages
                try (DBHandler handler = GBApplication.acquireDB()) {
                    final DaoSession session = handler.getDaoSession();
                    final ColmiSleepStageSampleProvider sampleProvider = new ColmiSleepStageSampleProvider(gbDevice, session);
                    sampleProvider.persistSamples(stageSamples, context);
                } catch (final Exception e) {
                    GB.toast(context, "Error saving sleep stage samples", Toast.LENGTH_LONG, GB.ERROR, e);
                }
            }
        }
    }

    public static void historicalHRV(@NonNull final GBDevice device,
                                     @NonNull final Context context,
                                     @NonNull final byte[] value,
                                     final int daysAgo) {
        LOG.info("Received HRV history sync packet: {}", StringUtils.bytesToHex(value));
        int hrvPacketNr = value[1] & 0xff;
        if (hrvPacketNr == 0xff) {
            LOG.info("Empty HRV history, sync aborted");
            device.unsetBusyTask();
            device.sendDeviceUpdateIntent(context);
        } else if (hrvPacketNr == 0) {
            int packetsTotalNr = value[2];
            LOG.info("HRV history packet {} out of total {}", hrvPacketNr, packetsTotalNr);
        } else {
            LOG.info("HRV history packet {}", hrvPacketNr);
            Calendar sampleCal = Calendar.getInstance();
            if (daysAgo != 0) {
                sampleCal.add(Calendar.DAY_OF_MONTH, 0 - daysAgo);
                sampleCal.set(Calendar.HOUR_OF_DAY, 0);
                sampleCal.set(Calendar.MINUTE, 0);
            }
            sampleCal.set(Calendar.SECOND, 0);
            sampleCal.set(Calendar.MILLISECOND, 0);
            int startValue = hrvPacketNr == 1 ? 3 : 2;  // packet 1 contains something in byte 2
            int minutesInPreviousPackets = 0;
            if (hrvPacketNr > 1) {
                minutesInPreviousPackets = 12 * 30;  // packet 1
                minutesInPreviousPackets += (hrvPacketNr - 2) * 13 * 30;
            }

            final List<ColmiHrvValueSample> samples = new ArrayList<>(value.length);
            for (int i = startValue; i < value.length - 1; i++) {
                final int hrv = value[i] & 0xff;
                if (hrv != 0x00) {
                    // Determine time of day
                    int minuteOfDay = minutesInPreviousPackets + (i - startValue) * 30;
                    sampleCal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
                    sampleCal.set(Calendar.MINUTE, minuteOfDay % 60);
                    LOG.info("Value {} is {} ms, time of day is {}", i, hrv, formatIso8601(sampleCal));
                    // Build sample object and save in database
                    ColmiHrvValueSample gbSample = new ColmiHrvValueSample();
                    gbSample.setTimestamp(sampleCal.getTimeInMillis());
                    gbSample.setValue(hrv);
                    samples.add(gbSample);
                }
            }

            // Save samples in database
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiHrvValueSampleProvider sampleProvider = new ColmiHrvValueSampleProvider(device, db.getDaoSession());
                sampleProvider.persistSamples(samples, context);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording HRV samples", e);
            }

            if (hrvPacketNr == 4) {
                device.unsetBusyTask();
                device.sendDeviceUpdateIntent(context);
            }
        }
    }

    public static void historicalTemperature(@NonNull final GBDevice device,
                                             @NonNull final Context context,
                                             @NonNull final byte[] value) {
        final ArrayList<ColmiTemperatureSample> temperatureSamples = new ArrayList<>();
        int length = BLETypeConversions.toUint16(value[2], value[3]);
        if (length < 50) {
            LOG.info("Received temperature data packet with length {} while expecting 50. Will not try to parse it.", length);
            return;
        }
        int index = 6; // start of data (day nr, followed by values)
        int days_ago = -1;
        while (days_ago != 0 && index - 6 < length) {
            days_ago = value[index];
            Calendar syncingDay = Calendar.getInstance();
            syncingDay.add(Calendar.DAY_OF_MONTH, 0 - days_ago);
            syncingDay.set(Calendar.MINUTE, 0);
            syncingDay.set(Calendar.SECOND, 0);
            syncingDay.set(Calendar.MILLISECOND, 0);
            index++;
            index++;  // Skip one extra unknown byte (so far always observed to be 0x1e)
            for (int hour=0; hour<=23; hour++) {
                syncingDay.set(Calendar.HOUR_OF_DAY, hour);
                syncingDay.set(Calendar.MINUTE, 0);
                float temp_00 = value[index] & 0xff;
                index++;
                float temp_30 = value[index] & 0xff;
                index++;
                if (temp_00 > 0) {
                    temp_00 = (temp_00 / 10) + 20;
                    LOG.info("Received temperature data from {} days ago at {}:00: {} °C", days_ago, hour, temp_00);
                    ColmiTemperatureSample temperatureSample = new ColmiTemperatureSample();
                    temperatureSample.setTimestamp(syncingDay.getTimeInMillis());
                    temperatureSample.setTemperature(temp_00);
                    temperatureSample.setTemperatureType(TemperatureSample.TYPE_SKIN);
                    temperatureSample.setTemperatureLocation(TemperatureSample.LOCATION_FINGER);
                    temperatureSamples.add(temperatureSample);
                }
                syncingDay.set(Calendar.MINUTE, 30);
                if (temp_30 > 0) {
                    temp_30 = (temp_30 / 10) + 20;
                    LOG.info("Received temperature data from {} days ago at {}:30: {} °C", days_ago, hour, temp_30);
                    ColmiTemperatureSample temperatureSample = new ColmiTemperatureSample();
                    temperatureSample.setTimestamp(syncingDay.getTimeInMillis());
                    temperatureSample.setTemperature(temp_30);
                    temperatureSample.setTemperatureType(TemperatureSample.TYPE_SKIN);
                    temperatureSample.setTemperatureLocation(TemperatureSample.LOCATION_FINGER);
                    temperatureSamples.add(temperatureSample);
                }
                if (index - 6 >= length) {
                    break;
                }
            }
        }
        if (!temperatureSamples.isEmpty()) {
            try (DBHandler db = GBApplication.acquireDB()) {
                ColmiTemperatureSampleProvider sampleProvider = new ColmiTemperatureSampleProvider(device, db.getDaoSession());
                sampleProvider.persistSamples(temperatureSamples, context);
            } catch (Exception e) {
                LOG.error("Error acquiring database for recording temperature samples", e);
            }
        }
    }
}
