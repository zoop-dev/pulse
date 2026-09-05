/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DailyTotals;

public enum PulseWidgetMetric {
    STEPS("steps", R.string.steps, R.drawable.ic_ms_steps, 0xFF0E9DC4, 0xFF2BD8FF),
    DISTANCE("distance", R.string.distance, R.drawable.ic_ms_distance, 0xFF0E9DC4, 0xFF2BD8FF),
    CALORIES("calories", R.string.calories, R.drawable.ic_ms_calories, 0xFFE07B1F, 0xFFFF9A4A),
    SLEEP("sleep", R.string.menuitem_sleep, R.drawable.ic_ms_sleep, 0xFF6A40E0, 0xFF7A5CFF),
    HEARTRATE("heartrate", R.string.menuitem_hr, R.drawable.ic_ms_hr, 0xFFE5484D, 0xFFFF6B6B),
    BODYBATTERY("bodybattery", R.string.body_energy, R.drawable.ic_ms_bodybattery, 0xFF1FA877, 0xFF4AD6A0),
    STRESS("stress", R.string.menuitem_stress, R.drawable.ic_ms_stress, 0xFFE07B1F, 0xFFFF9A4A),
    SPO2("spo2", R.string.pref_header_spo2, R.drawable.ic_ms_spo2, 0xFF0E9DC4, 0xFF2BD8FF),
    HRV("hrv", R.string.hrv, R.drawable.ic_ms_hrv, 0xFF6A40E0, 0xFF7A5CFF),
    RESPIRATION("respiration", R.string.respiratoryrate, R.drawable.ic_ms_respiration, 0xFF6A40E0, 0xFF7A5CFF);

    public final String key;
    public final int labelRes;
    public final int iconRes;
    private final int tintLight;
    private final int tintDark;

    PulseWidgetMetric(final String key, final int labelRes, final int iconRes,
                      final int tintLight, final int tintDark) {
        this.key = key;
        this.labelRes = labelRes;
        this.iconRes = iconRes;
        this.tintLight = tintLight;
        this.tintDark = tintDark;
    }

    public int tint(final boolean dark) {
        return dark ? tintDark : tintLight;
    }

    public String label(final Context context) {
        return context.getString(labelRes);
    }

    public static PulseWidgetMetric fromKey(final String key) {
        for (final PulseWidgetMetric m : values()) {
            if (m.key.equals(key)) {
                return m;
            }
        }
        return STEPS;
    }

    public static boolean isKnown(final String key) {
        for (final PulseWidgetMetric m : values()) {
            if (m.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    public static final class Reading {
        public final String value;
        public final int progress;
        public final int max;

        public Reading(final String value, final int progress, final int max) {
            this.value = value;
            this.progress = progress;
            this.max = max;
        }

        public boolean hasBar() {
            return max > 0;
        }

        public float fraction() {
            return max > 0 ? Math.min(1f, progress / (float) max) : 0f;
        }
    }

    public Reading read(final Context context, final GBDevice device, final DailyTotals totals,
                        final ActivityUser user, final DaoSession session) {
        try {
            switch (this) {
                case DISTANCE: {
                    long cm = totals.getDistance();
                    if (cm <= 0 && totals.getSteps() > 0) {
                        cm = totals.getSteps() * user.getStepLengthCm();
                    }
                    final double m = cm * 0.01;
                    return new Reading(FormatUtils.getFormattedDistanceLabel(m),
                            (int) m, user.getDistanceGoalMeters());
                }
                case CALORIES: {
                    final int cal = (int) (totals.getActiveCalories() / 1000);
                    return new Reading(String.valueOf(cal), cal, user.getCaloriesBurntGoal());
                }
                case SLEEP: {
                    final int sl = (int) totals.getSleep();
                    final String v = sl > 0
                            ? DateTimeUtils.formatDurationHoursMinutes(sl, TimeUnit.MINUTES)
                            : context.getString(R.string.pulse_no_sleep);
                    return new Reading(v, sl, user.getSleepDurationGoal());
                }
                case HEARTRATE: {
                    final int hr = intSample(device, session, Sample.HR);
                    return new Reading(hr > 0 ? String.valueOf(hr) : "–", 0, 0);
                }
                case BODYBATTERY: {
                    final int be = intSample(device, session, Sample.BODY_ENERGY);
                    return new Reading(be >= 0 ? String.valueOf(be) : "–", Math.max(be, 0), be >= 0 ? 100 : 0);
                }
                case STRESS: {
                    final int st = intSample(device, session, Sample.STRESS);
                    return new Reading(st >= 0 ? String.valueOf(st) : "–", 0, 0);
                }
                case SPO2: {
                    final int sp = intSample(device, session, Sample.SPO2);
                    return new Reading(sp > 0 ? sp + "%" : "–", 0, 0);
                }
                case HRV: {
                    final int hrv = intSample(device, session, Sample.HRV);
                    return new Reading(hrv > 0 ? hrv + " ms" : "–", 0, 0);
                }
                case RESPIRATION: {
                    final int rr = intSample(device, session, Sample.RESPIRATION);
                    return new Reading(rr > 0 ? String.valueOf(rr) : "–", 0, 0);
                }
                default: {
                    final int steps = (int) totals.getSteps();
                    return new Reading(String.valueOf(steps), steps, user.getStepsGoal());
                }
            }
        } catch (final Exception e) {
            return new Reading("–", 0, 0);
        }
    }

    private enum Sample { HR, BODY_ENERGY, STRESS, SPO2, HRV, RESPIRATION }

    private static int intSample(final GBDevice device, final DaoSession providedSession, final Sample which) {
        if (device == null) {
            return -1;
        }
        DBHandler ownDb = null;
        try {
            DaoSession session = providedSession;
            if (session == null) {
                ownDb = GBApplication.acquireDbReadOnly();
                session = ownDb.getDaoSession();
            }
            final DeviceCoordinator c = device.getDeviceCoordinator();
            switch (which) {
                case HR: {
                    final nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample s =
                            c.getHeartRateRestingSampleProvider(device, session).getLatestSample();
                    return s != null ? s.getHeartRate() : -1;
                }
                case BODY_ENERGY: {
                    final nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample s =
                            c.getBodyEnergySampleProvider(device, session).getLatestSample();
                    return s != null ? s.getEnergy() : -1;
                }
                case STRESS: {
                    final nodomain.freeyourgadget.gadgetbridge.model.StressSample s =
                            c.getStressSampleProvider(device, session).getLatestSample();
                    return s != null ? s.getStress() : -1;
                }
                case SPO2: {
                    final nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample s =
                            c.getSpo2SampleProvider(device, session).getLatestSample();
                    return s != null ? s.getSpo2() : -1;
                }
                case HRV: {
                    final nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample s =
                            c.getHrvValueSampleProvider(device, session).getLatestSample();
                    return s != null ? s.getValue() : -1;
                }
                case RESPIRATION: {
                    final nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample s =
                            c.getRespiratoryRateSampleProvider(device, session).getLatestSample();
                    return s != null ? Math.round(s.getRespiratoryRate()) : -1;
                }
                default:
                    return -1;
            }
        } catch (final Exception e) {
            return -1;
        } finally {
            if (ownDb != null) {
                try {
                    ownDb.close();
                } catch (final Exception ignored) {
                }
            }
        }
    }
}
