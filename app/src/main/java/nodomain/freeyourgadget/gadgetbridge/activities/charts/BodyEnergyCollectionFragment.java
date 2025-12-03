package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.viewpager2.adapter.FragmentStateAdapter;

import nodomain.freeyourgadget.gadgetbridge.adapter.BodyEnergyFragmentAdapter;

public class BodyEnergyCollectionFragment extends AbstractCollectionFragment {
    public BodyEnergyCollectionFragment() {

    }

    public static BodyEnergyCollectionFragment newInstance(final boolean allowSwipe) {
        final BodyEnergyCollectionFragment fragment = new BodyEnergyCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FragmentStateAdapter getFragmentAdapter() {
        return new BodyEnergyFragmentAdapter(this);
    }
}

