package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StressDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StressPeriodFragment;

public class StressFragmentAdapter extends NestedFragmentAdapter {
    public StressFragmentAdapter(Fragment fragment) {
        super(fragment);
    }

    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StressDailyFragment();
            case 1:
                return StressPeriodFragment.newInstance(7);
            case 2:
                return StressPeriodFragment.newInstance(30);
        }
        return new StressDailyFragment();
    }
}