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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityInternetFirewallBinding
import nodomain.freeyourgadget.gadgetbridge.databinding.DialogFirewallAddRuleBinding
import nodomain.freeyourgadget.gadgetbridge.entities.InternetFirewallRule
import nodomain.freeyourgadget.gadgetbridge.entities.InternetFirewallRuleDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.internet.FirewallAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InternetFirewallActivity : AbstractGBActivity() {
    private lateinit var binding: ActivityInternetFirewallBinding
    private lateinit var adapter: InternetFirewallRulesAdapter
    private var device: GBDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(GBDevice.EXTRA_DEVICE, GBDevice::class.java)
        } else {
            intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)
        }

        binding = ActivityInternetFirewallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = InternetFirewallRulesAdapter(
            onRuleClick = { rule -> showDomainDialog(rule) }
        )

        binding.firewallRulesList.layoutManager = LinearLayoutManager(this)
        binding.firewallRulesList.adapter = adapter

        // FAB
        binding.fabAddDomain.setOnClickListener { showDomainDialog(null) }

        // Show empty state initially, will be hidden if rules exist after load
        updateEmptyState(true)

        loadRules()
    }

    private fun loadRules() {
        try {
            GBApplication.acquireDB().use { db ->
                val qb = db.daoSession.internetFirewallRuleDao.queryBuilder()
                if (device != null) {
                    // device-specific rules
                    val deviceFromDb = DBHelper.getDevice(device, db.daoSession)
                    qb.where(InternetFirewallRuleDao.Properties.DeviceId.eq(deviceFromDb.id))
                } else {
                    // global rules
                    qb.where(InternetFirewallRuleDao.Properties.DeviceId.isNull)
                }
                qb.orderAsc(InternetFirewallRuleDao.Properties.Domain)
                val rules = qb.list()
                LOG.debug("Loaded {} firewall rules from database", rules.size)
                adapter.updateRules(rules)
                updateEmptyState(rules.isEmpty())
            }
        } catch (e: Exception) {
            LOG.error("Error loading rules from database", e)
            updateEmptyState(true)
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyStateText.text = getString(
            R.string.internet_firewall_no_domains,
            getString(R.string.internet_firewall_block)
        )
        if (isEmpty) {
            binding.firewallRulesList.visibility = View.GONE
            binding.emptyStateText.visibility = View.VISIBLE
        } else {
            binding.firewallRulesList.visibility = View.VISIBLE
            binding.emptyStateText.visibility = View.GONE
        }
    }

    private fun showDomainDialog(existingRule: InternetFirewallRule? = null) {
        val dialogBinding = DialogFirewallAddRuleBinding.inflate(LayoutInflater.from(this))
        val isEditing = existingRule != null

        // Pre-populate fields if editing
        if (isEditing) {
            dialogBinding.domainInputText.setText(existingRule.domain)
        }

        // Setup dropdown with actions
        val textAllow = getString(R.string.internet_firewall_allow)
        val textBlock = getString(R.string.internet_firewall_block)
        if (textAllow == textBlock) {
            throw RuntimeException("Allow and block actions have the same translation!")
        }

        val actions = arrayOf(textAllow, textBlock,).sorted()
        val actionAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, actions)
        dialogBinding.actionDropdown.setAdapter(actionAdapter)

        // Set default action
        val defaultActionIndex = if (isEditing) {
            if (existingRule.action == FirewallAction.ALLOW.name) actions.indexOf(textAllow) else actions.indexOf(textBlock)
        } else {
            actions.indexOf(textAllow) // Default to "Allow" for new entries
        }
        dialogBinding.actionDropdown.setText(actions[defaultActionIndex], false)

        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setCancelable(true)
            .setTitle(if (isEditing) R.string.internet_firewall_edit_domain else R.string.internet_firewall_add_domain)
            .setPositiveButton(if (isEditing) R.string.save else R.string.ok) { _, _ ->
                val domain = dialogBinding.domainInputText.text.toString().trim()
                val selectedAction = dialogBinding.actionDropdown.text.toString()
                val action = if (selectedAction == textAllow) {
                    FirewallAction.ALLOW.name
                } else {
                    FirewallAction.BLOCK.name
                }

                if (isEditing) {
                    updateRuleInDatabase(existingRule, domain, action)
                } else {
                    saveRuleToDatabase(domain, action)
                }
                loadRules()
            }.setNegativeButton(R.string.Cancel, null)

        // Add delete button if editing
        if (isEditing) {
            dialogBuilder.setNeutralButton(R.string.Delete) { _, _ ->
                deleteRuleFromDatabase(existingRule)
                loadRules()
            }
        }

        val dialog = dialogBuilder.show()

        // Disable OK/Save button if adding new domain. When editing, it's valid from the start
        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        if (!isEditing) {
            okButton.isEnabled = false
        }

        // Ensure valid domain
        dialogBinding.domainInputText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val domain = s.toString().trim()
                val isValid = domain.isNotBlank()
                okButton.isEnabled = isValid

                //if (domain.isEmpty() || isValid) {
                //    dialogBinding.domainInputLayout.error = null
                //} else {
                //    dialogBinding.domainInputLayout.error = getString(R.string.invalid_domain)
                //}
            }
        })
    }

    private fun saveRuleToDatabase(domain: String, action: String) {
        try {
            GBApplication.acquireDB().use { db ->
                val rule = InternetFirewallRule()
                rule.domain = domain
                rule.action = action
                if (device != null) {
                    val deviceFromDb = DBHelper.getDevice(device, db.daoSession)
                    rule.deviceId = deviceFromDb.id
                }
                db.daoSession.internetFirewallRuleDao.insert(rule)
                LOG.info("Added firewall rule: {} -> {}", domain, action)
            }
        } catch (e: Exception) {
            LOG.error("Error adding domain to database", e)
        }
    }

    private fun updateRuleInDatabase(rule: InternetFirewallRule, newDomain: String, newAction: String) {
        LOG.info("Updating firewall rule: {}/{} -> {}/{}", rule.domain, rule.action, newDomain, newAction)

        try {
            GBApplication.acquireDB().use { db ->
                rule.domain = newDomain
                rule.action = newAction
                db.daoSession.internetFirewallRuleDao.update(rule)
            }
        } catch (e: Exception) {
            LOG.error("Error updating rule in database", e)
        }
    }

    private fun deleteRuleFromDatabase(rule: InternetFirewallRule) {
        LOG.info("Deleting firewall rule: {}/{}", rule.domain, rule.action)

        try {
            GBApplication.acquireDB().use { db ->
                db.daoSession.internetFirewallRuleDao.delete(rule)
            }
        } catch (e: Exception) {
            LOG.error("Error deleting rule from database", e)
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(InternetFirewallActivity::class.java)
    }
}
