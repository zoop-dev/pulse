package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

public class CaloriesPeriodFragment extends CaloriesFragment<CaloriesPeriodFragment.CaloriesData> {
    protected static final Logger LOG = LoggerFactory.getLogger(CaloriesPeriodFragment.class);

    private TextView mDateView;
    private TextView activeCaloriesAvg;
    private TextView activeCaloriesTotal;
    private TextView restingCaloriesAvg;
    private TextView restingCaloriesTotal;
    private BarChart caloriesChart;

    private TextView mBalanceView;

    protected int CHART_TEXT_COLOR;
    protected int TEXT_COLOR;
    protected int CALORIES_GOAL;

    protected int BACKGROUND_COLOR;
    protected int DESCRIPTION_COLOR;

    @Override
    protected boolean isSingleDay() {
        return false;
    }

    public static CaloriesPeriodFragment newInstance(int totalDays) {
        CaloriesPeriodFragment fragmentFirst = new CaloriesPeriodFragment();
        Bundle args = new Bundle();
        args.putInt("totalDays", totalDays);
        fragmentFirst.setArguments(args);
        return fragmentFirst;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TOTAL_DAYS = getArguments() != null ? getArguments().getInt("totalDays") : 0;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_calories_period, container, false);

        rootView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            getChartsHost().enableSwipeRefresh(scrollY == 0);
        });

        mDateView = rootView.findViewById(R.id.calories_date_view);
        caloriesChart = rootView.findViewById(R.id.calories_chart);
        activeCaloriesAvg = rootView.findViewById(R.id.active_calories_avg);
        activeCaloriesTotal = rootView.findViewById(R.id.active_calories_total);
        restingCaloriesAvg = rootView.findViewById(R.id.resting_calories_avg);
        restingCaloriesTotal = rootView.findViewById(R.id.resting_calories_total);
        CALORIES_GOAL = GBApplication.getPrefs().getInt(ActivityUser.PREF_USER_CALORIES_BURNT, ActivityUser.defaultUserCaloriesBurntGoal);

        mBalanceView = rootView.findViewById(R.id.balance);

        setupCaloriesChart();
        refresh();

        return rootView;
    }

    protected void setupCaloriesChart() {
        caloriesChart.getDescription().setEnabled(false);
        if (TOTAL_DAYS <= 7) {
            caloriesChart.setTouchEnabled(false);
            caloriesChart.setPinchZoom(false);
        }
        caloriesChart.setDoubleTapToZoomEnabled(false);
        caloriesChart.getLegend().setEnabled(false);

        final XAxis xAxisBottom = caloriesChart.getXAxis();
        xAxisBottom.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxisBottom.setDrawLabels(true);
        xAxisBottom.setDrawGridLines(false);
        xAxisBottom.setEnabled(true);
        xAxisBottom.setDrawLimitLinesBehindData(true);
        xAxisBottom.setTextColor(CHART_TEXT_COLOR);

        final YAxis yAxisLeft = caloriesChart.getAxisLeft();
        yAxisLeft.setDrawGridLines(true);
        yAxisLeft.setDrawTopYLabelEntry(true);
        yAxisLeft.setEnabled(true);
        yAxisLeft.setTextColor(CHART_TEXT_COLOR);
        yAxisLeft.setAxisMinimum(0f);
        final LimitLine goalLine = new LimitLine(CALORIES_GOAL);
        goalLine.setLineColor(getResources().getColor(R.color.calories_color));
        goalLine.setLineWidth(1.5f);
        goalLine.enableDashedLine(15f, 10f, 0f);
        yAxisLeft.addLimitLine(goalLine);

        final YAxis yAxisRight = caloriesChart.getAxisRight();
        yAxisRight.setEnabled(true);
        yAxisRight.setDrawLabels(false);
        yAxisRight.setDrawGridLines(false);
        yAxisRight.setDrawAxisLine(true);
    }

    @Override
    public String getTitle() {
        return getString(R.string.calories);
    }

    @Override
    protected void init() {
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(requireContext());
        BACKGROUND_COLOR = GBApplication.getBackgroundColor(getContext());
        DESCRIPTION_COLOR = TEXT_COLOR = GBApplication.getTextColor(getContext());
        CHART_TEXT_COLOR = GBApplication.getSecondaryTextColor(getContext());
    }

    @Override
    protected CaloriesData refreshInBackground(ChartsHost chartsHost, DBHandler db, GBDevice device) {
        Calendar day = Calendar.getInstance();
        day.setTime(getEndDate());
        List<CaloriesDay> caloriesDaysData = getMyCaloriesDaysData(db, day, device);
        return new CaloriesData(caloriesDaysData);
    }

    @Override
    protected void updateChartsnUIThread(CaloriesData caloriesData) {
        mDateView.setText(DateTimeUtils.formatDaysUntil(TOTAL_DAYS, getTSEnd()));
        caloriesChart.setData(null);

        List<BarEntry> entries = new ArrayList<>();
        int counter = 0;
        for (CaloriesDay day : caloriesData.days) {
            entries.add(new BarEntry(counter, day.activeCalories));
            counter++;
        }
        BarDataSet set = new BarDataSet(entries, "Calories");
        set.setDrawValues(true);
        set.setColors(getResources().getColor(R.color.calories_color));
        final XAxis x = caloriesChart.getXAxis();
        x.setValueFormatter(getCaloriesChartDayValueFormatter(caloriesData));
        caloriesChart.getAxisLeft().setAxisMaximum((float) Math.max(set.getYMax() * 1.1, CALORIES_GOAL));

        BarData barData = new BarData(set);
        barData.setValueTextColor(TEXT_COLOR); //prevent tearing other graph elements with the black text. Another approach would be to hide the values cmpletely with data.setDrawValues(false);
        barData.setValueTextSize(10f);
        if (TOTAL_DAYS > 7) {
            caloriesChart.setRenderer(new AngledLabelsChartRenderer(caloriesChart, caloriesChart.getAnimator(), caloriesChart.getViewPortHandler()));
        }
        caloriesChart.setData(barData);
        activeCaloriesAvg.setText(String.format(String.valueOf(caloriesData.activeCaloriesDailyAvg)));
        activeCaloriesTotal.setText(String.format(String.valueOf(caloriesData.totalActiveCalories)));
        restingCaloriesAvg.setText(String.format(String.valueOf(caloriesData.restingCaloriesDailyAvg)));
        restingCaloriesTotal.setText(String.format(String.valueOf(caloriesData.totalRestingCalories)));

        mBalanceView.setText(caloriesData.getBalanceMessage(getContext(), CALORIES_GOAL));
    }

    ValueFormatter getCaloriesChartDayValueFormatter(CaloriesPeriodFragment.CaloriesData caloriesData) {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                CaloriesPeriodFragment.CaloriesDay day = caloriesData.days.get((int) value);
                String pattern = TOTAL_DAYS > 7 ? "dd" : "EEE";
                SimpleDateFormat formatLetterDay = new SimpleDateFormat(pattern, Locale.getDefault());
                return formatLetterDay.format(new Date(day.day.getTimeInMillis()));
            }
        };
    }

    @Override
    protected void renderCharts() {
        caloriesChart.invalidate();
    }

    protected void setupLegend(Chart<?> chart) {
    }

    protected static class CaloriesData extends ChartsData {
        List<CaloriesDay> days;
        long activeCaloriesDailyAvg = 0;
        long totalActiveCalories = 0;
        long restingCaloriesDailyAvg = 0;
        long totalRestingCalories = 0;
        CaloriesDay todayCaloriesDay;

        protected CaloriesData(List<CaloriesDay> days) {
            this.days = days;
            int daysCounter = days.size();
            for (CaloriesDay day : days) {
                this.totalActiveCalories += day.activeCalories;
                this.totalRestingCalories += day.restingCalories;
            }
            if (daysCounter > 0) {
                this.activeCaloriesDailyAvg = this.totalActiveCalories / daysCounter;
                this.restingCaloriesDailyAvg = this.totalRestingCalories / daysCounter;
            }
            this.todayCaloriesDay = days.get(days.size() - 1);
        }

        protected String getBalanceMessage(final Context context, final int targetValue) {
            if (totalActiveCalories == 0) {
                return context.getString(R.string.no_data);
            }

            final long totalBalance = totalActiveCalories - ((long) targetValue * days.size());
            if (totalBalance > 0) {
                return context.getString(R.string.calorie_over_goal, Math.abs(totalBalance));
            } else {
                return context.getString(R.string.calorie_under_goal, Math.abs(totalBalance));
            }
        }
    }
}
