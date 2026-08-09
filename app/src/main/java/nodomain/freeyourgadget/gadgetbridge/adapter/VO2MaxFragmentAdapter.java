package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.VO2MaxPeriodFragment;

public class VO2MaxFragmentAdapter extends NestedFragmentAdapter {
    public VO2MaxFragmentAdapter(final Fragment fragment) {
        super(fragment);
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        switch (position) {
            case 1:
                return VO2MaxPeriodFragment.newInstance(180, false);
            case 2:
                return VO2MaxPeriodFragment.newInstance(365, false);
        }
        return VO2MaxPeriodFragment.newInstance(30, true);
    }
}
