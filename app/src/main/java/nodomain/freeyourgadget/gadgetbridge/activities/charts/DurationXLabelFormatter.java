package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.util.Locale;

public class DurationXLabelFormatter extends ValueFormatter {
    public DurationXLabelFormatter() {
    }

    // TODO: this does not work. Cannot use precomputed labels
    @Override
    public String getFormattedValue(final float value) {
        final long shortenSeconds = Math.round(value / 1000);
        final long hours = shortenSeconds / 3600;
        final long minutes = (shortenSeconds % 3600) / 60;
        final long seconds = shortenSeconds % 60;

        if (hours > 0) {
            return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
        }
    }
}
