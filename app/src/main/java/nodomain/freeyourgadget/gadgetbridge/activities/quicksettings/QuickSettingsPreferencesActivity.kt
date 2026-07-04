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
package nodomain.freeyourgadget.gadgetbridge.activities.quicksettings

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractSettingsActivityV2
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.ListEntry
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettingDescriptor
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettingType
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettings
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.Prefs

/**
 * Settings screen for Quick Settings tiles and Device Controls.
 *
 * **Root screen** — lists all [NUM_TILES] tile slots with their current summary. Tapping a slot
 * navigates into a per-tile sub-screen.
 *
 * **Per-tile sub-screen** (key `"qs_tile_N"`) — three native preferences:
 * 1. **Device** ([ListPreference]) — choose from paired devices that expose DSL settings.
 * 2. **Setting** ([ListPreference]) — choose a toggle or list setting from the selected device.
 * 3. **Cycle through values** ([MultiSelectListPreference]) — restrict which values a list setting
 *    cycles through; hidden for toggle settings.
 *
 * All three preferences are non-persistent; changes are committed to [DeviceTilePrefs] manually
 * via change listeners.
 */
@RequiresApi(Build.VERSION_CODES.N)
class QuickSettingsPreferencesActivity : AbstractSettingsActivityV2() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // When launched via a long-press on a specific tile (ACTION_QS_TILE_PREFERENCES),
        // the system includes EXTRA_COMPONENT_NAME with the tile service's ComponentName (API 26+).
        // Inject EXTRA_PREF_SCREEN before super.onCreate() so AbstractSettingsActivityV2
        // routes straight to that tile's sub-screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            intent?.action == TileService.ACTION_QS_TILE_PREFERENCES
        ) {
            val component: ComponentName? =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.extras?.getParcelable(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.extras?.getParcelable(Intent.EXTRA_COMPONENT_NAME)
                }
            val tileIdx = component?.className
                ?.removePrefix("$packageName.activities.quicksettings.DeviceTileService")
                ?.toIntOrNull()
            if (tileIdx != null) {
                intent.putExtra(EXTRA_PREF_SCREEN, "qs_tile_$tileIdx")
            }
        }
        super.onCreate(savedInstanceState)
    }

    override fun newFragment(): PreferenceFragmentCompat = QuickSettingsFragment()

    companion object {
        const val NUM_TILES = QuickSettingsTilesActivity.NUM_TILES
        private const val TILE_KEY_PREFIX = "qs_tile_"

        // Non-persistent preference keys used within the per-tile sub-screen.
        private const val KEY_DEVICE = "qs_tile_tmp_device"
        private const val KEY_SETTING = "qs_tile_tmp_setting"
        private const val KEY_CYCLE = "qs_tile_tmp_cycle"
        private const val KEY_LOCKSCREEN = "qs_tile_tmp_lockscreen"
        private const val KEY_ADD_TO_QS = "qs_tile_tmp_add_to_qs"

        // Matches the qs_tile_label_N strings declared in the manifest for each tile slot.
        private val TILE_LABEL_RES = intArrayOf(
            R.string.qs_tile_label_1,
            R.string.qs_tile_label_2,
            R.string.qs_tile_label_3,
            R.string.qs_tile_label_4,
            R.string.qs_tile_label_5,
            R.string.qs_tile_label_6,
            R.string.qs_tile_label_7,
            R.string.qs_tile_label_8,
            R.string.qs_tile_label_9,
            R.string.qs_tile_label_10,
        )

        class QuickSettingsFragment : AbstractPreferenceFragment() {
            override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
                preferenceScreen = preferenceManager.createPreferenceScreen(requireContext())
                val tileIdx = rootKey?.removePrefix(TILE_KEY_PREFIX)?.toIntOrNull()
                if (tileIdx != null) {
                    buildTileScreen(tileIdx)
                } else {
                    buildRootScreen()
                }
            }

            override fun onResume() {
                super.onResume()

                // Refresh the root screen summaries after returning from a tile sub-screen.
                if (arguments?.getString(ARG_PREFERENCE_ROOT) == null) {
                    preferenceScreen.removeAll()
                    buildRootScreen()
                }
            }

            private fun buildRootScreen() {
                val ctx = requireContext()
                preferenceScreen.title = getString(R.string.pref_header_quick_settings)

                // Quick Settings Tiles category
                val tilesCategory = PreferenceCategory(ctx).apply {
                    setTitle(R.string.qs_tile_activity_title)
                    isIconSpaceReserved = false
                }
                preferenceScreen.addPreference(tilesCategory)

                for (i in 0 until NUM_TILES) {
                    val assignment = DeviceTilePrefs.load(i)
                    val device = assignment?.let { (address, _) ->
                        GBApplication.app().deviceManager.getDeviceByAddress(address)
                    }
                    val descriptor = assignment?.let { (address, key) ->
                        device?.let { QuickSettings.find(address, key) }
                    }

                    val tileScreen = preferenceManager.createPreferenceScreen(ctx).apply {
                        key = "$TILE_KEY_PREFIX$i"
                        title = getString(R.string.qs_tile_slot_number, i + 1)
                        summary = if (device != null && descriptor != null) {
                            "${device.aliasOrName} · ${getString(descriptor.title)}"
                        } else {
                            getString(R.string.qs_tile_not_assigned)
                        }
                        isIconSpaceReserved = false
                    }
                    // PreferenceScreen.onClick() is a no-op for empty screens, so drive
                    // navigation explicitly via the activity callback.
                    tileScreen.setOnPreferenceClickListener {
                        val act = requireActivity()
                        if (act is OnPreferenceStartScreenCallback) {
                            act.onPreferenceStartScreen(this@QuickSettingsFragment, tileScreen)
                        }
                        true
                    }
                    tilesCategory.addPreference(tileScreen)
                }
            }

            private fun buildTileScreen(tileIdx: Int) {
                val ctx = requireContext()
                preferenceScreen.title = getString(R.string.qs_tile_slot_number, tileIdx + 1)
                val assignment = DeviceTilePrefs.load(tileIdx)
                val currentAddress = assignment?.first ?: ""
                val currentKey = assignment?.second ?: ""

                // Resolve the device and descriptor from the stored assignment.
                val currentDevice = GBApplication.app().deviceManager.getDeviceByAddress(currentAddress)
                val currentDescriptor = currentDevice?.let {
                    QuickSettings.find(currentAddress, currentKey)
                }
                val currentCycleValues = DeviceTilePrefs.loadCycleValues(tileIdx)

                //
                // Add to Quick Settings (API 33+)
                //
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val tileLabel = getString(
                        TILE_LABEL_RES.getOrElse(tileIdx) { R.string.qs_tile_slot_number },
                    )
                    val addToQsPref = Preference(ctx).apply {
                        key = KEY_ADD_TO_QS
                        setIcon(R.drawable.ic_add)
                        setTitle(R.string.qs_tile_add_to_quick_settings)
                        isPersistent = false
                    }
                    preferenceScreen.addPreference(addToQsPref)
                    addToQsPref.setOnPreferenceClickListener {
                        val component = ComponentName(
                            ctx.packageName,
                            "${ctx.packageName}.activities.quicksettings.DeviceTileService$tileIdx",
                        )
                        val icon = Icon.createWithResource(ctx, R.drawable.ic_watch_bw)
                        ctx.getSystemService(StatusBarManager::class.java)
                            .requestAddTileService(component, tileLabel, icon, ctx.mainExecutor) { result ->
                                if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                                    Toast.makeText(ctx, R.string.qs_tile_already_in_quick_settings, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        true
                    }
                }

                // All devices that expose at least one DSL setting
                val allDevices = GBApplication.app().deviceManager.devices
                    .filter { QuickSettings.listFor(it).isNotEmpty() }

                //
                // Device
                //
                val devicePref = ListPreference(ctx).apply {
                    key = KEY_DEVICE
                    setTitle(R.string.device)
                    setDialogTitle(R.string.device)
                    setIcon(R.drawable.ic_devices_other)
                    isPersistent = false

                    val noneLabel = getString(R.string.qs_tile_no_device_selected)
                    entries = (listOf(noneLabel) + allDevices.map { it.aliasOrName }).toTypedArray()
                    entryValues = (listOf("") + allDevices.map { it.address }).toTypedArray()
                    value = currentAddress
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
                preferenceScreen.addPreference(devicePref)

                //
                // Setting
                //
                val settingPref = ListPreference(ctx).apply {
                    key = KEY_SETTING
                    setTitle(R.string.qs_tile_setting)
                    setDialogTitle(R.string.qs_tile_setting)
                    setIcon(R.drawable.ic_settings_applications)
                    isPersistent = false
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
                preferenceScreen.addPreference(settingPref)

                //
                // Cycle values
                //
                val cyclePref = MultiSelectListPreference(ctx).apply {
                    key = KEY_CYCLE
                    setTitle(R.string.qs_tile_cycle_values)
                    setDialogTitle(R.string.qs_tile_cycle_values)
                    setIcon(R.drawable.ic_cycle)
                    isPersistent = false
                    summaryProvider = Preference.SummaryProvider<MultiSelectListPreference> { pref ->
                        val selected = pref.values ?: emptySet()
                        val allValues = pref.entryValues
                        if (selected.isEmpty() || allValues == null || selected.size == allValues.size) {
                            pref.context.getString(R.string.qs_tile_cycle_values_all)
                        } else {
                            val entries = pref.entries ?: return@SummaryProvider ""
                            selected.mapNotNull { v ->
                                val i = allValues.indexOf(v)
                                if (i >= 0) entries[i] else null
                            }.joinToString(", ")
                        }
                    }
                }
                preferenceScreen.addPreference(cyclePref)

                //
                // Lock screen
                //
                val lockscreenPref = SwitchPreferenceCompat(ctx).apply {
                    key = KEY_LOCKSCREEN
                    setTitle(R.string.qs_tile_allow_lockscreen)
                    setSummary(R.string.qs_tile_allow_lockscreen_summary)
                    setIcon(R.drawable.ic_lock_open)
                    isPersistent = false
                    isChecked = DeviceTilePrefs.loadLockScreen(tileIdx)
                }
                preferenceScreen.addPreference(lockscreenPref)

                lockscreenPref.setOnPreferenceChangeListener { _, newVal ->
                    DeviceTilePrefs.saveLockScreen(tileIdx, newVal as Boolean)
                    true
                }

                // Populate setting and cycle prefs from the current device (if any)
                populateSettingPref(settingPref, cyclePref, currentDevice, currentKey)
                populateCyclePref(cyclePref, currentDevice, currentDescriptor, currentCycleValues)

                //
                // Change listeners
                //

                devicePref.setOnPreferenceChangeListener { _, newVal ->
                    val address = newVal as String
                    val device = GBApplication.app().deviceManager.getDeviceByAddress(address)

                    // Reset setting and cycle when device changes
                    populateSettingPref(settingPref, cyclePref, device, "")
                    cyclePref.isVisible = false
                    cyclePref.values = emptySet()

                    if (address.isEmpty()) {
                        DeviceTilePrefs.clear(tileIdx)
                    } else {
                        // Setting must also be chosen before we save - clear it for now
                        DeviceTilePrefs.clear(tileIdx)
                    }
                    notifyTile(tileIdx)
                    true
                }

                settingPref.setOnPreferenceChangeListener { _, newVal ->
                    val key = newVal as String
                    val address = devicePref.value ?: ""
                    val device = GBApplication.app().deviceManager.getDeviceByAddress(address)
                    val descriptor = device?.let { QuickSettings.find(address, key) }

                    // Update cycle pref for the new setting
                    populateCyclePref(cyclePref, device, descriptor, emptyList())

                    if (address.isNotEmpty() && key.isNotEmpty()) {
                        DeviceTilePrefs.save(tileIdx, address, key)
                        DeviceTilePrefs.saveCycleValues(tileIdx, emptyList())
                    }

                    notifyTile(tileIdx)
                    true
                }

                cyclePref.setOnPreferenceChangeListener { _, newVal ->
                    @Suppress("UNCHECKED_CAST")
                    val selected = newVal as Set<String>
                    val address = devicePref.value ?: ""
                    val key = settingPref.value ?: ""
                    val device = GBApplication.app().deviceManager.getDeviceByAddress(address)
                    val descriptor = device?.let { QuickSettings.find(address, key) }

                    // Preserve the declaration order from the full entry list
                    val ordered = if (descriptor != null) {
                        val setting = QuickSettings.getListSetting(device, descriptor.key)
                        val sp = GBApplication.getDeviceSpecificSharedPrefs(address)
                        val allEntries = setting?.let {
                            QuickSettings.resolveEntries(ctx, it, Prefs(sp))
                        } ?: emptyList()
                        val toSave = allEntries.map { it.value }.filter { it in selected }
                        if (toSave.size == allEntries.size) emptyList() else toSave
                    } else selected.toList()

                    DeviceTilePrefs.saveCycleValues(tileIdx, ordered)
                    notifyTile(tileIdx)
                    true
                }
            }

            /**
             * Populates [settingPref] with the DSL settings available on [device]. If [currentKey]
             * matches one of the available settings it is pre-selected; otherwise the preference is
             * left unset. Also resets [cyclePref] entries since the setting changed.
             */
            private fun populateSettingPref(
                settingPref: ListPreference,
                cyclePref: MultiSelectListPreference,
                device: GBDevice?,
                currentKey: String,
            ) {
                if (device == null) {
                    settingPref.entries = emptyArray()
                    settingPref.entryValues = emptyArray()
                    settingPref.value = null
                    settingPref.isVisible = false
                    cyclePref.entries = emptyArray()
                    cyclePref.entryValues = emptyArray()
                    return
                }
                val descriptors = QuickSettings.listFor(device)
                settingPref.entries = descriptors.map { getString(it.title) }.toTypedArray()
                settingPref.entryValues = descriptors.map { it.key }.toTypedArray()
                settingPref.value = currentKey.ifEmpty { null }
                settingPref.isVisible = true
            }

            /**
             * Populates [cyclePref] with the available [ListEntry] values for [descriptor], and
             * sets its visibility accordingly. Pre-selects [currentCycleValues] (or all entries if
             * empty). Hides the preference for non-list descriptors or when there is nothing to
             * populate - entries/entryValues must never be left null while visible, since
             * [MultiSelectListPreference]'s dialog crashes if opened without them.
             */
            private fun populateCyclePref(
                cyclePref: MultiSelectListPreference,
                device: GBDevice?,
                descriptor: QuickSettingDescriptor?,
                currentCycleValues: List<String>,
            ) {
                val setting = if (device != null && descriptor?.type == QuickSettingType.LIST) {
                    QuickSettings.getListSetting(device, descriptor.key)
                } else {
                    null
                }
                val ctx = requireContext()
                val allEntries = if (device != null && setting != null) {
                    val sp = GBApplication.getDeviceSpecificSharedPrefs(device.address)
                    QuickSettings.resolveEntries(ctx, setting, Prefs(sp))
                } else {
                    emptyList()
                }

                if (allEntries.isEmpty()) {
                    cyclePref.entries = emptyArray()
                    cyclePref.entryValues = emptyArray()
                    cyclePref.values = emptySet()
                    cyclePref.isVisible = false
                    return
                }

                cyclePref.entries = allEntries.map { entry ->
                    when (entry) {
                        is ListEntry.Res -> ctx.getString(entry.label)
                        is ListEntry.Text -> entry.label
                    }
                }.toTypedArray()
                cyclePref.entryValues = allEntries.map { it.value }.toTypedArray()

                val selected = currentCycleValues.ifEmpty { allEntries.map { it.value } }
                cyclePref.values = selected.toSet()
                cyclePref.isVisible = true
            }

            private fun notifyTile(tileIdx: Int) {
                val ctx = requireContext()
                val className = "${ctx.packageName}.activities.quicksettings.DeviceTileService$tileIdx"
                TileService.requestListeningState(ctx, ComponentName(ctx.packageName, className))
            }
        }
    }
}
