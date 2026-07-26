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
package nodomain.freeyourgadget.gadgetbridge.activities.aawireless

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import nodomain.freeyourgadget.gadgetbridge.R

class AAWirelessPairedPhoneAdapter(
    private val onClick: (AAWirelessPairedPhone) -> Unit
) : RecyclerView.Adapter<AAWirelessPairedPhoneAdapter.PhoneViewHolder>() {

    private val phones = mutableListOf<AAWirelessPairedPhone>()
    var enabled = true

    class PhoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val row: View = itemView.findViewById(R.id.phone_row)
        val name: TextView = itemView.findViewById(R.id.phone_name)
        val mac: TextView = itemView.findViewById(R.id.phone_mac)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submit(newPhones: List<AAWirelessPairedPhone>, enabled: Boolean) {
        phones.clear()
        phones.addAll(newPhones)
        this.enabled = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhoneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aawireless_paired_phone, parent, false)
        return PhoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhoneViewHolder, position: Int) {
        val phone = phones[position]
        holder.name.text = phone.name
        holder.mac.text = phone.mac
        holder.row.isEnabled = enabled
        holder.itemView.alpha = if (enabled) 1f else 0.5f
        holder.row.setOnClickListener {
            if (enabled) onClick(phone)
        }
    }

    override fun getItemCount(): Int = phones.size
}
