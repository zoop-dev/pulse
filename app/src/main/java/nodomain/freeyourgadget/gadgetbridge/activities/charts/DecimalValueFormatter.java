/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Renders a chart axis/marker value with a fixed number of decimals and a locale-independent
 * decimal separator. Workout chart data is already converted to the display unit before plotting
 * (so the chart library picks round ticks in that unit), so this formatter only formats – it
 * does not convert.
 */
public class DecimalValueFormatter extends ValueFormatter {
    private final DecimalFormat decimalFormat;

    public DecimalValueFormatter(final int decimals) {
        final StringBuilder pattern = new StringBuilder("0");
        for (int i = 0; i < decimals; i++) {
            if (i == 0) {
                pattern.append('.');
            }
            pattern.append('0');
        }
        this.decimalFormat = new DecimalFormat(pattern.toString(), DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    public String format(final double value) {
        return decimalFormat.format(value);
    }

    @Override
    public String getFormattedValue(final float value) {
        return format(value);
    }
}
