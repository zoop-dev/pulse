package nodomain.freeyourgadget.gadgetbridge.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityAuthKeyBinding
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper
import nodomain.freeyourgadget.gadgetbridge.util.GB

class AuthKeyActivity : AbstractGBActivity() {
    private lateinit var binding: ActivityAuthKeyBinding
    private var deviceCandidate: GBDeviceCandidate? = null
    private var coordinator: DeviceCoordinator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthKeyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        @Suppress("DEPRECATION")
        deviceCandidate = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_DEVICE_CANDIDATE, GBDeviceCandidate::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_DEVICE_CANDIDATE)
        }

        deviceCandidate?.let { candidate ->
            coordinator = DeviceHelper.getInstance().resolveDeviceType(candidate).deviceCoordinator
        }

        if (deviceCandidate == null || coordinator == null) {
            Toast.makeText(this, "Device info missing", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        title = getString(
            R.string.auth_key_activity_title_for,
            deviceCandidate?.name ?: getString(R.string.devicetype_unknown)
        )

        deviceCandidate?.macAddress?.let { macAddress ->
            val sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(macAddress)
            val authKey = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_AUTH_KEY, "")
            if (authKey?.isNotEmpty() == true) {
                binding.authKeyEditText.setText(authKey)
            }
        }

        binding.submitAuthKeyButton.setOnClickListener {
            val authKey = binding.authKeyEditText.text.toString().trim()

            if (authKey.isEmpty()) {
                binding.authKeyInputLayout.error = getString(R.string.auth_key_required_message)
                return@setOnClickListener
            } else {
                // Clear error
                binding.authKeyInputLayout.error = null
            }

            if (coordinator?.validateAuthKey(authKey) == true) {
                // Save the auth key
                deviceCandidate?.macAddress?.let { macAddress ->
                    val sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(macAddress)
                    sharedPrefs.edit { putString(DeviceSettingsPreferenceConst.PREF_AUTH_KEY, authKey) }
                }

                // Return the auth key and candidate to the calling activity
                val resultIntent = Intent().apply {
                    putExtra(EXTRA_AUTH_KEY_RESULT, authKey)
                    putExtra(EXTRA_DEVICE_CANDIDATE_RESULT, deviceCandidate)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                binding.authKeyInputLayout.error = getString(R.string.invalid_auth_key_message)
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_auth_key, menu)
        val helpItem = menu.findItem(R.id.auth_key_help)
        helpItem?.isVisible = coordinator?.authHelp != null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.auth_key_help -> {
                coordinator?.authHelp?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, coordinator?.authHelp!!.toUri())
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        GB.toast(this, getString(R.string.cannot_open_help_url), Toast.LENGTH_SHORT, GB.ERROR, e)
                    }
                } ?: GB.toast(this, "No help URL available.", Toast.LENGTH_SHORT, GB.ERROR)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val EXTRA_DEVICE_CANDIDATE = "EXTRA_DEVICE_CANDIDATE_FOR_AUTH"
        const val EXTRA_AUTH_KEY_RESULT = "EXTRA_AUTH_KEY_RESULT"
        const val EXTRA_DEVICE_CANDIDATE_RESULT = "EXTRA_DEVICE_CANDIDATE_RESULT"

        fun newIntent(context: Context, deviceCandidate: GBDeviceCandidate): Intent {
            return Intent(context, AuthKeyActivity::class.java).apply {
                putExtra(EXTRA_DEVICE_CANDIDATE, deviceCandidate)
            }
        }
    }
}
