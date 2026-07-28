package nodomain.freeyourgadget.gadgetbridge.devices.garmin

import android.content.Intent
import android.os.Bundle
import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityGarminAuthExpiredBinding
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminPrefs
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getDevice


class GarminAuthExpiredActivity : AbstractGBActivity() {
    private var devicePrefs: GarminPrefs? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityGarminAuthExpiredBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val device = intent.getDevice()
        if (device == null) {
            finish()
            return
        }

        devicePrefs = GarminPrefs(GBApplication.getDeviceSpecificSharedPrefs(device.address), device)

        binding.oauthExpiredDescription.text =
            getString(R.string.garmin_auth_expired_activity_description, device.aliasOrName)

        binding.buttonGotoAuthenticationSettings.setOnClickListener { _ ->
            val startIntent = Intent(this@GarminAuthExpiredActivity, DeviceSettingsActivity::class.java)
            startIntent.putExtra(GBDevice.EXTRA_DEVICE, device)
            startIntent.putExtra(
                DeviceSettingsActivity.MENU_ENTRY_POINT,
                DeviceSettingsActivity.MENU_ENTRY_POINTS.AUTH_SETTINGS
            )
            startActivity(startIntent)
            finish()
        }
        binding.buttonDontShowAgain.setOnClickListener { _ ->
            devicePrefs!!.preferences.edit {
                putBoolean(GarminPrefs.PREF_AUTH_EXPIRED_NOTIFICATION_ENABLED, false)
            }
            finish()
        }
    }
}
