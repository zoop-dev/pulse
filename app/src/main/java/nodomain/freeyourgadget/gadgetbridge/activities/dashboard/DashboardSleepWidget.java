/*  Copyright (C) 2023-2024 Arjan Schrijver, José Rebelo

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

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardSleepWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardSleepWidget extends AbstractGaugeWidget<DashboardSleepWidget.SleepData> {
    public DashboardSleepWidget() {
        super(R.string.menuitem_sleep, "sleep");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardSleepWidget.
     */
    public static DashboardSleepWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardSleepWidget fragment = new DashboardSleepWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsSleepMeasurement(device);
    }

    @Override
    protected SleepData populateData(final DashboardFragment.DashboardData dashboardData) {
        final long total = DashboardUtils.getSleepMinutesTotal(dashboardData);
        return new SleepData(total, DashboardUtils.getSleepMinutesGoalFactor(total));
    }

    @Override
    protected void draw(final SleepData data) {
        final String valueText = String.format(
                Locale.ROOT,
                "%d:%02d",
                (int) Math.floor(data.total / 60f),
                (int) (data.total % 60f)
        );

        setText(valueText);
        drawSimpleGauge(
                color_light_sleep,
                data.goalFactor
        );
    }

    protected static class SleepData {
        final long total;
        final float goalFactor;

        SleepData(final long total, final float goalFactor) {
            this.total = total;
            this.goalFactor = goalFactor;
        }
    }
}
