package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import nodomain.freeyourgadget.gadgetbridge.activities.charts.BodyEnergyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.BodyEnergyPeriodFragment;

public class BodyEnergyFragmentAdapter extends NestedFragmentAdapter {

    public BodyEnergyFragmentAdapter(Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new BodyEnergyFragment();
            case 1:
                return BodyEnergyPeriodFragment.newInstance(7);
            case 2:
                return BodyEnergyPeriodFragment.newInstance(30);
        }
        return new BodyEnergyFragment();
    }
}

