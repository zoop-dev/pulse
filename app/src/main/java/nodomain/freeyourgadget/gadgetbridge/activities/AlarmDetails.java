/*  Copyright (C) 2015-2024 Andreas Shimokawa, Carsten Pfeiffer, Daniel
    Dakhno, Daniele Gobbetti, Dmitry Markin, Lem Dulfo, Taavi Eomäe, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityAlarmDetailsBinding;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_LABEL;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm.ALARM_SOUND;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AlarmDetails extends AbstractGBActivity {
    private ActivityAlarmDetailsBinding binding;

    private Alarm alarm;
    private GBDevice device;

    private final Map<String, ALARM_SOUND> textToAlarmSound = new HashMap<>(ALARM_SOUND.values().length);
    private final Map<String, ALARM_LABEL> textToAlarmLabel = new HashMap<>(ALARM_LABEL.values().length);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAlarmDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        alarm = (Alarm) getIntent().getSerializableExtra(nodomain.freeyourgadget.gadgetbridge.model.Alarm.EXTRA_ALARM);
        device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        if (device == null) {
            throw new IllegalArgumentException("No device provided to AlarmDetails");
        }

        binding.cbSmartWakeup.setOnClickListener(v -> {
            ((CheckedTextView) v).toggle();
            binding.cbSmartWakeupInterval.setEnabled(((CheckedTextView) v).isChecked());
        });
        binding.cbSnooze.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbMonday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbTuesday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbWednesday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbThursday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbFriday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbSaturday.setOnClickListener(v -> ((CheckedTextView) v).toggle());
        binding.cbSunday.setOnClickListener(v -> ((CheckedTextView) v).toggle());

        binding.timePicker.setIs24HourView(DateFormat.is24HourFormat(GBApplication.getContext()));
        binding.timePicker.setCurrentHour(alarm.getHour());
        binding.timePicker.setCurrentMinute(alarm.getMinute());

        boolean smartAlarmSupported = supportsSmartWakeup(alarm.getPosition());
        boolean smartAlarmForced = forcedSmartWakeup(alarm.getPosition());
        boolean smartAlarmIntervalSupported = supportsSmartWakeupInterval(alarm.getPosition());

        binding.cbSmartWakeup.setChecked(alarm.getSmartWakeup() || smartAlarmForced);
        binding.cbSmartWakeup.setVisibility(smartAlarmSupported ? View.VISIBLE : View.GONE);
        if (smartAlarmForced) {
            binding.cbSmartWakeup.setEnabled(false);
            // Force the text to be visible for the "interval" part
            // Enabled or not can still be seen in the checkmark
            // TODO: I'd like feedback on this
            if (GBApplication.isDarkThemeEnabled())
                binding.cbSmartWakeup.setTextColor(getResources().getColor(android.R.color.secondary_text_dark));
            else
                binding.cbSmartWakeup.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        }
        if (smartAlarmIntervalSupported)
            binding.cbSmartWakeup.setText(R.string.alarm_smart_wakeup_interval);

        binding.cbSmartWakeupInterval.setVisibility(smartAlarmSupported && smartAlarmIntervalSupported ? View.VISIBLE : View.GONE);
        binding.cbSmartWakeupInterval.setEnabled(alarm.getSmartWakeup() || smartAlarmForced);
        if (alarm.getSmartWakeupInterval() != null) {
            binding.cbSmartWakeupInterval.setText(NumberFormat.getInstance().format(alarm.getSmartWakeupInterval()));
        }
        binding.cbSmartWakeupInterval.setFilters(new InputFilter[]{
                new InputFilter() {
                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                        if (dend >= 3) // Limit length
                            return "";

                        String strValue = dest.subSequence(0, dstart) + source.subSequence(start, end).toString() + dest.subSequence(dend, dest.length());
                        try {
                            int value = Integer.parseInt(strValue);
                            if (value > 255) {
                                binding.cbSmartWakeupInterval.setText("255");
                                binding.cbSmartWakeupInterval.setSelection(3); // Move cursor to end
                            }
                        } catch (NumberFormatException e) {
                            return "";
                        }
                        return null;
                    }
                }
        });

        binding.cbSnooze.setChecked(alarm.getSnooze());
        binding.cbSnooze.setVisibility(supportsSnoozing() ? View.VISIBLE : View.GONE);

        binding.cbBacklight.setChecked(alarm.getBacklight());
        binding.cbBacklight.setVisibility(supportsAlarmBacklight() ? View.VISIBLE : View.GONE);
        binding.cbBacklight.setOnClickListener(v -> ((CheckedTextView) v).toggle());

        binding.presetLabelLayout.setVisibility(supportsAlarmTitlePresets() ? View.VISIBLE : View.GONE);
        if (supportsAlarmTitlePresets()) {
            final List<ALARM_LABEL> alarmTitlePresets = getAlarmTitlePresets();
            String[] items = new String[alarmTitlePresets.size()];
            for (int i = 0; i < alarmTitlePresets.size(); i++) {
                items[i] = getString(alarmTitlePresets.get(i).getLabel());
                if (alarmTitlePresets.get(i).name().equals(alarm.getTitle())) {
                    binding.presetLabelSpinner.setText(items[i], false);
                }
                textToAlarmLabel.put(items[i], alarmTitlePresets.get(i));
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
            binding.presetLabelSpinner.setAdapter(adapter);
            if (StringUtils.isEmpty(binding.presetLabelSpinner.getText().toString())) {
                binding.presetLabelSpinner.setText(items[0], false);
            }
        }

        binding.soundModeLayout.setVisibility(supportsAlarmSounds() ? View.VISIBLE : View.GONE);
        if (supportsAlarmSounds()) {
            String[] items = new String[ALARM_SOUND.values().length - 1];
            for (int i = 0; i < items.length; i++) {
                final ALARM_SOUND alarmSound = ALARM_SOUND.values()[i + 1]; // skip unused
                items[i] = getString(alarmSound.getLabel());
                if (alarm.getSoundCode() == i) {
                    binding.soundModeSpinner.setText(items[i], false);
                }
                textToAlarmSound.put(items[i], alarmSound);
            }
            final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
            binding.soundModeSpinner.setAdapter(adapter);
        }

        binding.title.setVisibility(supportsTitle() ? View.VISIBLE : View.GONE);
        binding.title.setText(alarm.getTitle());

        final int titleLimit = getAlarmTitleLimit();
        if (titleLimit > 0) {
            binding.title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(titleLimit)});
        }

        binding.description.setVisibility(supportsDescription() ? View.VISIBLE : View.GONE);
        binding.description.setText(alarm.getDescription());

        binding.cbMonday.setChecked(alarm.getRepetition(Alarm.ALARM_MON));
        binding.cbTuesday.setChecked(alarm.getRepetition(Alarm.ALARM_TUE));
        binding.cbWednesday.setChecked(alarm.getRepetition(Alarm.ALARM_WED));
        binding.cbThursday.setChecked(alarm.getRepetition(Alarm.ALARM_THU));
        binding.cbFriday.setChecked(alarm.getRepetition(Alarm.ALARM_FRI));
        binding.cbSaturday.setChecked(alarm.getRepetition(Alarm.ALARM_SAT));
        binding.cbSunday.setChecked(alarm.getRepetition(Alarm.ALARM_SUN));
    }

    private boolean supportsSmartWakeup(int position) {
        return device.getDeviceCoordinator().supportsSmartWakeup(device, position);
    }

    private boolean supportsSmartWakeupInterval(int position) {
        return device.getDeviceCoordinator().supportsSmartWakeupInterval(device, position);
    }

    /**
     * The alarm at this position *must* be a smart alarm
     */
    private boolean forcedSmartWakeup(int position) {
        return device.getDeviceCoordinator().forcedSmartWakeup(device, position);
    }

    private boolean supportsTitle() {
        return device.getDeviceCoordinator().supportsAlarmTitle(device);
    }

    private int getAlarmTitleLimit() {
        return device.getDeviceCoordinator().getAlarmTitleLimit(device);
    }

    private boolean supportsDescription() {
        return device.getDeviceCoordinator().supportsAlarmDescription(device);
    }

    private boolean supportsSnoozing() {
        return device.getDeviceCoordinator().supportsAlarmSnoozing(device);
    }

    private boolean supportsAlarmSounds() {
        return device.getDeviceCoordinator().supportsAlarmSounds(device);
    }

    private boolean supportsAlarmBacklight() {
        return device.getDeviceCoordinator().supportsAlarmBacklight(device);
    }

    private boolean supportsAlarmTitlePresets() {
        return device.getDeviceCoordinator().supportsAlarmTitlePresets(device);
    }

    private List<ALARM_LABEL> getAlarmTitlePresets() {
        return device.getDeviceCoordinator().getAlarmTitlePresets(device);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // back button
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateAlarm() {
        // Set alarm as used and enabled if time has changed
        if (alarm.getUnused() && alarm.getHour() != binding.timePicker.getCurrentHour() || alarm.getMinute() != binding.timePicker.getCurrentMinute()) {
            alarm.setUnused(false);
            alarm.setEnabled(true);
        }
        alarm.setSmartWakeup(supportsSmartWakeup(alarm.getPosition()) && binding.cbSmartWakeup.isChecked());
        String interval = binding.cbSmartWakeupInterval.getText().toString();
        alarm.setSmartWakeupInterval(interval.isEmpty() ? null : Integer.parseInt(interval));
        alarm.setSnooze(supportsSnoozing() && binding.cbSnooze.isChecked());
        int repetitionMask = AlarmUtils.createRepetitionMask(
                binding.cbMonday.isChecked(),
                binding.cbTuesday.isChecked(),
                binding.cbWednesday.isChecked(),
                binding.cbThursday.isChecked(),
                binding.cbFriday.isChecked(),
                binding.cbSaturday.isChecked(),
                binding.cbSunday.isChecked()
        );
        alarm.setRepetition(repetitionMask);
        alarm.setHour(binding.timePicker.getCurrentHour());
        alarm.setMinute(binding.timePicker.getCurrentMinute());
        alarm.setBacklight(binding.cbBacklight.isChecked());
        if (supportsAlarmTitlePresets()) {
            final ALARM_LABEL alarmLabel = textToAlarmLabel.get(binding.presetLabelSpinner.getText().toString());
            if (alarmLabel != null) {
                alarm.setTitle(alarmLabel.name());
            } else {
                alarm.setTitle("");
            }
        } else {
            alarm.setTitle(binding.title.getText().toString());
        }
        final ALARM_SOUND alarmSound = textToAlarmSound.get(binding.soundModeSpinner.getText().toString());
        alarm.setSoundCode(Objects.requireNonNullElse(alarmSound, ALARM_SOUND.UNSET).ordinal());
        alarm.setDescription(binding.description.getText().toString());
        DBHelper.store(alarm);
    }

    @Override
    protected void onPause() {
        updateAlarm();
        super.onPause();
    }
}
