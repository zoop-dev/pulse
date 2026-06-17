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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
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
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.LoggerFactory

class EndurainSetupBottomSheet : BottomSheetDialogFragment() {

    private val LOG = LoggerFactory.getLogger(EndurainSetupBottomSheet::class.java)
    private val prefs get() = GBApplication.getPrefs().preferences
    private val vm: EndurainSetupViewModel by viewModels()

    private lateinit var serverLayout: TextInputLayout
    private lateinit var serverInput: TextInputEditText
    private lateinit var loginTypeGroup: MaterialButtonToggleGroup
    private lateinit var localButton: MaterialButton
    private lateinit var ssoButton: MaterialButton
    private lateinit var userLayout: TextInputLayout
    private lateinit var passLayout: TextInputLayout
    private lateinit var userInput: TextInputEditText
    private lateinit var passInput: TextInputEditText
    private lateinit var mfaLayout: TextInputLayout
    private lateinit var mfaInput: TextInputEditText
    private lateinit var progress: View
    private lateinit var next: MaterialButton

    // Broadcast receiver for SSO callback
    private val ssoCallbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val sessionId = intent?.getStringExtra("session_id")
            val success = intent?.getBooleanExtra("success", true) ?: true

            if (sessionId != null && success) {
                handleSsoCallback(sessionId)
            } else {
                activity?.runOnUiThread {
                    showProgress(false)
                    Toast.makeText(requireContext(),
                        getString(R.string.endurain_sso_login_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

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
        // Initialize views
        serverLayout = view.findViewById(R.id.server_layout)
        serverInput = view.findViewById(R.id.server_input)
        loginTypeGroup = view.findViewById(R.id.login_type_group)
        localButton = view.findViewById(R.id.local_login_button)
        ssoButton = view.findViewById(R.id.sso_login_button)
        userLayout = view.findViewById(R.id.user_layout)
        passLayout = view.findViewById(R.id.password_layout)
        userInput = view.findViewById(R.id.user_input)
        passInput = view.findViewById(R.id.password_input)
        mfaLayout = view.findViewById(R.id.mfa_layout)
        mfaInput = view.findViewById(R.id.mfa_input)
        progress = view.findViewById(R.id.progress)
        next = view.findViewById(R.id.next_button)

        serverInput.setText(prefs.getString("endurain_server", ""))

        // Register broadcast receiver for SSO callbacks
        ContextCompat.registerReceiver(
            requireContext(),
            ssoCallbackReceiver,
            IntentFilter("nodomain.freeyourgadget.gadgetbridge.ENDURAIN_SSO_CALLBACK"),
            ContextCompat.RECEIVER_EXPORTED
        )

        next.setOnClickListener {
            when (vm.step) {
                EndurainSetupViewModel.Step.SERVER -> handleServerStep()
                EndurainSetupViewModel.Step.LOCAL_LOGIN -> handleLocalLoginStep()
                EndurainSetupViewModel.Step.MFA_VERIFY -> handleMfaStep()
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
                    mfaLayout.visibility = View.GONE
                }
                R.id.sso_login_button -> {
                    vm.step = EndurainSetupViewModel.Step.SSO_PROVIDERS
                    startSsoFlow()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(ssoCallbackReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }

    private fun startSsoFlow() {
        showProgress(true)
        vm.fetchSsoProviders { success ->
            activity?.runOnUiThread {
                showProgress(false)
                if (success && vm.availableProviders.size == 1) {
                    launchSsoLogin(vm.availableProviders[0])
                } else if (success && vm.availableProviders.isNotEmpty()) {
                    showProviderSelection()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.endurain_no_sso_providers_available),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showProviderSelection() {
        EndurainSsoProviderDialog(requireContext(), vm.availableProviders) { provider ->
            launchSsoLogin(provider)
        }.show()
    }

    private fun launchSsoLogin(provider: EndurainIdentityProvider) {
        val ssoUrl = vm.generateSsoUrl(provider.slug)

        LOG.info("Launching secure browser for SSO URL: $ssoUrl")

        // Launch Custom Tab (secure browser)
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()

        showProgress(true)

        try {
            customTabsIntent.launchUrl(requireContext(), ssoUrl.toUri())
        } catch (e: Exception) {
            showProgress(false)
            Toast.makeText(
                requireContext(),
                getString(R.string.endurain_failed_to_open_browser, e.message),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleSsoCallback(sessionId: String) {
        showProgress(true)
        vm.exchangeSsoSession(sessionId) { success ->
            activity?.runOnUiThread {
                showProgress(false)
                if (success) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.endurain_sso_login_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.setFragmentResult(
                        "endurain_login_result",
                        Bundle().apply { putBoolean("success", true) }
                    )
                    dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.endurain_sso_login_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun handleServerStep() {
        val uri = serverInput.text.toString().toUri()
        if (uri.scheme == null || uri.host == null) {
            serverLayout.error = getString(R.string.endurain_invalid_server_url)
            return
        }
        serverLayout.error = null
        val server = "${uri.scheme}://${uri.host}${if (uri.port > 0) ":${uri.port}" else ""}"
        vm.server = server
        prefs.edit { putString("endurain_server", server) }

        showProgress(true)
        vm.fetchServerCapabilities(server) { ok ->
            activity?.runOnUiThread {
                showProgress(false)
                if (!ok) {
                    GB.toast(getString(R.string.endurain_failed_to_connect_to_server), Toast.LENGTH_SHORT, GB.INFO)
                    return@runOnUiThread
                }
                vm.step = EndurainSetupViewModel.Step.LOGIN_TYPE
                loginTypeGroup.visibility = View.VISIBLE
                localButton.visibility = if (vm.localLoginEnabled) View.VISIBLE else View.GONE
                ssoButton.visibility = if (vm.ssoEnabled) View.VISIBLE else View.GONE

                // Auto-select if only one option available
                if (vm.localLoginEnabled && !vm.ssoEnabled) {
                    loginTypeGroup.check(R.id.local_login_button)
                } else if (vm.ssoEnabled && !vm.localLoginEnabled) {
                    loginTypeGroup.check(R.id.sso_login_button)
                }
            }
        }
    }

    private fun handleLocalLoginStep() {
        val user = userInput.text.toString()
        val pass = passInput.text.toString()

        var hasError = false
        if (user.isBlank()) {
            userLayout.error = getString(R.string.required)
            hasError = true
        } else {
            userLayout.error = null
        }

        if (pass.isBlank()) {
            passLayout.error = getString(R.string.required)
            hasError = true
        } else {
            passLayout.error = null
        }

        if (hasError) return

        showProgress(true)
        vm.performLocalLogin(vm.server, user, pass) { success ->
            activity?.runOnUiThread {
                showProgress(false)
                when {
                    vm.step == EndurainSetupViewModel.Step.MFA_VERIFY -> {
                        // MFA required - show MFA input
                        userLayout.visibility = View.GONE
                        passLayout.visibility = View.GONE
                        mfaLayout.visibility = View.VISIBLE
                        next.text = getString(R.string.endurain_verify_mfa)
                        GB.toast(getString(R.string.endurain_enter_your_mfa_code), Toast.LENGTH_SHORT, GB.INFO)
                    }
                    success -> {
                        GB.toast(getString(R.string.endurain_login_successful), Toast.LENGTH_SHORT, GB.INFO)
                        // Send result to parent fragment
                        parentFragmentManager.setFragmentResult(
                            "endurain_login_result",
                            Bundle().apply { putBoolean("success", true) }
                        )
                        dismiss()
                    }
                    else -> {
                        GB.toast(getString(R.string.endurain_login_failed), Toast.LENGTH_SHORT, GB.INFO)
                    }
                }
            }
        }
    }

    private fun handleMfaStep() {
        val mfaCode = mfaInput.text.toString()
        if (mfaCode.isBlank()) {
            mfaLayout.error = getString(R.string.required)
            return
        }
        mfaLayout.error = null

        showProgress(true)
        vm.verifyMfa(mfaCode) { success ->
            activity?.runOnUiThread {
                showProgress(false)
                if (success) {
                    GB.toast(getString(R.string.endurain_mfa_verification_successful), Toast.LENGTH_SHORT, GB.INFO)
                    // Send result to parent fragment
                    parentFragmentManager.setFragmentResult(
                        "endurain_login_result",
                        Bundle().apply { putBoolean("success", true) }
                    )
                    dismiss()
                } else {
                    mfaLayout.error = getString(R.string.endurain_invalid_mfa_code)
                    GB.toast(getString(R.string.endurain_mfa_verification_failed), Toast.LENGTH_SHORT, GB.INFO)
                }
            }
        }
    }

    private fun showProgress(show: Boolean) {
        progress.visibility = if (show) View.VISIBLE else View.GONE
        next.isEnabled = !show
    }
}