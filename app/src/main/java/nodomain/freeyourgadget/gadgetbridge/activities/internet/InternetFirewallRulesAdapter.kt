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
package nodomain.freeyourgadget.gadgetbridge.activities.internet

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.databinding.FirewallRuleItemBinding
import nodomain.freeyourgadget.gadgetbridge.entities.InternetFirewallRule

class InternetFirewallRulesAdapter(
    private val onRuleClick: (InternetFirewallRule) -> Unit
) : RecyclerView.Adapter<InternetFirewallRulesAdapter.RuleViewHolder>() {

    private val rules = mutableListOf<InternetFirewallRule>()

    @SuppressLint("NotifyDataSetChanged")
    fun updateRules(newRules: List<InternetFirewallRule>) {
        rules.clear()
        rules.addAll(newRules)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val binding = FirewallRuleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        holder.bind(rules[position], onRuleClick)
    }

    override fun getItemCount(): Int = rules.size

    class RuleViewHolder(val binding: FirewallRuleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(rule: InternetFirewallRule, onRuleClick: (InternetFirewallRule) -> Unit) {
            binding.domainText.text = rule.domain

            val context = itemView.context
            binding.actionText.text = when (rule.action) {
                "ALLOW" -> context.getString(R.string.internet_firewall_allow)
                "BLOCK" -> context.getString(R.string.internet_firewall_block)
                else -> rule.action
            }

            itemView.setOnClickListener { onRuleClick(rule) }
        }
    }
}
