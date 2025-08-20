package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepsDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepsPeriodFragment;

public class StepsFragmentAdapter extends NestedFragmentAdapter {

    public StepsFragmentAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StepsDailyFragment();
            case 1:
                return StepsPeriodFragment.newInstance(7);
            case 2:
                return StepsPeriodFragment.newInstance(30);
        }
        return new StepsDailyFragment();
    }
}
