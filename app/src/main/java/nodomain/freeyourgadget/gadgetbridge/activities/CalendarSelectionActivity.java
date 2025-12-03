/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniele Gobbetti, José Rebelo,
    Ludovic Jozeau

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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NavUtils;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;


public class CalendarSelectionActivity extends AbstractGBActivity {
    private GBDevice gbDevice;
    private CalendarManager calendarManager;
    private List<CalendarManager.CalendarEntry> calendars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_selection);
        ListView calListView = (ListView) findViewById(R.id.calendar_selection_list_view);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) !=
            PackageManager.PERMISSION_GRANTED) {
            GB.toast(this, "Calendar permission not granted. Nothing to do.", Toast.LENGTH_SHORT,
                     GB.WARN);
            return;
        }

        gbDevice = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        calendarManager = new CalendarManager(this, gbDevice.getAddress());
        calendars = calendarManager.getCalendars();

        CalendarListAdapter calAdapter = new CalendarListAdapter(this, calendarManager, calendars);
        calListView.setAdapter(calAdapter);
        calListView.setOnItemClickListener(calAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class CalendarListAdapter extends ArrayAdapter<CalendarManager.CalendarEntry>
            implements AdapterView.OnItemClickListener {
        private final CalendarManager calendarManager;

        CalendarListAdapter(@NonNull Context context, CalendarManager calendarManager,
                            @NonNull List<CalendarManager.CalendarEntry> calendars) {
            super(context, 0, calendars);
            this.calendarManager = calendarManager;
        }

        private void setRowStyle(View view, boolean enabled) {
            // Set the state of the checkbox.
            CheckBox checked = (CheckBox) view.findViewById(R.id.item_checkbox);
            checked.setChecked(enabled);

            // Make the color slightly transparent when disabled.
            View colorBox = view.findViewById(R.id.calendar_color);
            Drawable background = colorBox.getBackground();
            background.setAlpha(enabled ? 0xFF : 0x2F);
            colorBox.setBackground(background);

            // Grey out the text if not enabled.
            view.findViewById(R.id.calendar_name).setEnabled(enabled);
            view.findViewById(R.id.calendar_owner_account).setEnabled(enabled);

            // If the color selection button is enabled, make it visible if the row is also enabled.
            ImageView colorButton = (ImageView) view.findViewById(R.id.calendar_color_drop_down);
            colorButton.setVisibility(
                    (enabled && colorButton.isEnabled()) ? View.VISIBLE : View.GONE);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            CalendarManager.CalendarEntry item = getItem(position);

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) super.getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_calendar_selection, parent, false);
            }

            // Populate each element
            ((TextView) view.findViewById(R.id.calendar_name)).setText(item.displayName());
            ((TextView) view.findViewById(R.id.calendar_owner_account)).setText(item.accountName());
            view.findViewById(R.id.calendar_color).setBackgroundColor(item.color());
            View colorDropDown = view.findViewById(R.id.calendar_color_drop_down);
            colorDropDown.setEnabled(!item.eventColors().isEmpty());

            if (!item.eventColors().isEmpty()) {
                colorDropDown.setOnClickListener((button) -> {
                    // Create a new layout for the dialog
                    LayoutInflater inflater =
                            (LayoutInflater) super.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View colorView =
                            inflater.inflate(R.layout.activity_calendar_color_selection, null);

                    GridView colorListView =
                            (GridView) colorView.findViewById(R.id.calendar_color_selection_list_view);

                    CalendarColorGridAdapter colorAdaptor =
                            new CalendarColorGridAdapter(super.getContext(), calendarManager,
                                                         item.getUniqueString(), item.eventColors().stream().sorted().toList());
                    colorListView.setAdapter(colorAdaptor);
                    colorListView.setOnItemClickListener(colorAdaptor);

                    new MaterialAlertDialogBuilder(getContext())
                        .setTitle(R.string.pref_title_calendar_sync_colors)
                        .setView(colorView).setIcon(R.drawable.ic_calendar_cancel)
                        .setPositiveButton(R.string.done, null)
                        .show();
                });
            }

            setRowStyle(view, !calendarManager.isCalendarBlacklisted(item.getUniqueString()));
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            CalendarManager.CalendarEntry item = getItem(position);
            // Invert the checkbox on click before checking it's state below.
            CheckBox selected = (CheckBox) view.findViewById(R.id.item_checkbox);
            selected.toggle();

            setRowStyle(view, selected.isChecked());

            if (selected.isChecked()) {
                calendarManager.removeFromCalendarBlacklist(item.getUniqueString());
            } else {
                calendarManager.addCalendarToBlacklist(item.getUniqueString());
            }

            GBApplication.deviceService(gbDevice).onSendConfiguration(GBPrefs.CALENDAR_BLACKLIST);
        }
    }

    private class CalendarColorGridAdapter extends ArrayAdapter<Integer>
            implements AdapterView.OnItemClickListener {
        private final CalendarManager calendarManager;
        private final String calendarUniqueName;

        CalendarColorGridAdapter(@NonNull Context context, @NonNull CalendarManager calendarManager,
                                 @NonNull String calendarUniqueName,
                                 @NonNull List<Integer> colors) {
            super(context, 0, colors);
            this.calendarManager = calendarManager;
            this.calendarUniqueName = calendarUniqueName;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
            int color = getItem(position);

            if (view == null) {
                LayoutInflater inflater = (LayoutInflater) super.getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.item_calendar_color_selection, parent, false);
            }
            CheckBox checked = (CheckBox) view.findViewById(R.id.item_event_color);
            checked.setChecked(!calendarManager.isColorBlacklistedForCalendar(calendarUniqueName, color));
            checked.setButtonTintList(ColorStateList.valueOf(0xFF000000 | color));
            checked.setBackgroundColor(0xFF000000 | color);
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int color = getItem(position);
            // Invert the checkbox on click before checking it's state below.
            CheckBox selected = (CheckBox) view.findViewById(R.id.item_event_color);
            selected.toggle();
            if (selected.isChecked()) {
                calendarManager.removeColorFromBlacklistForCalendar(calendarUniqueName, color);
            } else {
                calendarManager.addColorToBlacklistForCalendar(calendarUniqueName, color);
            }
            GBApplication.deviceService(gbDevice).onSendConfiguration(
                    DeviceSettingsPreferenceConst.PREF_CALENDAR_SYNC_COLOR_BLACKLIST);
        }
    }
}
