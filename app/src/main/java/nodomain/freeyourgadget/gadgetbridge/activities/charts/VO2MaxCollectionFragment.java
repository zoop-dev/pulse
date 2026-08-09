package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.VO2MaxFragmentAdapter;

public class VO2MaxCollectionFragment extends AbstractCollectionFragment {
    public VO2MaxCollectionFragment() {
    }

    public static VO2MaxCollectionFragment newInstance(final boolean allowSwipe) {
        final VO2MaxCollectionFragment fragment = new VO2MaxCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public FragmentStateAdapter getFragmentAdapter() {
        return new VO2MaxFragmentAdapter(this);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        final TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.calendar_month));
                    break;
                case 1:
                    tab.setText(getString(R.string.six_months));
                    break;
                case 2:
                    tab.setText(getString(R.string.calendar_year));
                    break;
            }
        }).attach();
    }
}
