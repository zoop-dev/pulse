package nodomain.freeyourgadget.gadgetbridge.activities.debug

import androidx.preference.PreferenceFragmentCompat
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2

class DebugActivityV2 : AbstractSettingsActivityV2(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun newFragment(): PreferenceFragmentCompat? {
        return MainDebugFragment()
    }
}
