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
package nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.DialogPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractPreferenceFragment
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigBoolean
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigFloat
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigInteger
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigString
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppConfig
import nodomain.freeyourgadget.gadgetbridge.devices.garmin.GarminPreferences
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID

class DynamicAppConfigFragment : AbstractPreferenceFragment() {
    private lateinit var device: GBDevice
    private lateinit var appId: UUID
    private lateinit var appName: String

    private val preferences: MutableList<DynamicAppConfig> = mutableListOf()

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action == null) {
                LOG.error("Got null action")
                return
            }

            when (action) {
                ACTION_APP_CONFIG_EVENT -> {
                    val intentDevice = intent.getParcelableExtra<GBDevice?>(GBDevice.EXTRA_DEVICE)!!
                    if (intentDevice != device) {
                        LOG.debug("Ignoring intent for device {}, we are {}", intentDevice, device)
                        return
                    }
                    val event = intent.getParcelableExtra<GBDeviceEventAppConfig>(GBDeviceEventAppConfig.EXTRA_EVENT)!!
                    if (event.uuid != appId) {
                        LOG.debug("Ignoring intent for app {}, we are {}", event.uuid, appId)
                        return
                    }

                    when (event.event) {
                        GBDeviceEventAppConfig.Event.APP_CONFIG_GET_SUCCESS -> {
                            preferences.clear()
                            preferences.addAll(event.configs)
                        }

                        GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED -> {
                            GB.toast("Failed to get app configs", Toast.LENGTH_SHORT, GB.INFO)
                            requireActivity().finish()
                        }

                        GBDeviceEventAppConfig.Event.APP_CONFIG_SET_SUCCESS -> {
                            setPrefsEnabled(true)
                        }

                        GBDeviceEventAppConfig.Event.APP_CONFIG_SET_FAILED -> {
                            GB.toast("Failed to set app config", Toast.LENGTH_SHORT, GB.INFO)
                        }
                    }
                }

                else -> {
                    LOG.error("Unknown action {}", action)
                    return
                }
            }

            reload()
        }
    }

    private fun setArgs(device: GBDevice, appId: String, appName: String?) {
        val args: Bundle = arguments ?: Bundle()
        args.putParcelable(GBDevice.EXTRA_DEVICE, device)
        args.putString(EXTRA_APP_ID, appId)
        args.putString(EXTRA_APP_NAME, appName)
        setArguments(args)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val arguments = arguments ?: return
        this.device = arguments.getParcelable<GBDevice?>(GBDevice.EXTRA_DEVICE)!!
        this.appId = UUID.fromString(arguments.getString(EXTRA_APP_ID, null))!!
        this.appName = arguments.getString(EXTRA_APP_NAME, null)!!

        LOG.info("Open app config for {}", appId)

        preferenceManager.setSharedPreferencesName("app_config_" + device.address + "_" + appId)
        setPreferencesFromResource(R.xml.garmin_realtime_settings, rootKey)

        val filter = IntentFilter()
        filter.addAction(ACTION_APP_CONFIG_EVENT)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(mReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    override fun onDestroyView() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mReceiver)
        super.onDestroyView()
    }

    fun refreshFromDevice() {
        preferences.clear()
        reload()
        GBApplication.deviceService(device).onAppConfigRequest(appId)
    }

    fun reload() {
        val activity = getActivity()
        if (activity == null) {
            LOG.error("Activity is null")
            return
        }

        val prefScreen = findPreference<PreferenceScreen?>(GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS)
        if (prefScreen == null) {
            LOG.error("Preference screen for {} is null", GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS)
            activity.finish()
            return
        }

        if (preferences.isEmpty()) {
            (activity as DynamicAppConfigActivity).setActionBarTitle(activity.getString(R.string.loading))

            // Disable all existing preferences while loading
            setPrefsEnabled(false)

            GBApplication.deviceService(device).onAppConfigRequest(appId)

            return
        }

        prefScreen.removeAll()

        (activity as DynamicAppConfigActivity).setActionBarTitle(appName)

        prefScreen.addPreference(Preference(requireActivity()).apply {
            key = "header_warning"
            summary = "Values are not checked, invalid values might break the app"
            setIcon(R.drawable.ic_warning_gray)
            isSelectable = false
            isPersistent = false
        })

        for (preference in preferences) {
            val pref: Preference
            when (preference) {
                is AppConfigBoolean -> {
                    pref = SwitchPreferenceCompat(requireActivity())
                    pref.layoutResource = R.layout.preference_checkbox
                    pref.setChecked(preference.value)
                    pref.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                        LOG.debug("Pref {} bool changed to {}", pref, newValue)
                        setPrefsEnabled(false)
                        preferences[preferences.indexOf(preference)] =
                            AppConfigBoolean(preference.key, newValue as Boolean)
                        GBApplication.deviceService(device).onAppConfigSet(appId, ArrayList(preferences))
                        true
                    }
                }

                is AppConfigString -> {
                    pref = EditTextPreference(activity)
                    pref.dialogTitle = preference.key
                    pref.setText(preference.value)
                    pref.summary = preference.value
                    pref.setOnBindEditTextListener {
                        it.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS)
                        it.setSelection(it.getText().length)
                    }
                    pref.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                        LOG.debug("Pref {} string changed to {}", pref, newValue)
                        setPrefsEnabled(false)
                        pref.isEnabled = false
                        preferences[preferences.indexOf(preference)] =
                            AppConfigString(preference.key, newValue as String)
                        GBApplication.deviceService(device).onAppConfigSet(appId, ArrayList(preferences))
                        true
                    }
                }

                is AppConfigFloat -> {
                    pref = EditTextPreference(activity)
                    pref.dialogTitle = preference.key
                    pref.setText(preference.value.toString())
                    pref.summary = preference.value.toString()
                    pref.setOnBindEditTextListener {
                        it.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED)
                        it.setSelection(it.getText().length)
                    }
                    pref.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                        LOG.debug("Pref {} float changed to {}", pref, newValue)
                        pref.isEnabled = false
                        preferences[preferences.indexOf(preference)] =
                            AppConfigFloat(preference.key, (newValue as String).toFloat())
                        GBApplication.deviceService(device).onAppConfigSet(appId, ArrayList(preferences))
                        true
                    }
                }

                is AppConfigInteger -> {
                    pref = EditTextPreference(activity)
                    pref.dialogTitle = preference.key
                    pref.setText(preference.value.toString())
                    pref.summary = preference.value.toString()
                    pref.setOnBindEditTextListener {
                        it.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED)
                        it.setSelection(it.getText().length)
                    }
                    pref.setOnPreferenceChangeListener { _: Preference?, newValue: Any? ->
                        LOG.debug("Pref {} int changed to {}", pref, newValue)
                        setPrefsEnabled(false)
                        preferences[preferences.indexOf(preference)] =
                            AppConfigInteger(preference.key, (newValue as String).toInt())
                        GBApplication.deviceService(device).onAppConfigSet(appId, ArrayList(preferences))
                        true
                    }
                }
            }

            pref.isIconSpaceReserved = false
            pref.isPersistent = false
            pref.title = preference.key
            pref.setKey(preference.key)
            prefScreen.addPreference(pref)
        }
    }

    fun setPrefsEnabled(enabled: Boolean) {
        val prefScreen = findPreference<PreferenceScreen?>(GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS)
        if (prefScreen == null) {
            LOG.error("Preference screen for {} is null", GarminPreferences.PREF_GARMIN_REALTIME_SETTINGS)
            requireActivity().finish()
            return
        }
        for (i in 0..<prefScreen.preferenceCount) {
            prefScreen.getPreference(i).isEnabled = enabled
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(DynamicAppConfigFragment::class.java)

        const val ACTION_APP_CONFIG_EVENT: String = "nodomain.freeyourgadget.gadgetbridge.app_config_event"

        const val EXTRA_APP_ID: String = "app_id"
        const val EXTRA_APP_NAME: String = "app_name"

        fun newInstance(device: GBDevice, appId: String, appName: String?): DynamicAppConfigFragment {
            val fragment = DynamicAppConfigFragment()
            fragment.setArgs(device, appId, appName)
            return fragment
        }
    }
}
