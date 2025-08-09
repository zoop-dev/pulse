/*  Copyright (C) 2025 Thomas Kuehne

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

package nodomain.freeyourgadget.gadgetbridge.devices.generic_scale

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.devices.GenericWeightSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GenericWeightSample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale.WeightScaleMeasurement
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.weightScale.WeightScaleProfile
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import kotlin.math.roundToInt

class GenericWeightScaleMeasurementActivity : AbstractGBActivity() {
    private val weightUpdatedReceiver: WeightUpdatedReceiver = WeightUpdatedReceiver()
    private var actual: TextView? = null
    private var save: Button? = null
    private var device: GBDevice? = null
    private var unit: String? = null

    private var measurement: WeightScaleMeasurement? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val address = intent.getStringExtra(WeightScaleProfile.EXTRA_ADDRESS)
            ?: throw IllegalArgumentException(WeightScaleProfile.EXTRA_ADDRESS + " must not be null")

        val manager = GBApplication.app().deviceManager
        device = manager.getDeviceByAddress(address)

        val settings = GBApplication.getDevicePrefs(device)
        unit = settings.getString(DeviceSettingsPreferenceConst.PREF_WEIGHT_SCALE_UNIT, null)

        if (unit == null) {
            unit = if (GBApplication.getPrefs().isMetricUnits) "kilogram" else "pound"
        }

        setContentView(R.layout.activity_weight_scale_measurement)

        save = findViewById(R.id.weight_scale_measurement_save)
        save?.setOnClickListener(OnSaveClicked())

        actual = findViewById(R.id.weight_scale_measurement_actual)

        val filter = IntentFilter()
        filter.addAction(WeightScaleProfile.ACTION_WEIGHT_MEASUREMENT)
        ContextCompat.registerReceiver(
            applicationContext, weightUpdatedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED
        )

        displayWeightInfo()
    }

    internal fun displayWeightInfo() {
        val raw: Double? = measurement?.weightKilogram
        val kg: Double = if (raw == null || raw.isNaN()) 0.0 else raw

        if (unit.equals("jin")) {
            val jin: Double = kg * 2
            actual?.text = getString(R.string.weight_scale_jin_format, jin)
        } else if (unit.equals("pound")) {
            val pound: Double = kg / 0.45359237
            actual?.text = getString(R.string.weight_scale_pound_format, pound)
        } else if (unit.equals("stone")) {
            val total: Int = (kg / 0.45359237).roundToInt()
            val stone: Int = total / 14
            val pound: Int = total % 14
            actual?.text = getString(R.string.weight_scale_stone_format, stone, pound)
        } else {
            actual?.text = getString(R.string.weight_scale_kilogram_format, kg)
        }
    }

    internal fun saveWeightInfo(measurement: WeightScaleMeasurement) {
        LOG.debug("saveWeightInfo - {}", measurement)

        // ignore: missing mandatory information
        if (measurement.weightKilogram == null) {
            return
        }

        // fix time if missing or broken
        if (measurement.time == null) {
            measurement.time = Instant.now()
        } else {
            // older or newer than 10 minutes - the time on the scale is likely out of sync
            val tooOld = Instant.now().minusSeconds(60L * 10L)
            val tooNew = Instant.now().plusSeconds(60L * 10L)
            if (measurement.time!!.isBefore(tooOld) || measurement.time!!.isAfter(tooNew)) {
                measurement.time = Instant.now()
            }
        }

        try {
            GBApplication.acquireDB().use { db ->
                val provider = GenericWeightSampleProvider(device, db.getDaoSession())
                val userId = DBHelper.getUser(db.getDaoSession()).id
                val deviceId = DBHelper.getDevice(device, db.getDaoSession()).id

                val sample = GenericWeightSample(
                    measurement.time!!.epochSecond,
                    deviceId,
                    userId,
                    measurement.weightKilogram!!.toFloat()
                )
                provider.addSample(sample)
            }
            LOG.debug("saveWeightInfo - saved {} kg", measurement.weightKilogram)
        } catch (e: Exception) {
            LOG.error("saveWeightInfo - error", e)
        }
    }

    private inner class OnSaveClicked : View.OnClickListener {
        override fun onClick(v: View?) {
            measurement?.let {
                save?.setEnabled(false)
                saveWeightInfo(it)
            }
        }
    }

    public override fun onDestroy() {
        applicationContext.unregisterReceiver(weightUpdatedReceiver)
        super.onDestroy()
    }

    private inner class WeightUpdatedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            LOG.debug("received measurement")
            val data: WeightScaleMeasurement? =
                intent.getParcelableExtra(WeightScaleProfile.EXTRA_WEIGHT_MEASUREMENT)
            val address: String? = intent.getStringExtra(WeightScaleProfile.EXTRA_ADDRESS)
            if (device?.address?.equals(address) == true) {
                measurement = data
                displayWeightInfo()
                save?.setEnabled(measurement?.weightKilogram != null)
            }
        }
    }

    companion object {
        private val LOG: Logger =
            LoggerFactory.getLogger(GenericWeightScaleMeasurementActivity::class.java)
    }
}