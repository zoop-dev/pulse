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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenterv2;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ActivityChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public final class PulseWidgetStyle {
    private PulseWidgetStyle() {
    }

    public static final String PREF_METRICS = "pulse_widget_metrics_";
    public static final String PREF_ACCENT = "pulse_widget_accent_";
    public static final String PREF_THEME = "pulse_widget_theme_";
    public static final String PREF_TAP = "pulse_widget_tap_";

    public static final String[] ACCENT_KEYS = {"blue", "violet", "coral", "mint", "pink"};
    public static final String[] THEME_KEYS = {"auto", "light", "dark"};
    public static final String[] TAP_KEYS = {"charts", "home", "health"};

    public static String accentKey(final int widgetId) {
        return GBApplication.getPrefs().getString(PREF_ACCENT + widgetId, "blue");
    }

    public static String themeKey(final int widgetId) {
        return GBApplication.getPrefs().getString(PREF_THEME + widgetId, "auto");
    }

    public static String tapKey(final int widgetId) {
        return GBApplication.getPrefs().getString(PREF_TAP + widgetId, "charts");
    }

    public static boolean isDark(final Context context, final String themeKey) {
        if ("light".equals(themeKey)) {
            return false;
        }
        if ("dark".equals(themeKey)) {
            return true;
        }
        final int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int accent(final String key, final boolean dark) {
        switch (key) {
            case "violet": return dark ? 0xFF8C6BFF : 0xFF6A40E0;
            case "coral":  return dark ? 0xFFFF6B6B : 0xFFE5484D;
            case "mint":   return dark ? 0xFF4AD6A0 : 0xFF1FA877;
            case "pink":   return dark ? 0xFFFF63C0 : 0xFFD6248C;
            default:       return dark ? 0xFF2BB8FF : 0xFF1488D6;
        }
    }

    public static int card(final boolean dark) {
        return dark ? 0xFF0E0E16 : 0xFFFFFFFF;
    }

    public static int cardAlt(final boolean dark) {
        return dark ? 0xFF17171F : 0xFFE4E7EC;
    }

    public static int track(final boolean dark) {
        return dark ? 0xFF2A2A36 : 0xFFD5D9E0;
    }

    public static int text(final boolean dark) {
        return dark ? 0xFFECEBE6 : 0xFF16181D;
    }

    public static int textDim(final boolean dark) {
        return dark ? 0xFF8A8A93 : 0xFF6B6F78;
    }

    public static int softTint(final int color) {
        return (color & 0x00FFFFFF) | 0x24000000;
    }

    public static PendingIntent tapIntent(final Context context, final int widgetId,
                                          final GBDevice device, final String tapKey) {
        final Intent intent;
        if ("charts".equals(tapKey) && device != null) {
            intent = new Intent(context, ActivityChartsActivity.class);
            intent.putExtra(GBDevice.EXTRA_DEVICE, device);
        } else if ("health".equals(tapKey)) {
            intent = new Intent(context, ControlCenterv2.class);
            intent.putExtra(ControlCenterv2.EXTRA_OPEN_TAB, "health");
        } else {
            intent = new Intent(context, ControlCenterv2.class);
        }
        intent.setPackage(context.getPackageName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return PendingIntent.getActivity(context, widgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
