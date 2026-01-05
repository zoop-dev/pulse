/*  Copyright (C) 2026 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.endurain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R

class EndurainSetupBottomSheet : BottomSheetDialogFragment() {

    private val prefs get() = GBApplication.getPrefs().preferences
    private val vm: EndurainSetupViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(
        R.layout.endurain_bottomsheet_setup_wizard,
        container,
        false
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val serverLayout = view.findViewById<TextInputLayout>(R.id.server_layout)
        val serverInput = view.findViewById<TextInputEditText>(R.id.server_input)

        val loginTypeGroup =
            view.findViewById<MaterialButtonToggleGroup>(R.id.login_type_group)
        val localButton = view.findViewById<MaterialButton>(R.id.local_login_button)
        val ssoButton = view.findViewById<MaterialButton>(R.id.sso_login_button)

        val userLayout = view.findViewById<TextInputLayout>(R.id.user_layout)
        val passLayout = view.findViewById<TextInputLayout>(R.id.password_layout)
        val userInput = view.findViewById<TextInputEditText>(R.id.user_input)
        val passInput = view.findViewById<TextInputEditText>(R.id.password_input)

        val progress = view.findViewById<View>(R.id.progress)
        val next = view.findViewById<MaterialButton>(R.id.next_button)

        serverInput.setText(prefs.getString("endurain_server", ""))

        fun showProgress(show: Boolean) {
            progress.visibility = if (show) View.VISIBLE else View.GONE
            next.isEnabled = !show
        }

        next.setOnClickListener {
            when (vm.step) {
                EndurainSetupViewModel.Step.SERVER -> {
                    val uri = serverInput.text.toString().toUri()
                    if (uri.scheme == null || uri.host == null) {
                        serverLayout.error = "Invalid server URL"
                        return@setOnClickListener
                    }
                    serverLayout.error = null
                    val server = "${uri.scheme}://${uri.host}"
                    vm.server = server
                    prefs.edit { putString("endurain_server", server) }

                    showProgress(true)
                    vm.fetchServerCapabilities(server) { ok ->
                        showProgress(false)
                        if (!ok) return@fetchServerCapabilities
                        vm.step = EndurainSetupViewModel.Step.LOGIN_TYPE
                        loginTypeGroup.visibility = View.VISIBLE
                        localButton.visibility =
                            if (vm.localLoginEnabled) View.VISIBLE else View.GONE
                        ssoButton.visibility =
                            if (vm.ssoEnabled) View.VISIBLE else View.GONE
                    }
                }

                EndurainSetupViewModel.Step.LOCAL_LOGIN -> {
                    val user = userInput.text.toString()
                    val pass = passInput.text.toString()
                    if (user.isBlank()) {
                        userLayout.error = "Required"
                        return@setOnClickListener
                    }
                    if (pass.isBlank()) {
                        passLayout.error = "Required"
                        return@setOnClickListener
                    }
                    userLayout.error = null
                    passLayout.error = null

                    showProgress(true)
                    vm.performLocalLogin(vm.server, user, pass) { success ->
                        showProgress(false)
                        if (success) {
                            prefs.edit {
                                putString("endurain_user", user)
                                putString("endurain_password", pass)
                            }
                            dismiss()
                        }
                    }
                }

                else -> {}
            }
        }

        loginTypeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.local_login_button -> {
                    vm.step = EndurainSetupViewModel.Step.LOCAL_LOGIN
                    userLayout.visibility = View.VISIBLE
                    passLayout.visibility = View.VISIBLE
                }
                R.id.sso_login_button -> {
                    vm.step = EndurainSetupViewModel.Step.SSO_LOGIN
                    // TODO launch SSO flow
                    dismiss()
                }
            }
        }
    }
}
