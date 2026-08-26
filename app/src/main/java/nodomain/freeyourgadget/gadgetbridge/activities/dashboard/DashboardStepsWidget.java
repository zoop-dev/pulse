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

import java.text.NumberFormat;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardStepsWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardStepsWidget extends AbstractGaugeWidget<DashboardStepsWidget.StepsData> {
    public DashboardStepsWidget() {
        super(R.string.steps, "stepsweek");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardStepsWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardStepsWidget fragment = new DashboardStepsWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsStepCounter(device);
    }

    @Override
    protected StepsData populateData(final DashboardFragment.DashboardData dashboardData) {
        final int total = DashboardUtils.getStepsTotal(dashboardData);
        return new StepsData(total, DashboardUtils.getStepsGoalFactor(total));
    }

    @Override
    protected void draw(final StepsData data) {
        setText(NumberFormat.getInstance().format(data.total));
        drawSimpleGauge(
                color_activity,
                data.goalFactor
        );
    }

    protected static class StepsData {
        final int total;
        final float goalFactor;

        StepsData(final int total, final float goalFactor) {
            this.total = total;
            this.goalFactor = goalFactor;
        }
    }
}
