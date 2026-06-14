package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.HRVStatusFragment;

public class HrvStatusFragmentAdapter extends NestedFragmentAdapter {
    public HrvStatusFragmentAdapter(final Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        switch (position) {
            case 0:
                return HRVStatusFragment.newLastNightInstance();
            case 1:
                return HRVStatusFragment.newPeriodInstance(7);
            case 2:
                return HRVStatusFragment.newPeriodInstance(30);
        }
        return HRVStatusFragment.newPeriodInstance(7);
    }
}
