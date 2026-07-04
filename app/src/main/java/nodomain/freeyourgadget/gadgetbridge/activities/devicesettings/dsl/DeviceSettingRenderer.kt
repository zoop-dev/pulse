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
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.util.preferences.GBSimpleSummaryProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handle returned by [DeviceSettingRenderer.render]. Call [run] to refresh visibility and dynamic
 * entries; call [cleanup] in [androidx.preference.PreferenceFragmentCompat.onDestroyView] to
 * unregister any SharedPreferences listeners registered for callbacks.
 */
class DeviceSettingsRefreshHandle(
    private val sp: SharedPreferences,
    private val spListeners: List<SharedPreferences.OnSharedPreferenceChangeListener>,
    private val refreshAction: Runnable,
) : Runnable {
    override fun run() = refreshAction.run()
    fun cleanup() = spListeners.forEach { sp.unregisterOnSharedPreferenceChangeListener(it) }
}

/**
 * Converts a list of [DeviceSetting] nodes into androidx [Preference] objects and adds them to
 * a parent [PreferenceGroup]. Returns a [DeviceSettingsRefreshHandle] that re-evaluates all
 * [DeviceSetting.visibleWhen] predicates and repopulates all dynamic [ListSetting.entriesProvider]
 * when invoked. Call [DeviceSettingsRefreshHandle.cleanup] when the fragment stops to unregister
 * any SharedPreferences listeners.
 *
 * [CategorySetting] nodes are automatically hidden when all of their member preferences (those
 * between this category and the next one or an [XmlScreenSetting]) are invisible.
 */
object DeviceSettingRenderer {
    private val LOG: Logger = LoggerFactory.getLogger(DeviceSettingRenderer::class.java)

    fun render(
        items: List<DeviceSetting>,
        parent: PreferenceGroup,
        prefs: Prefs,
        handler: DeviceSpecificSettingsHandler,
    ): DeviceSettingsRefreshHandle {
        val visibilityPairs = mutableListOf<Pair<Preference, (Prefs) -> Boolean>>()
        val dynamicEntryPairs = mutableListOf<Pair<ListPreference, (Prefs) -> List<ListEntry>>>()
        val categoryMemberPairs = mutableListOf<Pair<PreferenceCategory, MutableList<Preference>>>()
        val spListeners = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()
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
            categoryMemberPairs.forEach { (cat, members) ->
                cat.isVisible = members.isNotEmpty() && members.any { it.isVisible }
            }
        }

        val postRefresh: () -> Unit = {
            mainHandler.post {
                refreshRunnable.run()
            }
        }

        renderItems(
            items,
            parent,
            prefs,
            handler,
            visibilityPairs,
            dynamicEntryPairs,
            categoryMemberPairs,
            spListeners,
            mainHandler,
            sp,
            postRefresh
        )

        // Initial visibility passes - individual predicates first, then category membership
        visibilityPairs.forEach { (pref, predicate) -> pref.isVisible = predicate(prefs) }
        categoryMemberPairs.forEach { (cat, members) ->
            cat.isVisible = members.isNotEmpty() && members.any { it.isVisible }
        }

        return DeviceSettingsRefreshHandle(sp, spListeners, refreshRunnable)
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
        categoryMemberPairs: MutableList<Pair<PreferenceCategory, MutableList<Preference>>>,
        spListeners: MutableList<SharedPreferences.OnSharedPreferenceChangeListener>,
        mainHandler: Handler,
        sp: SharedPreferences,
        postRefresh: () -> Unit,
    ) {
        LOG.debug("Rendering device settings")

        val context = handler.context

        for (setting in items) {
            LOG.debug("Rendering {} as {}", setting.key, setting.javaClass.simpleName)

            val pref: Preference = when (setting) {
                is CategorySetting -> {
                    val category = PreferenceCategory(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                    }
                    parent.addPreference(category)
                    renderItems(
                        setting.children,
                        category,
                        prefs,
                        handler,
                        visibilityPairs,
                        dynamicEntryPairs,
                        categoryMemberPairs,
                        spListeners,
                        mainHandler,
                        sp,
                        postRefresh
                    )
                    val members = (0 until category.preferenceCount).map { category.getPreference(it) }.toMutableList()
                    if (members.isNotEmpty()) {
                        categoryMemberPairs.add(category to members)
                    }
                    if (setting.visibleWhen != null) {
                        visibilityPairs.add(category to setting.visibleWhen)
                    }
                    continue
                }

                is ScreenSetting -> {
                    val screen = parent.preferenceManager.createPreferenceScreen(context)
                    screen.key = setting.key
                    screen.setTitle(setting.title)
                    if (setting.summary != 0) screen.setSummary(setting.summary)
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
                        categoryMemberPairs,
                        spListeners,
                        mainHandler,
                        sp,
                        postRefresh
                    )
                    if (setting.visibleWhen != null) {
                        visibilityPairs.add(screen to setting.visibleWhen)
                    }
                    continue
                }

                is SwitchSetting -> {
                    SwitchPreferenceCompat(context).apply {
                        key = setting.key
                        setTitle(setting.title)
                        when {
                            setting.summaryOn != 0 || setting.summaryOff != 0 -> {
                                if (setting.summaryOn != 0) setSummaryOn(setting.summaryOn)
                                if (setting.summaryOff != 0) setSummaryOff(setting.summaryOff)
                            }

                            setting.summary != 0 -> setSummary(setting.summary)
                        }
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
                        if (setting.summary != 0) {
                            setSummary(setting.summary)
                        } else {
                            summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                        }
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
                        if (setting.summary != 0) setSummary(setting.summary)
                        if (setting.icon != 0) setIcon(setting.icon)
                        // TODO: Not supported by our sdk version: min = setting.min
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
                        isEnabled = setting.enabled
                        if (setting.summary != 0) {
                            setSummary(setting.summary)
                        } else if (setting.defaultSummary != null && setting.summaryTemplate != 0) {
                            summaryProvider = GBSimpleSummaryProvider(
                                setting.defaultSummary.invoke(context),
                                setting.summaryTemplate,
                            )
                        } else {
                            summaryProvider = EditTextPreference.SimpleSummaryProvider.getInstance()
                        }
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
                        if (setting.onSharedPreferenceChanged != null) {
                            val listener =
                                SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, changedKey ->
                                    if (changedKey == setting.key) {
                                        val newValue = sharedPrefs.getString(changedKey, setting.defaultValue)
                                            ?: setting.defaultValue
                                        mainHandler.post { setting.onSharedPreferenceChanged.invoke(newValue) }
                                    }
                                }
                            spListeners.add(listener)
                            sp.registerOnSharedPreferenceChangeListener(listener)
                        }
                    }
                }

                is ActionSetting -> {
                    Preference(context).apply {
                        key = setting.key
                        if (setting.title != 0) setTitle(setting.title)
                        if (setting.summary != 0) setSummary(setting.summary)
                        if (setting.icon != 0) setIcon(setting.icon)
                        isPersistent = false
                        setOnPreferenceClickListener {
                            setting.onClick?.invoke(handler) ?: false
                        }
                    }
                }

                is XmlScreenSetting -> {
                    // Inflate the root XML entry at the correct position so it appears in the
                    // order declared in the DSL rather than being appended at the end.
                    handler.addXmlPreferences(setting.screen.xml)
                    continue
                }
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
