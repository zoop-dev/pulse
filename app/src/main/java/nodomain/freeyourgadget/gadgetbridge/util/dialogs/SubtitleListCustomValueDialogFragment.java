package nodomain.freeyourgadget.gadgetbridge.util.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.preference.DialogPreference;
import androidx.preference.Preference;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.SubtitleListPreference;

/**
 * A small text entry dialog used by {@link MaterialSubtitleListPreferenceDialogFragment}
 * to let the user type a value that is not one of the preference's known entries. It is a plain
 * {@link DialogFragment} (rather than a {@link MaterialPreferenceDialogFragment}) since it is not
 * itself bound to a {@link DialogPreference}'s title/buttons, and is shown on top of/after the
 * list dialog has already been dismissed.
 */
public class SubtitleListCustomValueDialogFragment extends DialogFragment {
    private static final String ARG_KEY = "key";
    private static final String ARG_INITIAL_VALUE = "initialValue";
    private static final String ARG_TITLE = "title";

    private static final String SAVE_STATE_TEXT = "SubtitleListCustomValueDialogFragment.text";

    private TextInputEditText editText;
    private CharSequence text;

    public static SubtitleListCustomValueDialogFragment newInstance(final String key,
                                                                    final String initialValue,
                                                                    final CharSequence title) {
        final SubtitleListCustomValueDialogFragment fragment = new SubtitleListCustomValueDialogFragment();
        final Bundle args = new Bundle();
        args.putString(ARG_KEY, key);
        args.putString(ARG_INITIAL_VALUE, initialValue);
        args.putCharSequence(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            text = requireArguments().getString(ARG_INITIAL_VALUE, "");
        } else {
            text = savedInstanceState.getCharSequence(SAVE_STATE_TEXT);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Context context = requireContext();

        final View view = getLayoutInflater().inflate(R.layout.dialog_subtitle_list_custom_value, null);
        final TextInputLayout inputLayout = view.findViewById(R.id.subtitle_list_custom_value_layout);
        editText = view.findViewById(R.id.subtitle_list_custom_value_text);
        editText.setText(text);
        editText.setSelection(editText.getText() != null ? editText.getText().length() : 0);

        final AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(requireArguments().getCharSequence(ARG_TITLE))
                .setView(view)
                .setPositiveButton(R.string.ok, (d, which) -> persistValue())
                .setNegativeButton(R.string.cancel, null)
                .create();

        final Window window = dialog.getWindow();
        if (window != null) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }

        // The value cannot be empty - keep the positive button disabled until something is typed.
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                updateValidity(dialog, inputLayout, editable.toString());
            }
        });
        // The dialog's buttons don't exist yet at create() time - getButton() would return null
        // until the dialog is actually shown, so the initial state has to be set from here instead.
        dialog.setOnShowListener(d -> updateValidity(dialog, inputLayout, editText.getText() != null ? editText.getText().toString() : ""));

        return dialog;
    }

    private void updateValidity(final AlertDialog dialog, final TextInputLayout inputLayout, final String currentText) {
        final boolean isEmpty = currentText.trim().isEmpty();
        final Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) {
            positiveButton.setEnabled(!isEmpty);
        }
        inputLayout.setError(isEmpty ? getString(R.string.subtitle_list_custom_value_required) : null);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        if (editText != null) {
            outState.putCharSequence(SAVE_STATE_TEXT, editText.getText());
        }
    }

    private void persistValue() {
        final Fragment rawFragment = getTargetFragment();
        if (!(rawFragment instanceof DialogPreference.TargetFragment)) {
            return;
        }
        final DialogPreference.TargetFragment targetFragment = (DialogPreference.TargetFragment) rawFragment;
        final Preference preference = targetFragment.findPreference(requireArguments().getString(ARG_KEY));
        if (!(preference instanceof SubtitleListPreference)) {
            return;
        }

        final String value = editText.getText() != null ? editText.getText().toString().trim() : "";
        if (preference.callChangeListener(value)) {
            ((SubtitleListPreference) preference).setValue(value);
        }
    }
}
