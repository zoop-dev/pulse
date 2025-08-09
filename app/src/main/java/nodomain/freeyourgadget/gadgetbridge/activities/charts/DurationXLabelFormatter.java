package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.annotation.SuppressLint;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DurationXLabelFormatter extends ValueFormatter {
    @SuppressLint("SimpleDateFormat")
    private final SimpleDateFormat annotationDateFormat;

    private final Calendar cal = GregorianCalendar.getInstance();

    public DurationXLabelFormatter(String simpleDateFormatPattern) {
        this.annotationDateFormat = new SimpleDateFormat(simpleDateFormatPattern);
    }

    // TODO: this does not work. Cannot use precomputed labels
    @Override
    public String getFormattedValue(final float value) {
        cal.clear();
        final int ts = (int) value;
        cal.setTimeInMillis(ts);
        final Date date = cal.getTime();
        return annotationDateFormat.format(date);
    }
}