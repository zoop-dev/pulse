/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryCurrentSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryLevelProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryPowerSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryTemperatureSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.BatteryVoltageSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BatteryLevel;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;

/**
 * The battery-related metrics that can be plotted on the {@link BatteryInfoChartFragment}
 * chart, one per supported {@link nodomain.freeyourgadget.gadgetbridge.entities.AbstractBatterySample}
 * subclass, plus the pre-existing {@link BatteryLevel}.
 */
public enum BatteryMetric {
    // batteryIndex is n/a - the y-axis for LEVEL is always pinned to 0-100, so its minAxisSpan is unused
    LEVEL(R.string.battery_level, ActivitySummaryEntries.UNIT_PERCENTAGE, R.color.chart_battery_level, 0f, R.drawable.ic_battery),
    VOLTAGE(R.string.xiaomi_scooter_voltage, ActivitySummaryEntries.UNIT_VOLT, R.color.chart_battery_voltage, 0.5f, R.drawable.ic_bolt),
    CURRENT(R.string.electrical_current, ActivitySummaryEntries.UNIT_AMPERE, R.color.chart_battery_current, 0.2f, R.drawable.ic_power_input),
    POWER(R.string.xiaomi_scooter_power, ActivitySummaryEntries.UNIT_WATT, R.color.chart_battery_power, 2f, R.drawable.ic_electric_meter),
    TEMPERATURE(R.string.menuitem_temperature, ActivitySummaryEntries.UNIT_CELSIUS, R.color.chart_battery_temperature, 5f, R.drawable.ic_thermometer),
    ;

    public final int labelResId;
    public final String uomKey;
    public final int colorResId;
    /// The smallest y-axis span (in this metric's unit) a real-valued axis may be shrunk to,
    /// so that a nearly-flat series (e.g. idle current fluctuating by a few mA) doesn't get
    /// stretched to fill the whole chart height and read as a dramatic swing.
    public final float minAxisSpan;
    public final int iconResId;

    BatteryMetric(final int labelResId,
                  @NonNull final String uomKey,
                  final int colorResId,
                  final float minAxisSpan,
                  final int iconResId) {
        this.labelResId = labelResId;
        this.uomKey = uomKey;
        this.colorResId = colorResId;
        this.minAxisSpan = minAxisSpan;
        this.iconResId = iconResId;
    }

    /**
     * Whether any sample of this metric was ever recorded for the given battery index.
     */
    public boolean hasSamples(final DBHandler db, final GBDevice device, final int batteryIndex) {
        return switch (this) {
            case LEVEL -> new BatteryLevelProvider(device, db.getDaoSession()).hasSamples(batteryIndex);
            case VOLTAGE -> new BatteryVoltageSampleProvider(device, db.getDaoSession()).hasSamples(batteryIndex);
            case CURRENT -> new BatteryCurrentSampleProvider(device, db.getDaoSession()).hasSamples(batteryIndex);
            case POWER -> new BatteryPowerSampleProvider(device, db.getDaoSession()).hasSamples(batteryIndex);
            case TEMPERATURE ->
                    new BatteryTemperatureSampleProvider(device, db.getDaoSession()).hasSamples(batteryIndex);
        };
    }

    /**
     * Loads the raw samples for this metric. The timestamp is in unix epoch seconds and is
     * deliberately kept as an int here, rather than folded into a chart {@code Entry} (whose x is
     * a float): an absolute epoch-seconds value has more significant digits than a float's 24-bit
     * mantissa can hold, so it must go through {@link nodomain.freeyourgadget.gadgetbridge.activities.charts.TimestampTranslation}
     * before it is ever assigned to a float.
     */
    @NonNull
    public List<Sample> loadSamples(final DBHandler db,
                                    final GBDevice device,
                                    final int batteryIndex,
                                    final long tsFromMillis,
                                    final long tsToMillis) {
        final List<Sample> samples = new ArrayList<>();
        switch (this) {
            case LEVEL:
                for (final BatteryLevel sample : new BatteryLevelProvider(device, db.getDaoSession()).getAllSamples(batteryIndex, tsFromMillis, tsToMillis)) {
                    samples.add(new Sample(sample.getTimestamp(), sample.getLevel()));
                }
                break;
            case VOLTAGE:
                for (final var sample : new BatteryVoltageSampleProvider(device, db.getDaoSession()).getAllSamples(batteryIndex, tsFromMillis, tsToMillis)) {
                    samples.add(new Sample((int) (sample.getTimestamp() / 1000L), sample.getVoltage()));
                }
                break;
            case CURRENT:
                for (final var sample : new BatteryCurrentSampleProvider(device, db.getDaoSession()).getAllSamples(batteryIndex, tsFromMillis, tsToMillis)) {
                    samples.add(new Sample((int) (sample.getTimestamp() / 1000L), sample.getCurrent()));
                }
                break;
            case POWER:
                for (final var sample : new BatteryPowerSampleProvider(device, db.getDaoSession()).getAllSamples(batteryIndex, tsFromMillis, tsToMillis)) {
                    samples.add(new Sample((int) (sample.getTimestamp() / 1000L), sample.getPower()));
                }
                break;
            case TEMPERATURE:
                for (final var sample : new BatteryTemperatureSampleProvider(device, db.getDaoSession()).getAllSamples(batteryIndex, tsFromMillis, tsToMillis)) {
                    samples.add(new Sample((int) (sample.getTimestamp() / 1000L), sample.getTemperature()));
                }
                break;
            default:
                throw new IllegalStateException("Unhandled battery metric " + this);
        }
        return samples;
    }

    /**
     * The most recently recorded sample for this metric, regardless of any time range - used for
     * the header of (icon, value) pairs, which is independent of the chart's selected chips and
     * time window. Returns {@code null} if this metric was never recorded for this battery index.
     */
    @Nullable
    public Sample loadLatestSample(final DBHandler db, final GBDevice device, final int batteryIndex) {
        return switch (this) {
            case LEVEL -> {
                final BatteryLevel sample = new BatteryLevelProvider(device, db.getDaoSession()).getLatestSample(batteryIndex);
                yield sample == null ? null : new Sample(sample.getTimestamp(), sample.getLevel());
            }
            case VOLTAGE -> {
                final var sample = new BatteryVoltageSampleProvider(device, db.getDaoSession()).getLatestSample(batteryIndex);
                yield sample == null ? null : new Sample((int) (sample.getTimestamp() / 1000L), sample.getVoltage());
            }
            case CURRENT -> {
                final var sample = new BatteryCurrentSampleProvider(device, db.getDaoSession()).getLatestSample(batteryIndex);
                yield sample == null ? null : new Sample((int) (sample.getTimestamp() / 1000L), sample.getCurrent());
            }
            case POWER -> {
                final var sample = new BatteryPowerSampleProvider(device, db.getDaoSession()).getLatestSample(batteryIndex);
                yield sample == null ? null : new Sample((int) (sample.getTimestamp() / 1000L), sample.getPower());
            }
            case TEMPERATURE -> {
                final var sample = new BatteryTemperatureSampleProvider(device, db.getDaoSession()).getLatestSample(batteryIndex);
                yield sample == null ? null : new Sample((int) (sample.getTimestamp() / 1000L), sample.getTemperature());
            }
        };
    }

    /**
     * A single raw sample for a {@link BatteryMetric}, with the timestamp still in Unix epoch
     * seconds (int) rather than a chart-friendly, translated float x value.
     */
    public record Sample(int timestampSeconds, float value) {
    }
}
