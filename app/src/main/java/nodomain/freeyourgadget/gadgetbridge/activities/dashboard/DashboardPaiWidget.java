/*  Copyright (C) 2026 Ritvik Banakar

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;

/**
 * Dashboard widget that displays the user's current PAI (Personal Activity Intelligence) score.
 *
 * The gauge is rendered as a segmented arc mirroring the visual language used throughout
 * the rest of the dashboard. When the total PAI score meets or exceeds the device target,
 * the arc is drawn in full using two proportional color segments — chart_pai_weekly for
 * the carry-over (total minus today) portion and chart_pai_today for today's contribution.
 * When the target has not yet been reached, the arc is only partially filled to reflect
 * the fraction of the goal completed.
 *
 * The segmentation logic mirrors PaiChartFragment#refreshDayData and reuses the same
 * color constants so the chart and dashboard screens stay visually consistent.
 */
public class DashboardPaiWidget extends AbstractGaugeWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardPaiWidget.class);

    /** Key used to stash/retrieve PaiData from the shared dashboard bundle. */
    private static final String DATA_KEY = "pai";

    public DashboardPaiWidget() {
        super(R.string.menuitem_pai, DATA_KEY);
    }

    public static DashboardPaiWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardPaiWidget fragment = new DashboardPaiWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    // -------------------------------------------------------------------------
    // AbstractGaugeWidget contract
    // -------------------------------------------------------------------------

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsPai(device);
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);

        // Use getAllSamples bounded by timeFrom and timeTo to ensure we only show data
        // that actually exists within the selected day. Using getLatestSample(timeTo) has no lower
        // bound and could bleed back to a previous day's sample — for example
        // if Sunday has no data or the device was reset, it would incorrectly return Saturday's score.
        final long windowStartMs = dashboardData.timeFrom * 1000L;
        final long windowEndMs   = dashboardData.timeTo   * 1000L;

        final PaiData data = new PaiData();

        long latestTimestamp = Long.MIN_VALUE;

        try (DBHandler dbHandler = GBApplication.acquireDbReadOnly()) {
            for (final GBDevice dev : devices) {
                final DeviceCoordinator coordinator = dev.getDeviceCoordinator();
                final TimeSampleProvider<? extends PaiSample> provider =
                        coordinator.getPaiSampleProvider(dev, dbHandler.getDaoSession());

                if (provider == null) {
                    LOG.warn("Device {} returned a null PAI sample provider — skipping", dev);
                    continue;
                }

                final List<? extends PaiSample> samples = provider.getAllSamples(windowStartMs, windowEndMs);

                if (samples.isEmpty()) {
                    continue;
                }

                final PaiSample sample = samples.get(samples.size() - 1);

                if (sample.getTimestamp() > latestTimestamp) {
                    latestTimestamp = sample.getTimestamp();
                    data.total  = Math.round(sample.getPaiTotal());
                    data.today  = Math.round(sample.getPaiToday());
                    data.target = coordinator.getPaiTarget();
                }
            }
        } catch (final Exception e) {
            LOG.error("Could not get PAI sample for dashboard widget", e);
        }

        dashboardData.put(DATA_KEY, data);
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final PaiData paiData = (PaiData) dashboardData.get(DATA_KEY);

        if (paiData == null || paiData.target <= 0) {
            // No data available — render an empty gauge rather than crashing.
            drawSimpleGauge(0, -1);
            setText("0");
            return;
        }

        // ---- Value text -------------------------------------------------------
        setText(String.valueOf(paiData.total));

        // ---- color constants -------------------------------------------------
        // Same color resources as PaiChartFragment#init / updateChartsnUIThread
        // so the two screens are visually consistent.
        final int colorWeekly = ContextCompat.getColor(
                GBApplication.getContext(), R.color.chart_pai_weekly);
        final int colorToday  = ContextCompat.getColor(
                GBApplication.getContext(), R.color.chart_pai_today);

        final boolean targetMet = paiData.total >= paiData.target;

        if (targetMet) {
            // ---- Target reached: full arc, two proportional color segments ----
            //
            // When the goal is met the arc is drawn at 100 % of its length.
            // The two segments represent the carry-over portion (weekly total
            // minus today's contribution) and today's increment, exactly as the
            // stacked bar in PaiChartFragment does.
            //
            // Guard against division-by-zero
            final float todayFraction = paiData.total > 0
                    ? (float) paiData.today / paiData.total
                    : 0f;
            final float weeklyFraction = 1f - todayFraction;

            drawSegmentedGauge(
                    new int[]   { colorWeekly,     colorToday    },
                    new float[] { weeklyFraction,  todayFraction },
                    /* markerValue= */ -1,
                    /* showMarker=  */ false,
                    /* segment gap=     */ false
            );
        } else {
            // ---- Target not reached: partially filled arc ----------------------
            //
            // The filled portion of the arc equals total/target, matching the
            // gauge drawn in PaiChartFragment#updateChartsnUIThread where
            //   segments[0] = (total - today) / maxPai
            //   segments[1] = today           / maxPai
            // The unfilled remainder of the arc is implicit in drawSegmentedGauge
            // because the segments sum to less than 1.
            final float todayFraction   = (float) paiData.today                        / paiData.target;
            final float weeklyFraction  = (float) (paiData.total - paiData.today)      / paiData.target;

            drawSegmentedGauge(
                    new int[]   { colorWeekly,     colorToday   },
                    new float[] { weeklyFraction,  todayFraction },
                    /* markerValue= */ -1,
                    /* showMarker=  */ false,
                    /* segment gap=     */ false
            );
        }
    }

    // -------------------------------------------------------------------------
    // Internal data holder
    // -------------------------------------------------------------------------

    /**
     * Lightweight value object carrying the PAI figures needed for rendering.
     * Intentionally package-private so unit tests in the same package can
     * inspect it without reflection.
     */
    static final class PaiData implements Serializable {
        /** Rolling 7-day PAI total at the end of the selected period. */
        int total  = 0;
        /** PAI earned on the most recent day within the selected period. */
        int today  = 0;
        /** Device-specific PAI target (typically 100). */
        int target = 0;
    }
}