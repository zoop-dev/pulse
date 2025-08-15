/*  Copyright (C) 2019-2024 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.util;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.format.DateFormat;
import android.util.AttributeSet;

import androidx.preference.DialogPreference;

import java.util.Locale;

public class XTimePreference extends DialogPreference {
    protected int hour = 0;
    protected int minute = 0;

    protected Format format = Format.AUTO;

    public XTimePreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected Object onGetDefaultValue(final TypedArray a, final int index) {
        return a.getString(index);
    }

    @Override
    protected void onSetInitialValue(final Object defaultValue) {
        final String time = getPersistedString((String) defaultValue);
        final String[] pieces = time.split(":");

        hour = Integer.parseInt(pieces[0]);
        minute = Integer.parseInt(pieces[1]);

        updateSummary();
    }

    public String getPrefValue() {
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    public void setValue(final int hour, final int minute) {
        this.hour = hour;
        this.minute = minute;

        persistStringValue(getPrefValue());

        updateSummary();
    }

    void updateSummary() {
        if (is24HourFormat())
            setSummary(getTime24h());
        else
            setSummary(getTime12h());
    }

    String getTime24h() {
        return String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private String getTime12h() {
        final String suffix = hour < 12 ? "AM" : "PM";
        final int h = hour > 12 ? hour - 12 : hour;

        return String.format(Locale.ROOT, "%d:%02d %s",h, minute, suffix);
    }

    public void setFormat(final Format format) {
        this.format = format;
    }

    public Format getFormat() {
        return format;
    }

    void persistStringValue(final String value) {
        persistString(value);
    }

    public boolean is24HourFormat() {
        return switch (format) {
            case FORMAT_24H -> true;
            case FORMAT_12H -> false;
            default -> DateFormat.is24HourFormat(getContext());
        };
    }

    public enum Format {
        AUTO,
        FORMAT_24H,
        FORMAT_12H,
    }
}
