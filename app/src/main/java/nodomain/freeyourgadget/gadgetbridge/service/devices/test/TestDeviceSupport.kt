/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.test

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.os.Handler
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceCoordinator
import nodomain.freeyourgadget.gadgetbridge.devices.test.TestFeature
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.notifications.GBProgressNotification
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class TestDeviceSupport : AbstractDeviceSupport() {
    private lateinit var progressNotification: GBProgressNotification
    private val handler = Handler()

    override fun setContext(gbDevice: GBDevice, btAdapter: BluetoothAdapter, context: Context) {
        super.setContext(gbDevice, btAdapter, context)
        progressNotification = GBProgressNotification(context, GB.NOTIFICATION_CHANNEL_ID_TRANSFER)
    }

    override fun connect(): Boolean {
        LOG.info("Connecting")

        device.setUpdateState(GBDevice.State.CONNECTING, context)

        handler.postDelayed({
            LOG.info("Initialized")
            device.firmwareVersion = "1.0.0"
            device.firmwareVersion2 = "N/A"
            device.model = "0.1.7"

            if (this.coordinator.supportsLedColor(device)) {
                device.setExtraInfo("led_color", 0xff3061e3.toInt())
            } else {
                device.setExtraInfo("led_color", null)
            }

            if (this.coordinator.supports(device, TestFeature.FM_FREQUENCY)) {
                device.setExtraInfo("fm_frequency", 90.2f)
            } else {
                device.setExtraInfo("fm_frequency", null)
            }

            // TODO battery percentages
            // TODO hr measurements
            // TODO app list
            // TODO screenshots

            device.setUpdateState(GBDevice.State.INITIALIZED, context)
        }, 1000)

        return true
    }

    override fun dispose() {
        handler.removeCallbacksAndMessages(null)
    }

    override fun useAutoConnect(): Boolean {
        return false
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        LOG.debug("onFetchRecordedData - simulating progress")

        progressNotification.start(R.string.busy_task_fetch_activity_data, 0)
        device.setBusyTask(R.string.busy_task_fetch_activity_data, context)
        device.sendDeviceUpdateIntent(context)
        progressNotification.setTotalSize(100)
        handler.postDelayed({ progressNotification.setChunkProgress(20) }, 1000L)
        handler.postDelayed({ progressNotification.setChunkProgress(40) }, 2000L)
        handler.postDelayed({ progressNotification.setTotalProgress(60) }, 3000L)
        handler.postDelayed({ progressNotification.setTotalProgress(80) }, 4000L)
        handler.postDelayed({
            progressNotification.finish()
            device.unsetBusyTask()
            device.sendDeviceUpdateIntent(context)
        }, 5000L)
    }

    protected val coordinator: TestDeviceCoordinator
        get() = device.deviceCoordinator as TestDeviceCoordinator

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(TestDeviceSupport::class.java)
    }
}
