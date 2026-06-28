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
package nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Converts a list of [DeviceSetting] nodes into androidx [Preference] objects and adds them to
 * a parent [PreferenceGroup]. Returns a [Runnable] that re-evaluates all [DeviceSetting.visibleWhen] predicates
 * and repopulates all dynamic [ListSetting.entriesProvider] when invoked (e.g. on device state change).
 * The caller is responsible for invoking it when needed.
 */
object DeviceSettingRenderer {
    private val LOG: Logger = LoggerFactory.getLogger(DeviceSettingRenderer::class.java)

    fun render(
        items: List<DeviceSetting>,
        parent: PreferenceGroup,
        prefs: Prefs,
        handler: DeviceSpecificSettingsHandler,
    ): Runnable {
        val visibilityPairs = mutableListOf<Pair<Preference, (Prefs) -> Boolean>>()
        val dynamicEntryPairs = mutableListOf<Pair<ListPreference, (Prefs) -> List<ListEntry>>>()
        val mainHandler = Handler(Looper.getMainLooper())
        val sp: SharedPreferences = prefs.preferences

        val refreshRunnable = Runnable {
            LOG.debug("Refreshing device settings")

            val livePrefs = Prefs(sp)
            val context = handler.context
            visibilityPairs.forEach { (pref, predicate) -> pref.isVisible = predicate(livePrefs) }
            dynamicEntryPairs.forEach { (pref, provider) ->
                applyEntries(pref, provider(livePrefs), context)
            }
        }

        val postRefresh: () -> Unit = {
            mainHandler.post {
                refreshRunnable.run()
            }
        }

        renderItems(items, parent, prefs, handler, visibilityPairs, dynamicEntryPairs, postRefresh)

        // Initial visibility pass
        visibilityPairs.forEach { (pref, predicate) -> pref.isVisible = predicate(prefs) }

        return refreshRunnable
    }

    private fun applyEntries(pref: ListPreference, entries: List<ListEntry>, context: Context) {
        pref.entries = entries.map { entry ->
            when (entry) {
                is ListEntry.Res -> context.getString(entry.label)
                is ListEntry.Text -> entry.label
            }
        }.toTypedArray()
        pref.entryValues = entries.map { it.value }.toTypedArray()
    }

    private fun renderItems(
        items: List<DeviceSetting>,
        parent: PreferenceGroup,
        prefs: Prefs,
        handler: DeviceSpecificSettingsHandler,
        visibilityPairs: MutableList<Pair<Preference, (Prefs) -> Boolean>>,
        dynamicEntryPairs: MutableList<Pair<ListPreference, (Prefs) -> List<ListEntry>>>,
        postRefresh: () -> Unit,
    ) {
        LOG.debug("Rendering device settings")

        val context = handler.context
        for (setting in items) {
            LOG.debug("Rendering {} as {}", setting.key, setting.javaClass.simpleName)

            val pref: Preference = when (setting) {
                is ScreenSetting -> {
                    val screen = parent.preferenceManager.createPreferenceScreen(context)
                    screen.key = setting.key
                    screen.setTitle(setting.title)
                    if (setting.icon != 0) screen.setIcon(setting.icon)
                    // Must be added to parent before rendering children so that dependencies can be resolved
                    parent.addPreference(screen)
                    renderItems(
                        setting.children,
                        screen,
                        prefs,
                        handler,
                        visibilityPairs,
                        dynamicEntryPairs,
                        postRefresh
                    )
                    if (setting.visibleWhen != null) {
                        visibilityPairs.add(screen to setting.visibleWhen!!)
                    }
                    continue
                }

                is SwitchSetting -> {
                    SwitchPreferenceCompat(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        if (setting.icon != 0) setIcon(setting.icon)
                        setDefaultValue(setting.defaultValue)
                        setOnPreferenceChangeListener { _, _ ->
                            handler.notifyPreferenceChanged(setting.key)
                            postRefresh()
                            true
                        }
                    }
                }

                is ListSetting -> {
                    ListPreference(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        setDialogTitle(setting.title)
                        if (setting.icon != 0) setIcon(setting.icon)
                        when {
                            setting.entriesProvider != null -> {
                                applyEntries(this, setting.entriesProvider.invoke(prefs), context)
                                dynamicEntryPairs.add(this to setting.entriesProvider)
                            }

                            setting.entries.isNotEmpty() -> applyEntries(this, setting.entries, context)

                            else -> {
                                setEntries(setting.entriesRes)
                                setEntryValues(setting.entryValuesRes)
                            }
                        }
                        setDefaultValue(setting.defaultValue)
                        summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                        setOnPreferenceChangeListener { _, _ ->
                            handler.notifyPreferenceChanged(setting.key)
                            postRefresh()
                            true
                        }
                    }
                }

                is SeekBarSetting -> {
                    SeekBarPreference(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        if (setting.icon != 0) setIcon(setting.icon)
                        min = setting.min
                        max = setting.max
                        setDefaultValue(setting.defaultValue)
                        showSeekBarValue = setting.showValue
                        setOnPreferenceChangeListener { _, _ ->
                            handler.notifyPreferenceChanged(setting.key)
                            postRefresh()
                            true
                        }
                    }
                }

                is TextSetting -> {
                    EditTextPreference(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        setDialogTitle(setting.title)
                        if (setting.icon != 0) setIcon(setting.icon)
                        setDefaultValue(setting.defaultValue)
                        summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                        setOnBindEditTextListener { editText ->
                            editText.inputType = setting.inputType
                            if (setting.maxLength != null) {
                                editText.filters = arrayOf<InputFilter>(LengthFilter(setting.maxLength))
                            }
                            setting.onBindEditText?.invoke(editText)
                        }
                        setOnPreferenceChangeListener { _, _ ->
                            handler.notifyPreferenceChanged(setting.key)
                            postRefresh()
                            true
                        }
                    }
                }

                is ActionSetting -> {
                    Preference(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        if (setting.icon != 0) setIcon(setting.icon)
                        isPersistent = false
                        setOnPreferenceClickListener {
                            setting.onClick?.invoke(handler) ?: false
                        }
                    }
                }

                is XmlScreenSetting -> continue
            }

            parent.addPreference(pref)

            // Dependency must be set after addPreference
            val dependency: String? = when (setting) {
                is SwitchSetting -> setting.dependency
                is ListSetting -> setting.dependency
                is SeekBarSetting -> setting.dependency
                is TextSetting -> setting.dependency
                else -> null
            }
            if (dependency != null) pref.dependency = dependency

            if (setting.visibleWhen != null) {
                visibilityPairs.add(pref to setting.visibleWhen!!)
            }
        }
    }
}
