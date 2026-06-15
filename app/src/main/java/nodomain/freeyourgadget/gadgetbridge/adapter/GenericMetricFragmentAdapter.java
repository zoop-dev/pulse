package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.GenericMetricChartFragment;

public class GenericMetricFragmentAdapter extends NestedFragmentAdapter {
    public GenericMetricFragmentAdapter(final Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        switch (position) {
            case 0:
                return GenericMetricChartFragment.newInstance(1);
            case 1:
                return GenericMetricChartFragment.newInstance(7);
            case 2:
                return GenericMetricChartFragment.newInstance(30);
        }
        return GenericMetricChartFragment.newInstance(1);
    }
}
