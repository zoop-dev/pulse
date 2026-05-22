/*  Copyright (C) 2026  Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.garmin.actions

import android.content.BroadcastReceiver
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService.EXTRA_OPTIONS
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsHandler
import nodomain.freeyourgadget.gadgetbridge.activities.install.FileInstallerActivity
import nodomain.freeyourgadget.gadgetbridge.databinding.ActivitySendWaypointBinding
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.DistanceUnit
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport.BUNDLE_EXTRA_INSTALL_BYTES
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport.BUNDLE_EXTRA_INSTALL_TASK_NAME
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.WaypointHelper
import org.slf4j.LoggerFactory

class GarminSendWaypointActivity : AbstractGBActivity() {
    private var device: GBDevice? = null
    private lateinit var binding: ActivitySendWaypointBinding

    private var elevationInFeet = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                GB.ACTION_SET_FINISHED -> {
                    GB.toast(this@GarminSendWaypointActivity, R.string.devicestatus_upload_completed, Toast.LENGTH_SHORT, GB.INFO);
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySendWaypointBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val distanceUnit = GBApplication.getPrefs().distanceUnit
        val uom: String
        if (distanceUnit == DistanceUnit.IMPERIAL) {
            uom = getString(R.string.activity_send_waypoint_feet)
            elevationInFeet = true
        } else {
            uom = getString(R.string.activity_send_waypoint_meter)
            elevationInFeet = false
        }
        binding.waypointElevationLabel.text =
            getString(R.string.activity_send_waypoint_elevation_label, uom)

        val copyListener = ClickToCopy()
        binding.waypointInfo0.setOnClickListener(copyListener)
        binding.waypointInfo1.setOnClickListener(copyListener)
        binding.waypointInfo2.setOnClickListener(copyListener)
        binding.waypointInfo3.setOnClickListener(copyListener)

        device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE)

        if (savedInstanceState != null) {
            binding.waypointName.setText(savedInstanceState.getString(ITEM_NAME, ""))
            binding.waypointLatitude.setText(savedInstanceState.getString(ITEM_LATITUDE, ""))
            binding.waypointLongitude.setText(savedInstanceState.getString(ITEM_LONGITUDE, ""))
            binding.waypointElevation.setText(savedInstanceState.getString(ITEM_ELEVATION, ""))
        }

        val noLocationShared = intent.getBooleanExtra(EXTRA_NO_LOCATION_SHARED, false)
        if (!noLocationShared && binding.waypointLatitude.length() < 1) {
            val waypointHelper = WaypointHelper.fromIntent(intent)
            if (waypointHelper != null) {
                binding.waypointName.setText(waypointHelper.name)
                if (waypointHelper.latitude != null) {
                    binding.waypointLatitude.setText(waypointHelper.latitude.toString())
                }
                if (waypointHelper.longitude != null) {
                    binding.waypointLongitude.setText(waypointHelper.longitude.toString())
                }
                if (waypointHelper.elevation != null) {
                    var elevation = waypointHelper.elevation
                    if (elevationInFeet) {
                        elevation = DistanceUnit.meterToFeet(elevation)
                    }
                    binding.waypointElevation.setText(elevation.toString())
                }
            }
        }

        val watcher = WaypointWatcher()
        binding.waypointName.addTextChangedListener(watcher)
        binding.waypointLatitude.addTextChangedListener(watcher)
        binding.waypointLongitude.addTextChangedListener(watcher)
        binding.waypointElevation.addTextChangedListener(watcher)
        watcher.afterTextChanged(null)

        binding.waypointSend.setOnClickListener(SendListener())
        binding.waypointPaste.setOnClickListener(ClickToPaste())

        if (!noLocationShared && binding.waypointLatitude.length() < 1) {
            noLocationFound()
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter().apply {
            addAction(GB.ACTION_SET_FINISHED)
        })
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        super.onDestroy()
    }

    fun noLocationFound() {
        MaterialAlertDialogBuilder(this)
            .setIcon(R.drawable.ic_not_listed_location)
            .setTitle(R.string.activity_send_waypoint_no_location)
            .setMessage(R.string.activity_send_waypoint_no_location_details)
            .setNeutralButton(android.R.string.ok, null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ITEM_NAME, binding.waypointName.text.toString())
        outState.putString(ITEM_LATITUDE, binding.waypointLatitude.text.toString())
        outState.putString(ITEM_LONGITUDE, binding.waypointLongitude.text.toString())
        outState.putString(ITEM_ELEVATION, binding.waypointElevation.text.toString())
    }


    inner class ClickToPaste : View.OnClickListener {
        override fun onClick(view: View?) {
            val clipboard = view?.context?.getSystemService(
                CLIPBOARD_SERVICE
            ) as ClipboardManager?

            val clipData = clipboard?.primaryClip
            if (clipData != null) {
                var index = 0
                while (index < clipData.itemCount) {
                    val item = clipData.getItemAt(index)
                    val text = item.coerceToText(view.context)
                    if (text.length > 0) {
                        val waypointHelper = WaypointHelper.fromText(text)
                        if (waypointHelper != null) {
                            binding.waypointName.setText(waypointHelper.name)
                            if (waypointHelper.latitude != null) {
                                binding.waypointLatitude.setText(waypointHelper.latitude.toString())
                            }
                            if (waypointHelper.longitude != null) {
                                binding.waypointLongitude.setText(waypointHelper.longitude.toString())
                            }
                            var elevation = waypointHelper.elevation
                            if (elevation != null) {
                                if (elevationInFeet) {
                                    elevation = DistanceUnit.meterToFeet(elevation)
                                }
                                binding.waypointElevation.setText(elevation.toString())
                            }
                            return
                        }
                    }
                    index++
                }
            }
            noLocationFound()
        }

    }

    inner class ClickToCopy : View.OnClickListener {
        override fun onClick(view: View?) {
            val textView = view as TextView?
            if (textView != null) {
                val text = textView.text
                val clipboard = view.context.getSystemService(
                    CLIPBOARD_SERVICE
                ) as ClipboardManager?
                val clip = ClipData.newPlainText(binding.waypointName.text, text)
                clipboard?.setPrimaryClip(clip)
            }
        }
    }

    inner class SendListener : View.OnClickListener {
        override fun onClick(view: View?) {
            if (binding.waypointName.error != null
                || binding.waypointLatitude.error != null
                || binding.waypointLongitude.error != null
                || binding.waypointElevation.error != null
                || binding.waypointSend.error != null
            ) {
                return
            }
            val label = binding.waypointName.text.toString()
            val latitude = binding.waypointLatitude.text.toString().toDoubleOrNull()
            val longitude = binding.waypointLongitude.text.toString().toDoubleOrNull()
            var elevation = binding.waypointElevation.text.toString().toDoubleOrNull()

            if (elevationInFeet && elevation != null) {
                elevation = DistanceUnit.feetToMeter(elevation)
            }

            if (!label.isEmpty() && latitude?.isFinite() == true && longitude?.isFinite() == true) {
                val waypoint = WaypointHelper(label, latitude, longitude, elevation)
                val fitFile = WaypointHelper.generateFitLocationFile(waypoint)

                val options = Bundle()
                options.putByteArray(BUNDLE_EXTRA_INSTALL_BYTES, fitFile.outgoingMessage)
                options.putString(BUNDLE_EXTRA_INSTALL_TASK_NAME, "send waypoint")

                if (device == null || !device!!.state.equalsOrHigherThan(GBDevice.State.INITIALIZED)) {
                    val intent = Intent(
                        this@GarminSendWaypointActivity, FileInstallerActivity::class.java
                    )
                    intent.data = waypoint.toUri()
                    intent.putExtra(EXTRA_OPTIONS, options)
                    LOG.debug("startActivity {}", intent.component)
                    this@GarminSendWaypointActivity.startActivity(intent)
                } else {
                    LOG.debug("send waypoint to device")
                    GBApplication.deviceService(device).onInstallApp(waypoint.toUri(), options)
                }
            }
        }
    }

    inner class WaypointWatcher : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            // name
            if (binding.waypointName.length() < 1) {
                binding.waypointName.error = getString(R.string.activity_send_waypoint_name_missing)
            } else {
                val name = binding.waypointName.text.toString()
                val buffer = Charsets.UTF_8.encode(name)
                val overflow = buffer.limit() - 32
                if (overflow > 0) {
                    binding.waypointName.error = resources.getQuantityString(
                        R.plurals.activity_send_waypoint_name_length, overflow, overflow
                    )
                } else {
                    binding.waypointName.error = null
                }
            }

            // latitude
            if (binding.waypointLatitude.length() < 1) {
                binding.waypointLatitude.error =
                    getString(R.string.activity_send_waypoint_latitude_missing)
            } else {
                val raw = binding.waypointLatitude.text.toString()
                val actual = raw.toDoubleOrNull()
                if (actual == null || !actual.isFinite()) {
                    binding.waypointLatitude.error =
                        getString(R.string.activity_send_waypoint_latitude_number)
                } else if (actual < -90.0 || actual > 90.0) {
                    binding.waypointLatitude.error =
                        getString(R.string.activity_send_waypoint_latitude_range)
                } else {
                    binding.waypointLatitude.error = null
                }
            }

            // longitude
            if (binding.waypointLongitude.length() < 1) {
                binding.waypointLongitude.error =
                    getString(R.string.activity_send_waypoint_longitude_missing)
            } else {
                val raw = binding.waypointLongitude.text.toString()
                val actual = raw.toDoubleOrNull()
                if (actual == null || !actual.isFinite()) {
                    binding.waypointLongitude.error =
                        getString(R.string.activity_send_waypoint_longitude_number)
                } else if (actual < -180.0 || actual > 180.0) {
                    binding.waypointLongitude.error =
                        getString(R.string.activity_send_waypoint_longitude_range)
                } else {
                    binding.waypointLongitude.error = null
                }
            }

            // elevation
            if (binding.waypointElevation.length() < 1) {
                binding.waypointElevation.error = null
            } else {
                val raw = binding.waypointElevation.text.toString()
                var actual = raw.toDoubleOrNull()
                if (actual == null || !actual.isFinite()) {
                    binding.waypointElevation.error =
                        getString(R.string.activity_send_waypoint_elevation_number)
                } else {
                    if (elevationInFeet) {
                        actual = DistanceUnit.feetToMeter(actual)
                    }

                    if (actual < -500.0 || actual > 11827.0) {
                        val stringRes = if (elevationInFeet) {
                            R.string.activity_send_waypoint_elevation_range_imperial
                        } else {
                            R.string.activity_send_waypoint_elevation_range_metric
                        }
                        binding.waypointElevation.error = getString(stringRes)
                    } else {
                        binding.waypointElevation.error = null
                    }
                }
            }

            updateWpSend()
        }

        private fun updateWpSend() {
            val latitude = binding.waypointLatitude.text.toString().toDoubleOrNull()
            val longitude = binding.waypointLongitude.text.toString().toDoubleOrNull()
            val formated = WaypointHelper.formatPosition(latitude, longitude)
            if (formated != null) {
                binding.waypointInfo0.text = formated[0]
                binding.waypointInfo1.text = formated[1]
                binding.waypointInfo2.text = formated[2]
                binding.waypointInfo3.text = formated[3]
            } else {
                binding.waypointInfo0.text = null
                binding.waypointInfo1.text = null
                binding.waypointInfo2.text = null
                binding.waypointInfo3.text = null
            }

            binding.waypointSend.isEnabled =
                (binding.waypointName.error == null) && (binding.waypointLatitude.error == null) && (binding.waypointLongitude.error == null) && (binding.waypointSend.error == null)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GarminSendWaypointActivity::class.java)

        private const val ITEM_NAME = "waypointName"
        private const val ITEM_LATITUDE = "waypointLatitude"
        private const val ITEM_LONGITUDE = "waypointLongitude"
        private const val ITEM_ELEVATION = "waypointElevation"

        const val EXTRA_NO_LOCATION_SHARED: String = "extra_no_location_shared"

        fun handlePreferenceClick(
            handler: DeviceSpecificSettingsHandler
        ): Boolean {
            val device = handler.getDevice()
            if(!device.state.equalsOrHigherThan(GBDevice.State.INITIALIZED)){
                GB.toast(
                    handler.getContext(),
                    R.string.device_not_connected,
                    Toast.LENGTH_LONG,
                    GB.ERROR
                )
             return false
            }

            val intent = Intent(handler.context, GarminSendWaypointActivity::class.java)
            intent.putExtra(GBDevice.EXTRA_DEVICE, handler.getDevice())
            intent.putExtra(EXTRA_NO_LOCATION_SHARED, true)
            handler.getContext().startActivity(intent)
            return true
        }
    }
}

