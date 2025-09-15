/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiStressParser;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.stress.HuaweiStressScoreCalculation;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HuaweiStressCalibrationFragment extends AbstractGBFragment {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiStressCalibrationFragment.class);

    public static final String EXTRA_STRESS_PROGRESS = "huawei_stress_progress";
    public static final String EXTRA_STRESS_DATA = "huawei_stress_data";
    public static final String EXTRA_STRESS_ERROR = "huawei_stress_error";

    static final String FRAGMENT_TAG = "HUAWEI_STRESS_CALIBRATE_FRAGMENT";

    public static final String ACTION_STRESS_UPDATE = "nodomain.freeyourgadget.gadgetbridge.huawei.stress.start";
    public static final String ACTION_STRESS_RESULT = "nodomain.freeyourgadget.gadgetbridge.huawei.stress.result";

    private GBDevice device;

    private RelativeLayout layoutMeasure;
    private CircularProgressIndicator progress;
    private TextView countdownTime;
    private Button start;

    private RelativeLayout layoutResult;
    private TextView score;
    private TextView level;
    private CheckBox isCalibrate;
    private Slider calibrateSlider;


    private HuaweiStressParser.StressData data = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        final Bundle arguments = getArguments();
        if (arguments == null) {
            return null;
        }
        this.device = arguments.getParcelable(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            return null;
        }

        View rootView = inflater.inflate(R.layout.fragment_huawei_stress_calibrate, container, false);

        layoutMeasure = rootView.findViewById(R.id.huawei_stress_measure);
        progress = rootView.findViewById(R.id.huawei_stress_countdown);
        countdownTime = rootView.findViewById(R.id.huawei_stress_countdown_time);
        start = rootView.findViewById(R.id.huawei_stress_calibrate_start);

        layoutResult = rootView.findViewById(R.id.huawei_stress_result);
        score = rootView.findViewById(R.id.huawei_stress_score);
        level = rootView.findViewById(R.id.huawei_stress_level);
        isCalibrate = rootView.findViewById(R.id.huawei_stress_calibrate);
        calibrateSlider = rootView.findViewById(R.id.huawei_stress_calibrate_slider);
        Button finish = rootView.findViewById(R.id.huawei_stress_calibrate_finish);
        Button again = rootView.findViewById(R.id.huawei_stress_calibrate_again);


        calibrateSlider.setEnabled(false);

        start.setOnClickListener(view -> {
            start.setVisibility(View.GONE);
            startCalibration();
            layoutMeasure.setKeepScreenOn(true);
        });

        again.setOnClickListener(view -> {
            layoutResult.setVisibility(View.GONE);
            layoutMeasure.setVisibility(View.VISIBLE);
            start.setVisibility(View.VISIBLE);
            data = null;
            setProgress(60000);
        });

        finish.setOnClickListener(view -> finishCalibrate());

        isCalibrate.setOnClickListener(view -> calculateScore());

        calibrateSlider.addOnChangeListener((slider, value, fromUser) -> calculateScore());

        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STRESS_UPDATE);
        filter.addAction(ACTION_STRESS_RESULT);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filter);
        return rootView;
    }

    private void setProgress(long j) {
        // Hide button when we get progress.
        // Button can be visible when user has started calibration, closed activity and back again.
        if(start.getVisibility() != View.GONE) {
            start.setVisibility(View.GONE);
        }
        progress.setProgress((int) (j/1000));
        countdownTime.setText(formatTime(j));
    }

    private String getLevelDescription(byte level) {
        return switch (level) {
            case 1 -> getString(R.string.stress_relaxed);
            case 2 -> getString(R.string.stress_mild);
            case 3 -> getString(R.string.stress_moderate);
            case 4 -> getString(R.string.stress_high);
            default -> getString(R.string.n_a);
        };

    }

    private void calculateScore() {
        if(data == null) {
            return;
        }
        if(isCalibrate.isChecked()) {
            calibrateSlider.setEnabled(true);
            float userScore = calibrateSlider.getValue();
            float adjustedScoreFactor = HuaweiStressScoreCalculation.calibrateScoreFactor(userScore, data.scoreFactor);
            data.score = HuaweiStressScoreCalculation.calculateNormalizedFinalScore(adjustedScoreFactor);
        } else {
            calibrateSlider.setEnabled(false);
            data.score = HuaweiStressScoreCalculation.calculateNormalizedFinalScore(data.scoreFactor);
        }
        data.level = HuaweiStressScoreCalculation.calculateLevel(data.score);
        score.setText(String.format(Locale.ROOT, "%02d", data.score));
        level.setText(getLevelDescription(data.level));
    }

    public HuaweiStressParser.StressData getStressData(final Intent intent) {
        String str = intent.getStringExtra(EXTRA_STRESS_DATA);
        if(TextUtils.isEmpty(str)) {
            return null;
        }
        return HuaweiStressParser.stressDataFromJsonStr(str);
    }

    public void finishCalibrate() {
        data.endTime = System.currentTimeMillis(); // Marks the current result as the most recent
        String str = HuaweiStressParser.stressDataToJsonStr(data);
        if(TextUtils.isEmpty(str)) {
            Snackbar.make(layoutMeasure, getString(R.string.huawei_stress_calibrate_error), Snackbar.LENGTH_LONG).show();
            start.setVisibility(View.VISIBLE);
            return;
        }
        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString(HuaweiConstants.PREF_HUAWEI_STRESS_LAST_DATA, str);
        editor.putBoolean(HuaweiConstants.PREF_HUAWEI_STRESS_SWITCH, true);
        editor.apply();
        GBApplication.deviceService(device).onSendConfiguration(HuaweiConstants.PREF_HUAWEI_STRESS_SWITCH);
        if(getActivity() != null) {
            getActivity().finish();
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            if (action == null) {
                LOG.error("Got null action");
                return;
            }

            switch (action) {
                case ACTION_STRESS_UPDATE:
                    long currentCountDownTime = intent.getLongExtra(EXTRA_STRESS_PROGRESS, 0);
                    setProgress(currentCountDownTime);
                    break;
                case ACTION_STRESS_RESULT:
                    layoutMeasure.setKeepScreenOn(false);
                    boolean isError = intent.getBooleanExtra(EXTRA_STRESS_ERROR, true);
                    if(isError) {
                        setProgress(60000);
                        start.setVisibility(View.VISIBLE);
                        Snackbar.make(layoutMeasure, context.getString(R.string.huawei_stress_calibrate_error), Snackbar.LENGTH_LONG).show();
                    } else {
                        data = getStressData(intent);
                        layoutMeasure.setVisibility(View.GONE);
                        layoutResult.setVisibility(View.VISIBLE);
                        calculateScore();
                    }
               break;
                default:
                    LOG.error("Unknown action {}", action);
                    break;
            }
        }
    };

    private String formatTime(long milliSeconds) {
        return String.format(Locale.ROOT, "%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)));
    }

    private void setDevice(final GBDevice device) {
        final Bundle args = getArguments() != null ? getArguments() : new Bundle();
        args.putParcelable("device", device);
        setArguments(args);
    }


    @Override
    public void onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver);
        super.onDestroyView();
    }

    static HuaweiStressCalibrationFragment newInstance(final GBDevice device) {
        final HuaweiStressCalibrationFragment fragment = new HuaweiStressCalibrationFragment();
        fragment.setDevice(device);
        return fragment;
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return getString(R.string.huawei_stress_calibrate);
    }

    private void startCalibration() {
        GBApplication.deviceService(device).onSendConfiguration(HuaweiConstants.PREF_HUAWEI_STRESS_CALIBRATE);
    }
}
