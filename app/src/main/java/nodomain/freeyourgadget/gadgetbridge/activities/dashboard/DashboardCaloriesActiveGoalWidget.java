/*  Copyright (C) 2024-2025 a0z, Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.DashboardFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.CaloriesDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardCaloriesActiveGoalWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardCaloriesActiveGoalWidget extends AbstractGaugeWidget {
    public DashboardCaloriesActiveGoalWidget() {
        super(R.string.active_calories, "calories", CaloriesDailyFragment.GaugeViewMode.ACTIVE_CALORIES_GOAL.toString());
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param dashboardData An instance of DashboardFragment.DashboardData.
     * @return A new instance of fragment DashboardStepsWidget.
     */
    public static DashboardCaloriesActiveGoalWidget newInstance(final DashboardFragment.DashboardData dashboardData) {
        final DashboardCaloriesActiveGoalWidget fragment = new DashboardCaloriesActiveGoalWidget();
        final Bundle args = new Bundle();
        args.putSerializable(ARG_DASHBOARD_DATA, dashboardData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected boolean isSupportedBy(final GBDevice device) {
        return device.getDeviceCoordinator().supportsActiveCalories(device);
    }

    @Override
    protected void populateData(final DashboardFragment.DashboardData dashboardData) {
        dashboardData.getActiveCaloriesTotal();
        dashboardData.getActiveCaloriesGoalFactor();
    }

    @Override
    protected void draw(final DashboardFragment.DashboardData dashboardData) {
        setText(String.valueOf(dashboardData.getActiveCaloriesTotal()));
        final int colorCalories = ContextCompat.getColor(GBApplication.getContext(), R.color.calories_color);
        drawSimpleGauge(
                colorCalories,
                dashboardData.getActiveCaloriesGoalFactor()
        );
    }
}
