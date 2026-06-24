package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.List;

final class DailyCumulativeLineChartHelper {
    private DailyCumulativeLineChartHelper() {
    }

    static void setup(final LineChart chart, final int chartTextColor) {
        chart.getDescription().setEnabled(false);
        chart.setDoubleTapToZoomEnabled(false);

        final XAxis xAxisBottom = chart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(chartTextColor);
        xAxisBottom.setAxisMinimum(0f);
        xAxisBottom.setAxisMaximum(24 * 60 * 60f);

        final YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setAxisMinimum(0);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(chartTextColor);

        final YAxis yAxisRight = chart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    static void setCumulativeData(
            final LineChart chart,
            final List<Entry> entries,
            final ValueFormatter xValueFormatter,
            final String label,
            final int color,
            final int textColor,
            final int goal,
            final float yAxisMaximum
    ) {
        chart.setData(null);

        final LegendEntry legendEntry = new LegendEntry();
        legendEntry.label = label;
        legendEntry.formColor = color;
        chart.getLegend().setTextColor(textColor);
        chart.getLegend().setCustom(Collections.singletonList(legendEntry));

        chart.getXAxis().setValueFormatter(xValueFormatter);

        final LineDataSet lineDataSet = new LineDataSet(entries, label);
        lineDataSet.setColor(color);
        lineDataSet.setDrawCircles(false);
        lineDataSet.setLineWidth(2f);
        lineDataSet.setFillAlpha(255);
        lineDataSet.setCircleColor(color);
        lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
        lineDataSet.setDrawFilled(true);
        lineDataSet.setFillAlpha(60);
        lineDataSet.setFillColor(color);

        final YAxis yAxisLeft = chart.getAxisLeft();
        yAxisLeft.removeAllLimitLines();
        final LimitLine goalLine = new LimitLine(goal);
        goalLine.setLineColor(color);
        goalLine.setLineWidth(1.5f);
        goalLine.enableDashedLine(15f, 10f, 0f);
        yAxisLeft.addLimitLine(goalLine);
        yAxisLeft.setAxisMaximum(yAxisMaximum);

        chart.setData(new LineData(lineDataSet));
    }

    static float maxY(final List<Entry> entries) {
        float maxY = 0f;
        for (final Entry entry : entries) {
            maxY = Math.max(maxY, entry.getY());
        }
        return maxY;
    }

    static ValueFormatter timeValueFormatter(final int startTs, final String simpleDateFormatPattern) {
        return new DayStartValueFormatter(startTs, simpleDateFormatPattern);
    }

    private static class DayStartValueFormatter extends ValueFormatter {
        private final int startTs;
        private final SimpleDateFormat annotationDateFormat;
        private final Calendar cal = GregorianCalendar.getInstance();

        DayStartValueFormatter(final int startTs, final String simpleDateFormatPattern) {
            this.startTs = startTs;
            this.annotationDateFormat = new SimpleDateFormat(simpleDateFormatPattern);
        }

        @Override
        public String getFormattedValue(final float value) {
            cal.clear();
            cal.setTimeInMillis((startTs + (int) value) * 1000L);
            return annotationDateFormat.format(cal.getTime());
        }
    }
}
