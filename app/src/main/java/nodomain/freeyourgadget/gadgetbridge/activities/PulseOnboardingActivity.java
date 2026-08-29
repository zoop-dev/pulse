/*  Copyright (C) 2026 Pulse

    This file is part of Pulse, a Garmin-only fork of Gadgetbridge.

    Pulse is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;
import nodomain.freeyourgadget.gadgetbridge.activities.welcome.WelcomePageIndicator;
import nodomain.freeyourgadget.gadgetbridge.util.PulseWeather;

public class PulseOnboardingActivity extends AbstractGBActivity {
    public static final String PREF_ONBOARDED = "pulse_onboarded";

    private static final int[] PAGE_LAYOUTS = {
            R.layout.onboard_page_welcome,
            R.layout.onboard_page_appearance,
            R.layout.onboard_page_weather,
            R.layout.onboard_page_permissions,
            R.layout.onboard_page_start,
    };
    private static final int PAGE_LAST = PAGE_LAYOUTS.length - 1;

    private static final String[] ACCENT_KEYS = {"blue", "violet", "coral", "mint", "pink"};
    private static final int[] ACCENT_VIEW_IDS = {
            R.id.onboard_accent_blue, R.id.onboard_accent_violet, R.id.onboard_accent_coral,
            R.id.onboard_accent_mint, R.id.onboard_accent_pink,
    };
    private static final int[] ACCENT_COLORS = {
            R.color.accent_blue, R.color.accent_violet, R.color.accent_coral,
            R.color.accent_mint, R.color.accent_pink,
    };

    private static final int PAGE_PERMISSIONS = 3;

    private ViewPager2 pager;
    private Button nextButton;
    private Button skipButton;

    private boolean permissionsReviewed;
    private String selectedTheme;
    private String selectedAccent;
    private String selectedWeather;

    private final ActivityResultLauncher<String[]> restorePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                persist();
                final Intent i = new Intent(this, BackupRestoreProgressActivity.class);
                i.putExtra(BackupRestoreProgressActivity.EXTRA_URI, uri);
                i.putExtra(BackupRestoreProgressActivity.EXTRA_ACTION, "import");
                startActivity(i);
                finish();
            });

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_onboarding);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        pager = findViewById(R.id.onboard_pager);
        pager.setAdapter(new PageAdapter());
        pager.setOffscreenPageLimit(1);

        final WelcomePageIndicator indicator = findViewById(R.id.onboard_indicator);
        indicator.setViewPager(pager);

        nextButton = findViewById(R.id.onboard_next);
        skipButton = findViewById(R.id.onboard_skip);
        nextButton.setOnClickListener(v -> {
            final int cur = pager.getCurrentItem();
            if (cur < PAGE_LAST) {
                pager.setCurrentItem(cur + 1, true);
            } else {
                persistAndFinish();
            }
        });
        skipButton.setOnClickListener(v -> persistAndFinish());

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(final int position) {
                nextButton.setText(position == PAGE_LAST
                        ? R.string.pulse_onboard_done : R.string.pulse_onboard_next);
                skipButton.setVisibility(position == PAGE_LAST ? View.GONE : View.VISIBLE);
                updateNav();
            }
        });
        updateNav();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pager.getCurrentItem() > 0) {
                    pager.setCurrentItem(pager.getCurrentItem() - 1, true);
                } else {
                    persistAndFinish();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNav();
    }

    private void updateNav() {
        if (pager == null) {
            return;
        }
        final boolean locked = pager.getCurrentItem() == PAGE_PERMISSIONS && !permissionsReviewed;
        nextButton.setEnabled(!locked);
        nextButton.setAlpha(locked ? 0.4f : 1f);
        pager.setUserInputEnabled(!locked);
    }

    private void persist() {
        final android.content.SharedPreferences.Editor editor =
                GBApplication.getPrefs().getPreferences().edit();
        if (selectedTheme != null) {
            editor.putString("pref_key_theme", selectedTheme);
        }
        if (selectedAccent != null) {
            editor.putString("pulse_accent", selectedAccent);
        }
        if (selectedWeather != null) {
            editor.putString("pulse_weather_source", selectedWeather);
        }
        editor.putBoolean(PREF_ONBOARDED, true);
        editor.putBoolean("first_run", false);
        editor.apply();

        GBApplication.applyPulseNightMode();
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(GBApplication.ACTION_THEME_CHANGE));

        if ("auto".equals(selectedWeather)) {
            PulseWeather.maybeFetch(getApplicationContext());
        }
    }

    private void persistAndFinish() {
        persist();
        finish();
    }

    private void onThemePicked(final String theme) {
        selectedTheme = theme;
        pager.getAdapter().notifyItemChanged(1);
    }

    private void onAccentPicked(final String accent) {
        selectedAccent = accent;
        pager.getAdapter().notifyItemChanged(1);
    }

    private void onWeatherPicked(final String source) {
        selectedWeather = source;
        pager.setCurrentItem(3, true);
    }

    private GradientDrawable swatch(final int colorRes, final boolean selected) {
        final GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(ContextCompat.getColor(this, colorRes));
        if (selected) {
            d.setStroke(dp(3), ContextCompat.getColor(this, R.color.pulse_text));
        }
        return d;
    }

    private int dp(final int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    private final class PageAdapter extends RecyclerView.Adapter<PageAdapter.PageHolder> {
        final class PageHolder extends RecyclerView.ViewHolder {
            PageHolder(final View itemView) {
                super(itemView);
            }
        }

        @Override
        public int getItemCount() {
            return PAGE_LAYOUTS.length;
        }

        @Override
        public int getItemViewType(final int position) {
            return position;
        }

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(PAGE_LAYOUTS[viewType], parent, false);
            view.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new PageHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final PageHolder holder, final int position) {
            final View v = holder.itemView;
            switch (position) {
                case 1:
                    bindAppearance(v);
                    break;
                case 2:
                    v.findViewById(R.id.onboard_weather_openmeteo).setOnClickListener(x -> onWeatherPicked("auto"));
                    v.findViewById(R.id.onboard_weather_breezy).setOnClickListener(x -> onWeatherPicked("external"));
                    v.findViewById(R.id.onboard_weather_skip).setOnClickListener(x -> onWeatherPicked("off"));
                    break;
                case 3: {
                    final Button review = v.findViewById(R.id.onboard_permissions_review);
                    review.setBackgroundTintList(null);
                    review.setOnClickListener(x -> {
                        permissionsReviewed = true;
                        updateNav();
                        startActivity(new Intent(PulseOnboardingActivity.this, PermissionsActivity.class)
                                .putExtra(PermissionsActivity.ARG_SHOW_DO_NOT_ASK_BUTTON, true));
                    });
                    break;
                }
                case 4: {
                    final Button connect = v.findViewById(R.id.onboard_connect);
                    connect.setBackgroundTintList(null);
                    connect.setOnClickListener(x -> {
                        persist();
                        startActivity(new Intent(PulseOnboardingActivity.this, DiscoveryActivityV2.class));
                        finish();
                    });
                    final Button restore = v.findViewById(R.id.onboard_restore);
                    restore.setBackgroundTintList(null);
                    restore.setOnClickListener(x -> restorePicker.launch(new String[]{"*/*"}));
                    break;
                }
                default:
                    break;
            }
        }

        private void bindAppearance(final View v) {
            bindThemeButton(v, R.id.onboard_theme_light, "light");
            bindThemeButton(v, R.id.onboard_theme_dark, "dark");
            bindThemeButton(v, R.id.onboard_theme_system, "system");
            for (int i = 0; i < ACCENT_VIEW_IDS.length; i++) {
                final View swatch = v.findViewById(ACCENT_VIEW_IDS[i]);
                final String key = ACCENT_KEYS[i];
                swatch.setBackground(swatch(ACCENT_COLORS[i], key.equals(selectedAccent)));
                swatch.setOnClickListener(x -> onAccentPicked(key));
            }
        }

        private void bindThemeButton(final View v, final int id, final String theme) {
            final Button b = v.findViewById(id);
            b.setBackgroundResource(theme.equals(selectedTheme)
                    ? R.drawable.pulse_pill_primary : R.drawable.pulse_pill);
            b.setBackgroundTintList(null);
            b.setTextColor(ContextCompat.getColor(PulseOnboardingActivity.this,
                    theme.equals(selectedTheme) ? R.color.pulse_bg : R.color.pulse_text));
            b.setOnClickListener(x -> onThemePicked(theme));
        }
    }
}
