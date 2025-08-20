package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.RespiratoryRateDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.RespiratoryRatePeriodFragment;

public class RespiratoryRateFragmentAdapter extends NestedFragmentAdapter {

    public RespiratoryRateFragmentAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new RespiratoryRateDailyFragment();
            case 1:
                return RespiratoryRatePeriodFragment.newInstance(7);
            case 2:
                return RespiratoryRatePeriodFragment.newInstance(30);
        }
        return new RespiratoryRateDailyFragment();
    }
}
