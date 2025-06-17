/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.util.preferences;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class CountryCodeTextWatcher implements TextWatcher {
    private final EditText editText;

    public CountryCodeTextWatcher(final EditText editText) {
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
        editText.removeTextChangedListener(this);

        final StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < editable.length(); i++) {
            final char c = Character.toUpperCase(editable.charAt(i));
            if (c >= 'A' && c <= 'Z') {
                filtered.append(c);
            }
        }

        String newText = filtered.toString();
        if (newText.length() > 2) {
            newText = newText.substring(0, 2);
        }

        if (!newText.equals(editable.toString())) {
            editText.setText(newText);
            editText.setSelection(newText.length());
        }

        editText.addTextChangedListener(this);
        editText.getRootView().findViewById(android.R.id.button1).setEnabled(editText.getText().length() == 2);
    }
}
