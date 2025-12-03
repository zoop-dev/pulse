/*  Copyright (C) 2024 a0z, José Rebelo

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

import nodomain.freeyourgadget.gadgetbridge.activities.charts.Spo2ChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.Spo2PeriodFragment;

public class Spo2FragmentAdapter extends NestedFragmentAdapter {

    public Spo2FragmentAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new Spo2ChartFragment();
            case 1:
                return Spo2PeriodFragment.newInstance(7);
            case 2:
                return Spo2PeriodFragment.newInstance(30);
        }
        return new Spo2ChartFragment();
    }
}

