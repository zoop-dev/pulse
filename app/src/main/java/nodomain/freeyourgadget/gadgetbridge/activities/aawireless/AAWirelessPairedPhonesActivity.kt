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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.adapter.SimpleIconListAdapter
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivityAawirelessPairedPhonesBinding
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.RunnableListIconItem
import nodomain.freeyourgadget.gadgetbridge.service.devices.aawireless.AAWirelessPrefs
import nodomain.freeyourgadget.gadgetbridge.util.kotlin.getDevice
import androidx.core.content.edit

class AAWirelessPairedPhonesActivity : AbstractGBActivity() {
    private lateinit var gbDevice: GBDevice
    private lateinit var binding: ActivityAawirelessPairedPhonesBinding
    private lateinit var phoneAdapter: AAWirelessPairedPhoneAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAawirelessPairedPhonesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gbDevice = intent.getDevice()
            ?: throw IllegalArgumentException("GBDevice must not be null")

        phoneAdapter = AAWirelessPairedPhoneAdapter { phone -> showPhoneActionsDialog(phone) }
        binding.phonesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.phonesRecyclerView.adapter = phoneAdapter

        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).registerReceiver(
            broadcastReceiver,
            IntentFilter(GBDevice.ACTION_DEVICE_CHANGED)
        )

        refresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        @Suppress("DEPRECATION")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val changedDevice = intent.getDevice()
            if (changedDevice != null && changedDevice.address == gbDevice.address) {
                gbDevice = changedDevice
                refresh()
            }
        }
    }

    private fun prefs(): AAWirelessPrefs =
        AAWirelessPrefs(GBApplication.getDevicePrefs(gbDevice).preferences, gbDevice)

    private fun refresh() {
        val prefs = prefs()
        val dongleMode = prefs.enableDongleMode()

        binding.preferLastConnectedRow.visibility = if (dongleMode) View.GONE else View.VISIBLE
        binding.preferLastConnected.setOnCheckedChangeListener(null)
        binding.preferLastConnected.isChecked = prefs.preferLastConnected()
        binding.preferLastConnected.isEnabled = gbDevice.isInitialized
        binding.preferLastConnected.setOnCheckedChangeListener { _, isChecked -> setPreferLastConnected(isChecked) }

        val phonesCount = prefs.pairedPhoneCount
        val phones = (0 until phonesCount).map { i ->
            AAWirelessPairedPhone(
                mac = prefs.getPairedPhoneMac(i),
                name = prefs.getPairedPhoneName(i),
                position = i,
                isLast = i == phonesCount - 1,
                dongleMode = dongleMode,
            )
        }
        phoneAdapter.submit(phones, enabled = gbDevice.isInitialized)

        binding.phonesRecyclerView.visibility = if (phones.isEmpty()) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (phones.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setPreferLastConnected(enabled: Boolean) {
        GBApplication.getDevicePrefs(gbDevice).preferences.edit {
            putBoolean(AAWirelessPrefs.PREF_PREFER_LAST_CONNECTED, enabled)
        }
        GBApplication.deviceService(gbDevice).onSendConfiguration(AAWirelessPrefs.PREF_PREFER_LAST_CONNECTED)
    }

    private fun showPhoneActionsDialog(phone: AAWirelessPairedPhone) {
        val items = mutableListOf<RunnableListIconItem>()

        if (!phone.dongleMode) {
            items.add(
                RunnableListIconItem(getString(R.string.switch_to_phone, phone.name), R.drawable.ic_switch_left) {
                    sendPhoneBroadcast(AAWirelessPrefs.ACTION_PHONE_SWITCH, phone.mac)
                }
            )
        }
        if (phone.position > 0) {
            items.add(
                RunnableListIconItem(getString(R.string.widget_move_up), R.drawable.ic_arrow_upward) {
                    sendPhoneSortBroadcast(phone.mac, phone.position - 1)
                }
            )
        }
        if (!phone.isLast) {
            items.add(
                RunnableListIconItem(getString(R.string.widget_move_down), R.drawable.ic_arrow_downward) {
                    sendPhoneSortBroadcast(phone.mac, phone.position + 1)
                }
            )
        }
        items.add(
            RunnableListIconItem(getString(R.string.delete), R.drawable.ic_delete) {
                sendPhoneBroadcast(AAWirelessPrefs.ACTION_PHONE_DELETE, phone.mac)
            }
        )

        val adapter = SimpleIconListAdapter(this, items)
        MaterialAlertDialogBuilder(this)
            .setAdapter(adapter) { _, position -> items[position].action.run() }
            .setTitle(phone.name)
            .setNegativeButton(android.R.string.cancel) { _, _ -> }
            .create()
            .show()
    }

    private fun sendPhoneBroadcast(action: String, mac: String) {
        val intent = Intent(action)
        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, mac)
        intent.setPackage(BuildConfig.APPLICATION_ID)
        sendBroadcast(intent)
    }

    private fun sendPhoneSortBroadcast(mac: String, newPosition: Int) {
        val intent = Intent(AAWirelessPrefs.ACTION_PHONE_SORT)
        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_MAC, mac)
        intent.putExtra(AAWirelessPrefs.EXTRA_PHONE_NEW_POSITION, newPosition)
        intent.setPackage(BuildConfig.APPLICATION_ID)
        sendBroadcast(intent)
    }
}
