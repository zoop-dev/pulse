package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceViewHolder;

import org.apache.commons.lang3.StringUtils;

import nodomain.freeyourgadget.gadgetbridge.R;

/**
 * A {@link ListPreference} which shows a subtitle below each entry's title, both in the dialog
 * and in the preference's own summary, so that a technical value (a package name, a URL, ...) can
 * be shown alongside a friendly title. Optionally, it can also offer an extra "Custom…" entry
 * that lets the user type a free-text value that is not in the list.
 */
public class SubtitleListPreference extends ListPreference {
    private CharSequence[] entrySubtitles;
    private boolean allowCustomValue;
    private CharSequence unavailableSummary;

    public SubtitleListPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubtitleListPreference);
        try {
            final int entrySubtitlesResId = a.getResourceId(R.styleable.SubtitleListPreference_entrySubtitles, 0);
            if (entrySubtitlesResId != 0) {
                entrySubtitles = a.getResources().getTextArray(entrySubtitlesResId);
            }
            allowCustomValue = a.getBoolean(R.styleable.SubtitleListPreference_allowCustomValue, false);
        } finally {
            a.recycle();
        }

        // A dialogMessage would suppress the list view entirely, since the picker dialog is built
        // from a plain adapter rather than the dialog layout resource. Never allow one to be set.
        setDialogMessage(null);

        setSummaryProvider(preference -> {
            final SubtitleListPreference pref = (SubtitleListPreference) preference;

            if (pref.unavailableSummary != null) {
                return pref.unavailableSummary;
            }

            final String value = pref.getValue();

            // Check for a matching entry before falling back to the not-set/custom cases below,
            // since an entry's value is allowed to be blank (e.g. an empty string standing in for
            // "use the default", to keep the persisted value backward compatible).
            final int index = pref.findIndexOfValue(value);
            if (index >= 0) {
                final CharSequence title = pref.getEntries()[index];
                final CharSequence subtitle = pref.getSubtitleFor(value);
                if (StringUtils.isNotBlank(subtitle)) {
                    return title + "\n" + subtitle;
                }
                return title;
            }

            if (StringUtils.isBlank(value)) {
                return pref.getContext().getString(R.string.not_set);
            }

            if (pref.isAllowCustomValue()) {
                return pref.getContext().getString(R.string.custom) + "\n" + value;
            }

            return value;
        });
    }

    /**
     * Marks this preference as unavailable (e.g. because no supported app is installed), disabling
     * it and showing the given summary instead of the usual value-derived one.
     */
    public void setUnavailable(@Nullable final CharSequence summary) {
        this.unavailableSummary = summary;
        setEnabled(summary == null);
    }

    @Nullable
    public CharSequence[] getEntrySubtitles() {
        return entrySubtitles;
    }

    public void setEntrySubtitles(@Nullable final CharSequence[] entrySubtitles) {
        this.entrySubtitles = entrySubtitles;
    }

    /**
     * Returns the subtitle for a given entry value, or {@code null} if there is none, or the
     * value is not one of the known entries.
     */
    @Nullable
    public CharSequence getSubtitleFor(final String value) {
        if (entrySubtitles == null) {
            return null;
        }
        final int index = findIndexOfValue(value);
        if (index < 0 || index >= entrySubtitles.length) {
            return null;
        }
        return entrySubtitles[index];
    }

    public boolean isAllowCustomValue() {
        return allowCustomValue;
    }

    public void setAllowCustomValue(final boolean allowCustomValue) {
        this.allowCustomValue = allowCustomValue;
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final TextView summary = (TextView) holder.findViewById(android.R.id.summary);
        if (summary != null) {
            summary.setSingleLine(false);
            summary.setMaxLines(2);
        }
    }
}
