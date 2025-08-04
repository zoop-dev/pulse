/*  Copyright (C) 2024 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityWelcomeBinding;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;

public class WelcomeActivity extends AbstractGBActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        final ActivityWelcomeBinding binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        WelcomeFragmentsPagerAdapter pagerAdapter = new WelcomeFragmentsPagerAdapter(this);
        binding.welcomeViewpager.setAdapter(pagerAdapter);

        // Set up welcome page indicator
        binding.welcomePageIndicator.setViewPager(binding.welcomeViewpager);
    }

    private static class WelcomeFragmentsPagerAdapter extends FragmentStateAdapter {
        public WelcomeFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return switch (position) {
                case 0 -> new WelcomeFragmentIntro();
                case 1 -> new WelcomeFragmentOverview();
                case 2 -> new WelcomeFragmentDocsSource();
                case 3 -> new WelcomeFragmentPermissions();
                default -> new WelcomeFragmentGetStarted();
            };
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
