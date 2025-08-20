package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.viewpager2.adapter.FragmentStateAdapter;

import nodomain.freeyourgadget.gadgetbridge.adapter.RespiratoryRateFragmentAdapter;

public class RespiratoryRateCollectionFragment extends AbstractCollectionFragment {
    public RespiratoryRateCollectionFragment() {

    }

    public static RespiratoryRateCollectionFragment newInstance(final boolean allowSwipe) {
        final RespiratoryRateCollectionFragment fragment = new RespiratoryRateCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FragmentStateAdapter getFragmentAdapter() {
        return new RespiratoryRateFragmentAdapter(this);
    }
}

