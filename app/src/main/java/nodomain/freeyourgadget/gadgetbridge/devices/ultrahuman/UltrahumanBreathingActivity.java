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
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

// TODO: verify GUI lifecycle
// TODO: updates when multiple devices are active
// TODO: polish and localize the GUI
public class UltrahumanBreathingActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(UltrahumanBreathingActivity.class);
    private final ExerciseUpdateReceiver UpdateReceiver = new ExerciseUpdateReceiver();
    private TextView UiStatus;
    private TextView UiHR;
    private TextView UiHRV;
    private TextView UiTemp;
    private TextView UiTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultrahuman_breathing);

        findViewById(R.id.ultrahuman_breathing_start).setOnClickListener(new StartListener());
        findViewById(R.id.ultrahuman_breathing_stop).setOnClickListener(new StopListener());
        UiStatus = findViewById(R.id.ultrahuman_breathing_status);
        UiHR = findViewById(R.id.ultrahuman_breathing_HR);
        UiTemp = findViewById(R.id.ultrahuman_breathing_temp);
        UiHRV = findViewById(R.id.ultrahuman_breathing_HRV);
        UiTime = findViewById(R.id.ultrahuman_breathing_time);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UltrahumanConstants.ACTION_EXERCISE_UPDATE);
        ContextCompat.registerReceiver(getApplicationContext(), UpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);

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
        getApplicationContext().unregisterReceiver(UpdateReceiver);
        super.onDestroy();
    }

    private class StartListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            changeExercise(UltrahumanExercise.BREATHING_START);
        }
    }

    private class StopListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            changeExercise(UltrahumanExercise.BREATHING_STOP);
        }
    }

    private class ExerciseUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            UltrahumanExerciseData data = (UltrahumanExerciseData) intent.getSerializableExtra(UltrahumanConstants.EXTRA_EXERCISE);
            if (data != null) {
                String update = String.format(Locale.US, "b:%s%% t:%x", data.BatteryLevel, 0xFF & data.Exercise);
                UiStatus.setText(update);

                if (data.Timestamp > -1) {
                    // todo - display local time
                    final Calendar calendar = DateTimeUtils.getCalendarUTC();
                    calendar.setTimeInMillis(data.Timestamp * 1000L);
                    int hour = calendar.get(Calendar.HOUR_OF_DAY);
                    int minute = calendar.get(Calendar.MINUTE);
                    int second = calendar.get(Calendar.SECOND);
                    String time = String.format(Locale.US, "%02d:%02d:%02d", hour, minute, second);
                    UiTime.setText(time);
                }

                if (data.HR > -1) {
                    String hr = String.format(Locale.US, "%s bpm", data.HR);
                    UiHR.setText(hr);
                }
                if (data.HRV > 0) {
                    String hrv = String.format(Locale.US, "%s ms", data.HRV);
                    UiHRV.setText(hrv);
                }
                if (data.Temperature > -1) {
                    String temp = String.format(Locale.US, "%.3f °C", data.Temperature);
                    UiTemp.setText(temp);
                }
            }
        }
    }
}