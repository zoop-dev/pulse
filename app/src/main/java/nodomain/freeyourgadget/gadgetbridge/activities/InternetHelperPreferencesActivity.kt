/*  Copyright (C) 2025 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.entities.URLFilterEntry
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils
import nodomain.freeyourgadget.gadgetbridge.util.PermissionsUtils.PACKAGE_INTERNET_HELPER

class InternetHelperPreferencesActivity : AbstractGBActivity() {
    val urlItems = ArrayList<UrlListAdapter.UrlEntry>()

    private fun retrieveList() {
        val urlFilterEntries = DBHelper.getURLFilterEntries()
        urlItems.clear()
        for (entry in urlFilterEntries) {
            urlItems.add(UrlListAdapter.UrlEntry(entry))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internet_helper_preferences)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, InternetHelperPreferencesFragment())
            .commit()

        retrieveList()

        val recyclerView = findViewById<RecyclerView>(R.id.internet_helper_url_list)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = UrlListAdapter(this, urlItems) { entry, action ->
            when (action) {
                UrlListAdapter.UrlAction.ALLOW -> {
                    entry.urlFilterEntry.allowed = true
                    DBHelper.store(entry.urlFilterEntry)
                    recyclerView.adapter?.notifyDataSetChanged()
                }
                UrlListAdapter.UrlAction.DENY -> {
                    entry.urlFilterEntry.allowed = false
                    DBHelper.store(entry.urlFilterEntry)
                    recyclerView.adapter?.notifyDataSetChanged()
                }
                UrlListAdapter.UrlAction.EDIT -> {
                    val inputLayout = TextInputLayout(this)
                    val editText = TextInputEditText(this).apply {
                        setText(entry.urlFilterEntry.url)
                    }
                    inputLayout.addView(editText)
                    inputLayout.hint = getString(R.string.internet_helper_url_filter_hint)

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.internet_helper_url_filter_title))
                        .setView(inputLayout)
                        .setPositiveButton(R.string.save) { dialog, _ ->
                            entry.urlFilterEntry.url = editText.text.toString()
                            DBHelper.store(entry.urlFilterEntry)
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                        .setNegativeButton(getString(R.string.Cancel), null)
                        .show()
                }
                UrlListAdapter.UrlAction.DELETE -> {
                    MaterialAlertDialogBuilder(this)
                        .setPositiveButton(R.string.yes) {_,_ ->
                            DBHelper.delete(entry.urlFilterEntry)
                            retrieveList()
                            recyclerView.adapter?.notifyDataSetChanged()
                        }
                        .setNegativeButton(R.string.no, null)
                        .setMessage(getString(R.string.internet_helper_url_filter_delete))
                        .show()
                }
            }
        }
    }

    class InternetHelperPreferencesFragment : AbstractPreferenceFragment() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?
        ) {
            setPreferencesFromResource(R.xml.internethelper_preferences, rootKey)
            val installWarning = findPreference<Preference>("pref_key_internethelper_not_installed")
            if (AndroidUtils.isPackageInstalled(PACKAGE_INTERNET_HELPER)) {
                installWarning?.isVisible = false
            } else {
                installWarning?.setOnPreferenceClickListener {
                    val startIntent = Intent(Intent.ACTION_VIEW)
                    startIntent.data = "https://codeberg.org/Freeyourgadget/Internethelper/releases".toUri()
                    startActivity(startIntent)
                    true
                }
            }
        }
    }

    class UrlListAdapter(
        private val context: Context,
        private val urls: List<UrlEntry>,
        private val onAction: (UrlEntry, UrlAction) -> Unit
    ) : RecyclerView.Adapter<UrlListAdapter.UrlViewHolder>() {

        data class UrlEntry(
            val urlFilterEntry: URLFilterEntry,
        )

        enum class UrlAction {
            ALLOW, DENY, EDIT, DELETE
        }

        inner class UrlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val title: TextView = itemView.findViewById(R.id.url_title)
            val status: TextView = itemView.findViewById(R.id.url_status)
            val menuButton: ImageButton = itemView.findViewById(R.id.url_menu_button)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_url_list_entry, parent, false)
            return UrlViewHolder(view)
        }

        override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
            val entry = urls[position]

            holder.title.text = entry.urlFilterEntry.url
            holder.status.text = if (entry.urlFilterEntry.allowed)
                context.getString(R.string.internet_helper_url_allowed)
            else
                context.getString(R.string.internet_helper_url_denied)

            holder.menuButton.setOnClickListener {
                showPopupMenu(holder.menuButton, entry)
            }
        }

        private fun showPopupMenu(anchor: View, entry: UrlEntry) {
            val popupMenu = PopupMenu(anchor.context, anchor)
            if (entry.urlFilterEntry.allowed) {
                popupMenu.menu.add(Menu.NONE, 1, Menu.NONE,
                    context.getString(R.string.internet_helper_url_action_deny))
            } else {
                popupMenu.menu.add(Menu.NONE, 2, Menu.NONE,
                    context.getString(R.string.internet_helper_url_action_allow))
            }
            popupMenu.menu.add(Menu.NONE, 3, Menu.NONE,
                context.getString(R.string.internet_helper_url_action_edit))
            popupMenu.menu.add(Menu.NONE, 4, Menu.NONE,
                context.getString(R.string.internet_helper_url_action_delete))

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> onAction(entry, UrlAction.DENY)
                    2 -> onAction(entry, UrlAction.ALLOW)
                    3 -> onAction(entry, UrlAction.EDIT)
                    4 -> onAction(entry, UrlAction.DELETE)
                }
                true
            }

            popupMenu.show()
        }

        override fun getItemCount(): Int = urls.size
    }
}