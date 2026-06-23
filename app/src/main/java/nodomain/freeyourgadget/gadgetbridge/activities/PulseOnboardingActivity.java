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
import android.os.Bundle;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.discovery.DiscoveryActivityV2;

/** Pulse: first-run welcome screen, shown once until "Get started" is tapped. */
public class PulseOnboardingActivity extends AbstractGBActivity {
    public static final String PREF_ONBOARDED = "pulse_onboarded";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pulse_onboarding);

        findViewById(R.id.onboard_connect).setOnClickListener(v ->
                startActivity(new Intent(this, DiscoveryActivityV2.class)));

        findViewById(R.id.onboard_start).setOnClickListener(v -> {
            GBApplication.getPrefs().getPreferences().edit().putBoolean(PREF_ONBOARDED, true).apply();
            finish();
        });
    }
}
