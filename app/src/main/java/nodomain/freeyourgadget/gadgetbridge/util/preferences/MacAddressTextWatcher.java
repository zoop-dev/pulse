package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import org.apache.commons.lang3.StringUtils;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;

@SuppressWarnings("ClassCanBeRecord")
public class MacAddressTextWatcher implements TextWatcher {
    private final EditText editText;

    public MacAddressTextWatcher(final EditText editText) {
        this.editText = editText;
    }

    @Override
    public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
    }

    @Override
    public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {
    }

    @Override
    public void afterTextChanged(final Editable editable) {
        editText.getRootView().findViewById(android.R.id.button1)
                .setEnabled(validMacAddress(editable.toString()));
    }

    private boolean validMacAddress(final String mac) {
        if (StringUtils.isBlank(mac)) {
            return false;
        }

        if (BuildConfig.INTERNET_ACCESS) {
            // For builds with internet access (Bangle.js), allow more flexible formats
            return true;
        }

        return mac.matches("^([0-9A-F]{2}:){5}[0-9A-F]{2}$");
    }
}
