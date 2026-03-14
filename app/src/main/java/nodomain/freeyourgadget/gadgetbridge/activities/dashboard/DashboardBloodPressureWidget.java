/*
    Copyright (C) 2026 Christian Breiteneder

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BloodPressureSample;

public class DashboardBloodPressureWidget extends AbstractGaugeWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardBloodPressureWidget.class);

    public DashboardBloodPressureWidget() {
        super(R.string.blood_pressure, "bloodpressure");
    }

    public static DashboardBloodPressureWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardBloodPressureWidget fragment = new DashboardBloodPressureWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsBloodPressureMeasurement(device);
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        final List<GBDevice> devices = getSupportedDevices(dashboardData);
        int latestSystolic = 0;
        int latestDiastolic = 0;
        long latestTimestamp = 0;

        try (DBHandler dbHandler = GBApplication.acquireDbReadOnly()) {
            for (final GBDevice dev : devices) {
                final var provider = dev.getDeviceCoordinator()
                        .getBloodPressureSampleProvider(dev, dbHandler.getDaoSession());
                if (provider == null) continue;
                final List<? extends BloodPressureSample> samples = provider.getAllSamples(
                        dashboardData.timeFrom * 1000L,
                        dashboardData.timeTo * 1000L
                );

                if (!samples.isEmpty()) {
                    final BloodPressureSample latest = samples.get(samples.size() - 1);
                    if (latest.getTimestamp() > latestTimestamp) {
                        latestTimestamp = latest.getTimestamp();
                        latestSystolic  = latest.getBpSystolic();
                        latestDiastolic = latest.getBpDiastolic();
                    }
                }
            }
        } catch (final Exception e) {
            LOG.error("Could not get blood pressure samples", e);
        }

        dashboardData.put("bp_systolic",  latestSystolic);
        dashboardData.put("bp_diastolic", latestDiastolic);
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        final Integer systolic  = (Integer) dashboardData.get("bp_systolic");
        final Integer diastolic = (Integer) dashboardData.get("bp_diastolic");

        if (systolic != null && systolic > 0 && diastolic != null && diastolic > 0) {
            setText(systolic + "/" + diastolic);

            // WHO classification: color and gauge position based on systolic/diastolic values
            final int color;
            final float gaugeValue;
            if (systolic < 120 && diastolic < 80) {
                color      = android.graphics.Color.rgb(76, 175, 80);  // green  – Normal
                gaugeValue = 0.25f;
            } else if (systolic < 130 && diastolic < 80) {
                color      = android.graphics.Color.rgb(139, 195, 74); // lime   – Elevated
                gaugeValue = 0.45f;
            } else if (systolic < 140 || diastolic < 90) {
                color      = android.graphics.Color.rgb(255, 152, 0);  // orange – Stage 1
                gaugeValue = 0.65f;
            } else {
                color      = android.graphics.Color.rgb(244, 67, 54);  // red    – Stage 2+
                gaugeValue = 0.88f;
            }
            drawSimpleGauge(color, gaugeValue);
        } else {
            setText(getString(R.string.stats_empty_value));
            drawSimpleGauge(android.graphics.Color.GRAY, -1);
        }
    }
}