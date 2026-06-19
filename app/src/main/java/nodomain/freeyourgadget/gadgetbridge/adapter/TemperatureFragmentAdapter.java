/*  Copyright (C) 2026 The Gadgetbridge Contributors

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.TemperatureDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.TemperaturePeriodFragment;

public class TemperatureFragmentAdapter extends NestedFragmentAdapter {
    public TemperatureFragmentAdapter(@NonNull final Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        switch (position) {
            case 0:
                return new TemperatureDailyFragment();
            case 1:
                return TemperaturePeriodFragment.newInstance(7);
            case 2:
                return TemperaturePeriodFragment.newInstance(30);
        }

        return new TemperatureDailyFragment();
    }
}
