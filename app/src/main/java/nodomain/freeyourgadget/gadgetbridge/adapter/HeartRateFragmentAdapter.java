package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.DaySleepChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.WeekSleepChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.HeartRatePeriodFragment;

public class HeartRateFragmentAdapter extends NestedFragmentAdapter {
    public HeartRateFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return HeartRatePeriodFragment.newInstance(1);
            case 1:
                return HeartRatePeriodFragment.newInstance(7);
            case 2:
                return HeartRatePeriodFragment.newInstance(30);
        }
        return new HeartRatePeriodFragment();
    }
}
