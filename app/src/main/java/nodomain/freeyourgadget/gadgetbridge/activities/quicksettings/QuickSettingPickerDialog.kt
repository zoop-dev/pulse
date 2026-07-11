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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.dsl.QuickSettingDescriptor

/**
 * Shows a picker dialog for a list of [QuickSettingDescriptor]s, grouped under non-selectable
 * headers naming the DSL category/screen each setting belongs to. Devices can expose many
 * similarly-named settings across different screens, so a flat list makes it unclear which one
 * is which; grouping mirrors the structure the user already sees in the device's own settings.
 */
object QuickSettingPickerDialog {

    private sealed class Row {
        data class Header(val label: String) : Row()
        data class Item(val descriptor: QuickSettingDescriptor, val label: String) : Row()
    }

    fun show(
        context: Context,
        title: CharSequence,
        settings: List<QuickSettingDescriptor>,
        selectedKey: String? = null,
        onCancel: (() -> Unit)? = null,
        onSelected: (QuickSettingDescriptor) -> Unit,
    ) {
        val rows = mutableListOf<Row>()
        settings.groupBy { it.category }.forEach { (category, items) ->
            if (category != 0) {
                rows.add(Row.Header(context.getString(category)))
            }
            items.forEach { rows.add(Row.Item(it, context.getString(it.title))) }
        }

        val adapter = object : ArrayAdapter<Row>(context, 0, rows) {
            override fun getViewTypeCount() = 2

            override fun getItemViewType(position: Int) =
                if (getItem(position) is Row.Header) 0 else 1

            override fun isEnabled(position: Int) = getItem(position) !is Row.Header

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return when (val row = getItem(position)!!) {
                    is Row.Header -> {
                        val view = convertView ?: LayoutInflater.from(context).inflate(
                            R.layout.list_item_quick_setting_picker_header, parent, false,
                        )
                        view.findViewById<TextView>(R.id.quick_setting_picker_header_label).text = row.label
                        view
                    }

                    is Row.Item -> {
                        val view = convertView ?: LayoutInflater.from(context).inflate(
                            R.layout.list_item_quick_setting_picker, parent, false,
                        )
                        view.findViewById<TextView>(R.id.quick_setting_picker_item_label).text = row.label
                        view.findViewById<RadioButton>(R.id.quick_setting_picker_item_radio).isChecked =
                            row.descriptor.key == selectedKey
                        view
                    }
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setAdapter(adapter, null)
            .setOnCancelListener { onCancel?.invoke() }
            .create()
        dialog.show()

        // Wired up after show() since the AlertDialog only inflates its ListView at that point.
        dialog.listView.setOnItemClickListener { _, _, position, _ ->
            (adapter.getItem(position) as? Row.Item)?.let {
                dialog.dismiss()
                onSelected(it.descriptor)
            }
        }
    }
}
