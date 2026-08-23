package nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getParcelableCompat
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getSerializableCompat
import java.util.UUID

class DynamicAppConfigActivity : AbstractSettingsActivityV2() {
    private lateinit var device: GBDevice
    private lateinit var appId: String
    private lateinit var appName: String

    override fun newFragment(): PreferenceFragmentCompat {
        return DynamicAppConfigFragment.newInstance(device, appId, appName)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        device = intent.getParcelableCompat<GBDevice>(GBDevice.EXTRA_DEVICE)!!
        appId = intent.getSerializableCompat<UUID>(DeviceService.EXTRA_APP_UUID)!!.toString()
        appName = intent.getStringExtra("app_name") ?: getString(R.string.unknown)

        super.onCreate(savedInstanceState)

        if (!device.isInitialized) {
            GB.toast(getString(R.string.watch_not_connected), Toast.LENGTH_SHORT, GB.INFO)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        this.fragment?.refreshFromDevice()
    }

    private val fragment: DynamicAppConfigFragment?
        get() {
            val fragmentManager = supportFragmentManager
            val fragment =
                fragmentManager.findFragmentByTag(AbstractPreferenceFragment.FRAGMENT_TAG) ?: return null

            if (fragment is DynamicAppConfigFragment) {
                return fragment
            }

            return null
        }
}
