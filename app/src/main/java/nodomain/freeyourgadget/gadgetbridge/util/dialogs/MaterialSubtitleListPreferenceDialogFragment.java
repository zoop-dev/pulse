/*  Copyright (C) 2026 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.dialogs;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.SubtitleListPreference;

/**
 * Shows a single-choice picker for a {@link SubtitleListPreference}, rendering each entry as a
 * title over its subtitle. If the preference allows a custom value, an extra "Custom…" row is
 * appended, which opens a {@link SubtitleListCustomValueDialogFragment} to let the user type a
 * value that is not one of the known entries.
 */
public class MaterialSubtitleListPreferenceDialogFragment extends MaterialPreferenceDialogFragment {
    private static final String SAVE_STATE_ENTRIES = "SubtitleListPreferenceDialogFragment.entries";
    private static final String SAVE_STATE_ENTRY_VALUES = "SubtitleListPreferenceDialogFragment.entryValues";
    private static final String SAVE_STATE_ENTRY_SUBTITLES = "SubtitleListPreferenceDialogFragment.entrySubtitles";
    private static final String SAVE_STATE_ALLOW_CUSTOM_VALUE = "SubtitleListPreferenceDialogFragment.allowCustomValue";
    private static final String SAVE_STATE_SELECTED_INDEX = "SubtitleListPreferenceDialogFragment.selectedIndex";
    private static final String SAVE_STATE_CUSTOM_VALUE = "SubtitleListPreferenceDialogFragment.customValue";

    private CharSequence[] mEntries;
    private CharSequence[] mEntryValues;
    private CharSequence[] mEntrySubtitles;
    private boolean mAllowCustomValue;
    private int mSelectedIndex;
    private String mCustomValue;

    private boolean mShowCustomValueDialogRequested;

    public static MaterialSubtitleListPreferenceDialogFragment newInstance(final String key) {
        final MaterialSubtitleListPreferenceDialogFragment fragment = new MaterialSubtitleListPreferenceDialogFragment();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            final SubtitleListPreference preference = getSubtitleListPreference();
            mEntries = preference.getEntries() != null ? preference.getEntries() : new CharSequence[0];
            mEntryValues = preference.getEntryValues() != null ? preference.getEntryValues() : new CharSequence[0];
            mEntrySubtitles = preference.getEntrySubtitles();
            mAllowCustomValue = preference.isAllowCustomValue();
            mCustomValue = preference.getValue();
            mSelectedIndex = preference.findIndexOfValue(mCustomValue);
        } else {
            mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES);
            mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES);
            mEntrySubtitles = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_SUBTITLES);
            mAllowCustomValue = savedInstanceState.getBoolean(SAVE_STATE_ALLOW_CUSTOM_VALUE);
            mSelectedIndex = savedInstanceState.getInt(SAVE_STATE_SELECTED_INDEX);
            mCustomValue = savedInstanceState.getString(SAVE_STATE_CUSTOM_VALUE);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues);
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_SUBTITLES, mEntrySubtitles);
        outState.putBoolean(SAVE_STATE_ALLOW_CUSTOM_VALUE, mAllowCustomValue);
        outState.putInt(SAVE_STATE_SELECTED_INDEX, mSelectedIndex);
        outState.putString(SAVE_STATE_CUSTOM_VALUE, mCustomValue);
    }

    private SubtitleListPreference getSubtitleListPreference() {
        return (SubtitleListPreference) getPreference();
    }

    @Override
    protected void onPrepareDialogBuilder(final MaterialAlertDialogBuilder builder) {
        super.onPrepareDialogBuilder(builder);

        final RowAdapter adapter = new RowAdapter();
        builder.setAdapter(adapter, (DialogInterface dialog, int which) -> {
            if (which < mEntries.length) {
                mSelectedIndex = which;
                MaterialSubtitleListPreferenceDialogFragment.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            } else {
                mShowCustomValueDialogRequested = true;
                MaterialSubtitleListPreferenceDialogFragment.this.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
            }
            dialog.dismiss();
        });

        // The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
        // dialog instead of the user having to press 'Ok'.
        builder.setPositiveButton(null, null);
    }

    @Override
    public void onDialogClosed(final boolean positiveResult) {
        if (positiveResult) {
            if (mSelectedIndex >= 0 && mSelectedIndex < mEntryValues.length) {
                final String value = mEntryValues[mSelectedIndex].toString();
                final SubtitleListPreference preference = getSubtitleListPreference();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                }
            }
        } else if (mShowCustomValueDialogRequested) {
            showCustomValueDialog();
        }
    }

    private void showCustomValueDialog() {
        final SubtitleListPreference preference = getSubtitleListPreference();
        final String initialValue = mSelectedIndex < 0 && mCustomValue != null ? mCustomValue : "";

        final SubtitleListCustomValueDialogFragment fragment = SubtitleListCustomValueDialogFragment.newInstance(
                preference.getKey(), initialValue, preference.getDialogTitle());
        fragment.setTargetFragment(getTargetFragment(), 0);
        fragment.show(getParentFragmentManager(), "subtitle_list_preference_custom_value");
    }

    private class RowAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mEntries.length + (mAllowCustomValue ? 1 : 0);
        }

        @Override
        public Object getItem(final int position) {
            return position < mEntries.length ? mEntries[position] : null;
        }

        @Override
        public long getItemId(final int position) {
            return position;
        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            final View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_subtitle_picker, parent, false);

            final TextView titleView = view.findViewById(R.id.subtitle_picker_item_title);
            final TextView subtitleView = view.findViewById(R.id.subtitle_picker_item_subtitle);
            final RadioButton radioButton = view.findViewById(R.id.subtitle_picker_item_radio);

            final CharSequence title;
            final CharSequence subtitle;
            final boolean checked;

            if (position < mEntries.length) {
                title = mEntries[position];
                subtitle = mEntrySubtitles != null && position < mEntrySubtitles.length ? mEntrySubtitles[position] : null;
                checked = position == mSelectedIndex;
            } else {
                title = getString(R.string.custom);
                final boolean isCustomSelected = mSelectedIndex < 0;
                subtitle = isCustomSelected && !TextUtils.isEmpty(mCustomValue) ? mCustomValue : getString(R.string.not_set);
                checked = isCustomSelected;
            }

            titleView.setText(title);
            if (TextUtils.isEmpty(subtitle)) {
                subtitleView.setVisibility(View.GONE);
            } else {
                subtitleView.setVisibility(View.VISIBLE);
                subtitleView.setText(subtitle);
            }
            radioButton.setChecked(checked);

            return view;
        }
    }
}
