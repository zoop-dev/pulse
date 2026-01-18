package nodomain.freeyourgadget.gadgetbridge.internet

import android.net.Uri
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.InternetFirewallRule
import nodomain.freeyourgadget.gadgetbridge.entities.InternetFirewallRuleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.LoggerFactory

class InternetFirewall(
    private val requestType: InternetRequestType,
    private val device: GBDevice
) {
    fun isAllowed(uri: Uri): Boolean {
        val prefs = GBApplication.getPrefs()

        when (requestType) {
            InternetRequestType.PEBBLE_APP_STORE -> return prefs.getBoolean(
                "pref_key_internethelper_allow_pebble_appstore",
                false
            )

            InternetRequestType.PEBBLE_APP_CONFIG -> return prefs.getBoolean(
                "pref_key_internethelper_allow_pebble_configs",
                false
            )

            InternetRequestType.PEBBLE_BACKGROUND_JS -> return prefs.getBoolean(
                "pref_key_internethelper_allow_pebble_background_js",
                false
            )

            InternetRequestType.BANGLE_APP_LOADER -> {
                if (GBApplication.hasDirectInternetAccess()) {
                    // We're in the bangle build with direct internet access
                    return true
                }
                return prefs.getBoolean(
                    "pref_key_internethelper_allow_bangle_app_loader",
                    false
                )
            }

            InternetRequestType.WATCH_APP -> {
                // Devices with watch app internet access must have devicesettings_device_internet_access
                val devicePrefs: Prefs = GBApplication.getDevicePrefs(device)
                if (!devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS, false)) {
                    LOG.warn("Device has no internet access enabled for watch apps")
                    return false
                }
                return when (getFirewallAction(uri)) {
                    FirewallAction.ALLOW -> true
                    FirewallAction.BLOCK -> false
                }
            }
        }
    }

    private fun getFirewallAction(uri: Uri): FirewallAction {
        val defaultAction = FirewallAction.BLOCK
        val rules: List<InternetFirewallRule>

        try {
            val deviceRules = GBApplication.acquireDB().use { db ->
                val deviceFromDb = DBHelper.getDevice(device, db.daoSession)

                val qb = db.getDaoSession().internetFirewallRuleDao.queryBuilder()
                return@use qb
                    .where(
                        qb.and(
                            InternetFirewallRuleDao.Properties.Domain.eq(uri.host),
                            InternetFirewallRuleDao.Properties.DeviceId.eq(deviceFromDb.id)
                        )
                    ).build().list()
            }

            val globalRules = GBApplication.acquireDB().use { db ->
                val qb = db.getDaoSession().internetFirewallRuleDao.queryBuilder()
                return@use qb
                    .where(
                        qb.and(
                            InternetFirewallRuleDao.Properties.Domain.eq(uri.host),
                            InternetFirewallRuleDao.Properties.DeviceId.isNull
                        )
                    ).build().list()
            }

            LOG.debug("Got rules: {} device-specific / {} global", deviceRules.size, globalRules.size)

            // Prioritize device-specific rules
            rules = deviceRules + globalRules
        } catch (e: Exception) {
            LOG.error("Error reading firewall rules from db, default action is {}", defaultAction, e)
            return defaultAction
        }

        if (rules.isEmpty()) {
            LOG.debug("No rules matched for {}, default action is {}", uri, defaultAction)
            return defaultAction
        }

        val action = FirewallAction.valueOf(rules[0].action)

        LOG.debug("Firewall action: {} {}", action, uri)

        return action
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(InternetFirewall::class.java)
    }
}
