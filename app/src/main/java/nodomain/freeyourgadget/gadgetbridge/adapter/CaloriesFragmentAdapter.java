package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.CaloriesDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.CaloriesPeriodFragment;

public class CaloriesFragmentAdapter extends NestedFragmentAdapter {
    protected FragmentManager fragmentManager;

    public CaloriesFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
        fragmentManager = childFragmentManager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new CaloriesDailyFragment();
            case 1:
                return CaloriesPeriodFragment.newInstance(7);
            case 2:
                return CaloriesPeriodFragment.newInstance(30);
        }
        return new CaloriesDailyFragment();
    }
}
