/*  Copyright (C) 2025  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.ultrahuman;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Locale;

import lineageos.weather.util.WeatherUtils;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

// TODO: updates when multiple devices are active
// TODO: polish and localize the GUI
public class UltrahumanBreathingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanBreathingActivity.class);
    private final ExerciseUpdateReceiver UpdateReceiver = new ExerciseUpdateReceiver();
    private TextView UiStatus;
    private TextView UiHR;
    private TextView UiHRV;
    //private TextView UiMystery;
    private TextView UiTemp;
    private TextView UiTime;

    private TextView UiBatGadget;
    private boolean MetricUnits;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultrahuman_breathing);

        findViewById(R.id.ultrahuman_exercise_start1).setOnClickListener(new ExerciseClick(UltrahumanExercise.BREATHING_START));
        findViewById(R.id.ultrahuman_exercise_stop1).setOnClickListener(new ExerciseClick(UltrahumanExercise.BREATHING_STOP));

        UiStatus = findViewById(R.id.ultrahuman_exercise_type);
        UiTime = findViewById(R.id.ultrahuman_exercise_time);
        UiHR = findViewById(R.id.ultrahuman_exercise_hr_current);
        UiTemp = findViewById(R.id.ultrahuman_exercise_temperature_current);
        UiBatGadget  = findViewById(R.id.ultrahuman_exercise_batGadget);



        UiHRV = findViewById(R.id.ultrahuman_exercise_hrv_current);
        //UiMystery = findViewById(R.id.ultrahuman_breathing_mystery);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DeviceService.ACTION_REALTIME_SAMPLES);
        filter.addAction(UltrahumanConstants.ACTION_EXERCISE_UPDATE);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(UpdateReceiver, filter);

        final GBPrefs prefs = GBApplication.getPrefs();
        final String metric = getString(R.string.p_unit_metric);
        final String unitSystem = prefs.getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, metric);
        MetricUnits = unitSystem.equals(metric);

        Button temperatureUom = findViewById(R.id.ultrahuman_exercise_temperature_uom);
        temperatureUom.setText(MetricUnits ? R.string.unit_celsius : R.string.unit_fahrenheit);

        changeExercise(UltrahumanExercise.CHECK);
    }

    private void changeExercise(UltrahumanExercise exercise) {
        LOG.info("changeExercise {}", exercise);
        final Intent intent = new Intent(UltrahumanConstants.ACTION_CHANGE_EXERCISE);
        intent.setPackage(BuildConfig.APPLICATION_ID);
        intent.putExtra(UltrahumanConstants.EXTRA_EXERCISE, exercise.Code);
        getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(UpdateReceiver);
        super.onDestroy();
    }

    private class ExerciseClick implements View.OnClickListener {
        final UltrahumanExercise Exercise;
        ExerciseClick(UltrahumanExercise exercise){
            Exercise = exercise;
        }
        @Override
        public void onClick(View v) {
            changeExercise(Exercise);
        }
    }

    private class ExerciseUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            UltrahumanExerciseData data = (UltrahumanExerciseData) intent.getSerializableExtra(DeviceService.EXTRA_REALTIME_SAMPLE);

            if (data != null) {
                String exercise = String.format(Locale.ROOT, "[%02x]", 0xFF & data.Exercise);
                UiStatus.setText(exercise);


                if(data.BatteryLevel > -1) {
                    String batGadget = getString(R.string.ultrahuman_exercise_charge, data.BatteryLevel);
                    UiBatGadget.setText(batGadget);
                }

                if (data.Timestamp > -1) {
                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(data.Timestamp * 1000L);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);

                    final String time = getString(R.string.ultrahuman_exercise_time_format, hour, minute, second);
                    UiTime.setText(time);

                    if (data.HR > -1) {
                        String hr = getString(R.string.ultrahuman_exercise_hr_format, data.HR);
                        UiHR.setText(hr);
                    } else {
                        UiHR.setText("");
                    }

                    if (data.Temperature > -1) {
                        double degree;
                        if (MetricUnits) {
                            degree = data.Temperature;
                        } else {
                            degree = WeatherUtils.celsiusToFahrenheit(data.Temperature);
                        }

                        String temp = getString(R.string.ultrahuman_exercise_temperature_format, degree);
                        UiTemp.setText(temp);
                    } else {
                        UiTemp.setText("");
                    }

                    if (data.HRV > 0) {
                        String hrv = getString(R.string.ultrahuman_exercise_hrv_format, data.HRV);
                        UiHRV.setText(hrv);
                    } else if (data.MeasurementType != 0x72) {
                        UiHRV.setText("");
                    }
                }

                /*if (data.Mystery != null) {
                    UiMystery.setText(data.Mystery + " ?");
                } else {
                    UiMystery.setText("");
                }*/
            }
        }
    }
}