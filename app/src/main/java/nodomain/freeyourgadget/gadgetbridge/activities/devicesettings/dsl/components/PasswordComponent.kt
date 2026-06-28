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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.components

import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.DeviceSettingsScope

enum class PasswordMode {
    NUMBERS_6,
    NUMBERS_4_DIGITS_0_TO_9,
    NUMBERS_4_DIGITS_1_TO_4,
    VISIBLE_NUMBERS_4_DIGITS_0_TO_9,
}

/**
 * Adds a password sub-screen with an enable toggle and a password text field, fully configured
 * for the given [PasswordMode] - including input type, length enforcement, cursor positioning,
 * and OK-button gating via a text watcher.
 */
fun DeviceSettingsScope.passwordScreen(mode: PasswordMode) {
    val inputType = when (mode) {
        PasswordMode.NUMBERS_6,
        PasswordMode.NUMBERS_4_DIGITS_0_TO_9,
        PasswordMode.NUMBERS_4_DIGITS_1_TO_4 ->
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD

        PasswordMode.VISIBLE_NUMBERS_4_DIGITS_0_TO_9 ->
            InputType.TYPE_CLASS_NUMBER
    }

    val maxLength = when (mode) {
        PasswordMode.NUMBERS_6 -> 6
        PasswordMode.NUMBERS_4_DIGITS_0_TO_9,
        PasswordMode.NUMBERS_4_DIGITS_1_TO_4,
        PasswordMode.VISIBLE_NUMBERS_4_DIGITS_0_TO_9 -> 4
    }

    val onBindEditText: (EditText) -> Unit = { editText ->
        // Position the cursor at the end of any existing text.
        editText.setSelection(editText.text.length)

        // For the 1-to-4 digit mode, append an additional character filter.
        if (mode == PasswordMode.NUMBERS_4_DIGITS_1_TO_4) {
            val digitFilter = InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    val c = source[i]
                    if (!c.isDigit() || c < '1' || c > '4') return@InputFilter ""
                }
                null
            }
            editText.filters += digitFilter
        }

        // Gate the dialog OK button: only enabled when the text is exactly maxLength characters.
        val okButton: View? = editText.rootView.findViewById(android.R.id.button1)
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(editable: Editable) {
                okButton?.isEnabled = editable.length == maxLength
            }
        })
    }

    screen(
        key = DeviceSettingsPreferenceConst.PREF_SCREEN_PASSWORD,
        title = R.string.prefs_password,
        icon = R.drawable.ic_password,
        connectedOnly = false,
    ) {
        switchSetting(
            key = DeviceSettingsPreferenceConst.PREF_PASSWORD_ENABLED,
            title = R.string.prefs_password_enabled,
            icon = R.drawable.ic_password,
            connectedOnly = true,
        )
        text(
            key = DeviceSettingsPreferenceConst.PREF_PASSWORD,
            title = R.string.prefs_password,
            icon = R.drawable.ic_password,
            dependency = DeviceSettingsPreferenceConst.PREF_PASSWORD_ENABLED,
            inputType = inputType,
            maxLength = maxLength,
            connectedOnly = true,
            onBindEditText = onBindEditText,
        )
    }
}
