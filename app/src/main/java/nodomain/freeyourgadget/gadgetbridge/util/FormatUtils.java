/*  Copyright (C) 2021-2024 Petr Vaněk

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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit;

public class FormatUtils {

    /**
     * This method formats a given number into a string with m (meters), km (kilometers),
     * ft (feet) or mi (miles). These string units can be translated, so the resulting text might
     * be different in each language.
     * The number is also localizes through DecimalFormatSymbols based on current locale.
     *
     * @param distance
     */
    public static String getFormattedDistanceLabel(double distanceMeters) {
        double distanceFeet = distanceMeters * 3.28084f;
        double distanceFormatted = distanceMeters;

        // Keep the unit separate from the number pattern — translated units can contain characters
        // that DecimalFormat would treat as format specifiers and crash on (upstream #6331).
        String formatString = "###";
        String unit = GBApplication.getContext().getString(R.string.meters);
        if (distanceMeters > 2000) {
            distanceFormatted = distanceMeters / 1000;
            formatString = "###.#";
            unit = GBApplication.getContext().getString(R.string.km);
        }
        final DistanceUnit distanceUnit = GBApplication.getPrefs().getDistanceUnit();
        if (distanceUnit == DistanceUnit.IMPERIAL) {
            unit = GBApplication.getContext().getString(R.string.ft);
            distanceFormatted = distanceFeet;
            // switch to miles once past ~0.1 mi so walked distances read consistently as miles
            if (distanceFeet > 500) {
                distanceFormatted = distanceFeet * 0.0001893939f;
                formatString = "###.#";
                unit = GBApplication.getContext().getString(R.string.mi);
            }
        }
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols(GBApplication.getLanguage());
        final DecimalFormat df = new DecimalFormat(formatString, symbols);

        return df.format(distanceFormatted) + unit;
    }
}