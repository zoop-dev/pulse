package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import nodomain.freeyourgadget.gadgetbridge.adapter.GenericMetricFragmentAdapter;

public class GenericMetricCollectionFragment extends AbstractCollectionFragment {
    private static final int MONTH_TAB_POSITION = 2;

    public GenericMetricCollectionFragment() {
    }

    public static GenericMetricCollectionFragment newInstance(final boolean allowSwipe) {
        final GenericMetricCollectionFragment fragment = new GenericMetricCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FragmentStateAdapter getFragmentAdapter() {
        return new GenericMetricFragmentAdapter(this);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState == null) {
            viewPager.setCurrentItem(MONTH_TAB_POSITION, false);
        }
    }
}
