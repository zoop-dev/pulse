/*  Copyright (C) 2025 Gideon Zenz

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

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.dialogs.MaterialDialogFragment;

public class HealthConnectInitialSyncDialog extends MaterialDialogFragment {
    private static final Logger LOG = LoggerFactory.getLogger(HealthConnectInitialSyncDialog.class);

    private RadioGroup radioGroup;
    private RadioButton radioPreset;
    private RadioButton radioCustomDate;
    private Spinner spinnerPreset;
    private Button buttonSelectDate;
    private LocalDate customSelectedDate = null;

    public interface InitialSyncDialogListener {
        void onSyncPeriodSelected();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = requireContext();
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_health_connect_initial_sync, null);

        radioGroup = dialogView.findViewById(R.id.radio_group_sync_period);
        radioPreset = dialogView.findViewById(R.id.radio_preset);
        radioCustomDate = dialogView.findViewById(R.id.radio_custom_date);
        spinnerPreset = dialogView.findViewById(R.id.spinner_sync_period);
        buttonSelectDate = dialogView.findViewById(R.id.button_select_date);

        // Setup preset spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
                R.array.health_connect_initial_sync_periods, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPreset.setAdapter(adapter);
        spinnerPreset.setSelection(1); // Default to 7 days

        // Radio button listeners to enable/disable controls
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_preset) {
                spinnerPreset.setEnabled(true);
                buttonSelectDate.setEnabled(false);
            } else if (checkedId == R.id.radio_custom_date) {
                spinnerPreset.setEnabled(false);
                buttonSelectDate.setEnabled(true);
            }
        });

        // Initially select preset option
        radioPreset.setChecked(true);
        spinnerPreset.setEnabled(true);
        buttonSelectDate.setEnabled(false);

        // Date picker button
        buttonSelectDate.setOnClickListener(v -> showDatePicker());

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
        builder.setView(dialogView)
                .setTitle(R.string.health_connect_initial_sync_dialog_title)
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    handleSyncPeriodSelection();
                })
                .setNegativeButton(R.string.Cancel, (dialog, id) -> dismiss());

        return builder.create();
    }

    private void showDatePicker() {
        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.health_connect_initial_sync_select_date)
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Convert UTC milliseconds to LocalDate in system timezone
            customSelectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            buttonSelectDate.setText(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(customSelectedDate));
        });

        datePicker.show(getParentFragmentManager(), "DATE_PICKER");
    }

    private void handleSyncPeriodSelection() {
        long startTimestampSeconds;

        if (radioPreset.isChecked()) {
            int selectedPosition = spinnerPreset.getSelectedItemPosition();
            int daysToSync;
            switch (selectedPosition) {
                case 0: // 3 days
                    daysToSync = 3;
                    break;
                case 1: // 7 days
                    daysToSync = 7;
                    break;
                case 2: // 30 days
                    daysToSync = 30;
                    break;
                case 3: // Sync all
                    daysToSync = -1; // Special value to indicate sync all
                    break;
                default:
                    daysToSync = 7; // Default fallback
            }

            if (daysToSync == -1) {
                startTimestampSeconds = -1;
                LOG.info("Initial sync period selected: Sync all data");
            } else {
                Instant now = Instant.now();
                startTimestampSeconds = now.minusSeconds(daysToSync * 24L * 60L * 60L).getEpochSecond();
                LOG.info("Initial sync period selected: {} days (from preset)", daysToSync);
            }

        } else if (radioCustomDate.isChecked() && customSelectedDate != null) {
            startTimestampSeconds = customSelectedDate.atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .getEpochSecond();
            LOG.info("Initial sync period selected: custom date {}", customSelectedDate);

        } else {
            // Fallback to 7 days if nothing selected properly
            Instant now = Instant.now();
            startTimestampSeconds = now.minusSeconds(7 * 24L * 60L * 60L).getEpochSecond();
            LOG.warn("No valid sync period selected, defaulting to 7 days");
        }

        GBApplication.getContext()
                .getSharedPreferences(GBPrefs.HEALTH_CONNECT_SETTINGS, Context.MODE_PRIVATE)
                .edit()
                .putLong(GBPrefs.HEALTH_CONNECT_INITIAL_SYNC_START_TS, startTimestampSeconds)
                .commit();

        dismiss();
        showSyncNowDialog();
    }

    private void showSyncNowDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.health_connect_sync_now_title)
                .setMessage(R.string.health_connect_sync_now_message)
                .setPositiveButton(R.string.health_connect_sync_now, (dialog, which) -> {
                    if (getTargetFragment() instanceof InitialSyncDialogListener) {
                        ((InitialSyncDialogListener) getTargetFragment()).onSyncPeriodSelected();
                    } else if (getParentFragment() instanceof InitialSyncDialogListener) {
                        ((InitialSyncDialogListener) getParentFragment()).onSyncPeriodSelected();
                    }
                })
                .setNegativeButton(R.string.health_connect_sync_later, (dialog, which) -> {
                })
                .setCancelable(false)
                .show();
    }
}

