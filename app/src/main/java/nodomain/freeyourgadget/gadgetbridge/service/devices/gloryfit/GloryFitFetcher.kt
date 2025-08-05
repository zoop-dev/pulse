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
package nodomain.freeyourgadget.gadgetbridge.service.devices.gloryfit

import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.devices.GenericHeartRateSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSleepStageSampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.GenericSpo2SampleProvider
import nodomain.freeyourgadget.gadgetbridge.devices.gloryfit.GloryFitStepsSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.GenericHeartRateSample
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSleepStageSample
import nodomain.freeyourgadget.gadgetbridge.entities.GenericSpo2Sample
import nodomain.freeyourgadget.gadgetbridge.entities.GloryFitStepsSample
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils
import nodomain.freeyourgadget.gadgetbridge.util.GB
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.Collections
import java.util.GregorianCalendar
import java.util.LinkedList
import java.util.Queue

class GloryFitFetcher(val mSupport: GloryFitSupport) {
    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GloryFitFetcher::class.java)
    }

    private val mFetchQueue: Queue<GloryFitFetchType> = LinkedList()
    private var mCurrentFetch: GloryFitFetchType? = null
    private var mCurrentSleepSession: Calendar? = null
    private var mCurrentSessionAfterMidnight = false

    fun reset() {
        mFetchQueue.clear()
        mCurrentFetch = null
        mCurrentSleepSession = null
    }

    fun onFetchRecordedData(dataTypes: Int) {
        val coordinator = mSupport.device.deviceCoordinator

        if ((dataTypes and RecordedDataTypes.TYPE_ACTIVITY) != 0) {
            mFetchQueue.add(GloryFitFetchType.STEPS)
            mFetchQueue.add(GloryFitFetchType.HEART_RATE)
            mFetchQueue.add(GloryFitFetchType.SLEEP)
        }

        if ((dataTypes and RecordedDataTypes.TYPE_SPO2) != 0 && coordinator.supportsSpo2(mSupport.device)) {
            mFetchQueue.add(GloryFitFetchType.SPO2)
        }

        if (mCurrentFetch == null) {
            triggerNextFetch()
        }
    }

    fun triggerNextFetch() {
        val wasFetching = mCurrentFetch != null
        mCurrentFetch = this.mFetchQueue.poll()

        mCurrentFetch?.let {
            LOG.debug("Fetching next: {}", it)

            mSupport.device.setBusyTask(
                it.descriptionRes,
                mSupport.context
            )
            mSupport.device.sendDeviceUpdateIntent(mSupport.context)

            sendFetchCommand(it)

            return
        }

        if (wasFetching) {
            LOG.debug("All operations finished")

            GB.updateTransferNotification(null, "", false, 100, mSupport.context)
            GB.signalActivityDataFinish(mSupport.device)

            if (mSupport.device.isBusy) {
                mSupport.device.unsetBusyTask()
                mSupport.device.sendDeviceUpdateIntent(mSupport.context)
            }
        }
    }

    private fun sendFetchCommand(type: GloryFitFetchType) {
        val builder = mSupport.createTransactionBuilder("fetch $type")
        val cmd: ByteArray
        when (type) {
            GloryFitFetchType.STEPS -> {
                cmd = byteArrayOf(GloryFitSupport.CMD_STEPS, GloryFitSupport.FETCH_START)
            }

            GloryFitFetchType.HEART_RATE -> {
                cmd = byteArrayOf(GloryFitSupport.CMD_HEART_RATE, GloryFitSupport.FETCH_START)
            }

            GloryFitFetchType.SPO2 -> {
                cmd = byteArrayOf(GloryFitSupport.CMD_SPO2, GloryFitSupport.FETCH_START)
            }

            GloryFitFetchType.SLEEP -> {
                cmd = byteArrayOf(GloryFitSupport.CMD_SLEEP_INFO, 0x01)
            }
        }

        builder.write(GloryFitSupport.UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *cmd)

        builder.queue()
    }

    fun handleSleepInfo(value: ByteArray) {
        when (value[1]) {
            GloryFitSupport.SLEEP_INFO_DATE -> {
                val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                buf.get(ByteArray(2)) // discard first 2 bytes
                val timestamp = buf.getDate()
                val numStages = buf.get().toInt() and 0xff
                mCurrentSleepSession = timestamp
                mCurrentSessionAfterMidnight = false
                LOG.debug(
                    "Got sleep info date {}, expect {} stages",
                    DateTimeUtils.formatIso8601(timestamp.time),
                    numStages
                )
            }

            GloryFitSupport.SLEEP_INFO_END -> {
                LOG.debug("Got sleep info end")
                mCurrentSleepSession = null
                triggerNextFetch()
            }
        }
    }

    fun handleSleepStages(value: ByteArray) {
        mCurrentSleepSession?.let {
            LOG.debug("Got sleep stages at {}", DateTimeUtils.formatIso8601(it.time))

            if ((value.size - 1) % 6 != 0) {
                LOG.error("Unexpected sleep stages payload size {}", value.size)
                return
            }

            val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
            buf.get() // discard first byte

            val samples: MutableList<GenericSleepStageSample?> = mutableListOf()

            while (buf.position() < buf.limit()) {
                val timestamp = GregorianCalendar.getInstance()
                timestamp.timeInMillis = it.timeInMillis

                val sample = GenericSleepStageSample()

                val hour = buf.get().toInt()
                val minute = buf.get().toInt()
                val stage = buf.get().toInt()
                buf.get() // ? 1
                val duration = buf.getShort()

                if (hour > 12) {
                    // assume times after noon correspond to the previous day
                    // unless they already come after noon the next day
                    // TODO is this right?
                    if (!mCurrentSessionAfterMidnight) {
                        timestamp.add(Calendar.DATE, -1)
                    }
                } else {
                    mCurrentSessionAfterMidnight = true
                }

                timestamp.set(Calendar.HOUR_OF_DAY, hour)
                timestamp.set(Calendar.MINUTE, minute)

                sample.timestamp = timestamp.timeInMillis
                sample.stage = stage
                sample.duration = duration.toInt()

                LOG.debug("Sleep stage at {}: {} for {}", DateTimeUtils.formatIso8601(timestamp.time), stage, duration)

                samples.add(sample)
            }

            LOG.debug("Persisting {} sleep stage samples", samples.size)

            try {
                GBApplication.acquireDB().use { handler ->
                    val session = handler.getDaoSession()
                    val sampleProvider = GenericSleepStageSampleProvider(mSupport.device, session)

                    sampleProvider.persistForDevice(mSupport.context, mSupport.device, samples)
                }
            } catch (e: Exception) {
                GB.toast(mSupport.context, "Error saving sleep session samples", Toast.LENGTH_LONG, GB.ERROR, e)
            }

            return
        }

        LOG.error("Got sleep stages, but sleep session date is unknown")
    }

    fun handleSteps(value: ByteArray) {
        when {
            value.size == 3 && value[1] == GloryFitSupport.FETCH_END -> {
                LOG.debug("Got steps fetch end")
                triggerNextFetch()
            }

            value.size == 18 -> {
                val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                buf.get() // discard first bytes
                val timestamp = buf.getDate()
                timestamp.set(Calendar.HOUR_OF_DAY, buf.get().toInt() and 0xff)

                val sample = GloryFitStepsSample()
                sample.timestamp = timestamp.timeInMillis
                sample.totalSteps = buf.getShort().toInt()
                sample.runningStart = buf.get().toInt()
                sample.runningEnd = buf.get().toInt()
                buf.get() // unk
                sample.runningSteps = buf.getShort().toInt()
                sample.walkingStart = buf.get().toInt()
                sample.walkingEnd = buf.get().toInt()
                buf.get() // unk
                sample.walkingSteps = buf.getShort().toInt()

                LOG.debug("Steps {}: {}", DateTimeUtils.formatIso8601(timestamp.time), sample.totalSteps)

                try {
                    GBApplication.acquireDB().use { handler ->
                        val session = handler.getDaoSession()
                        val sampleProvider = GloryFitStepsSampleProvider(mSupport.device, session)

                        sampleProvider.persistForDevice(
                            mSupport.context,
                            mSupport.device,
                            Collections.singletonList(sample)
                        )
                    }
                } catch (e: Exception) {
                    GB.toast(mSupport.context, "Error saving steps samples", Toast.LENGTH_LONG, GB.ERROR, e)
                }
            }

            else -> LOG.warn("Unknown steps command {}", value.toHexString())
        }
    }

    fun handleHeartRate(value: ByteArray): Boolean {
        when {
            value.size == 18 && value[1] == GloryFitSupport.FETCH_DATA -> {
                // f707e90703083030315c2f2f2e2f2f353331
                // 07:40 - 53
                // 07:20 - 47
                // 07:00 - 47

                val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                buf.get() // discard first byte
                val timestamp = buf.getDate()
                timestamp.set(Calendar.HOUR_OF_DAY, buf.get().toInt() and 0xff)

                timestamp.add(Calendar.MINUTE, -10 * (buf.limit() - buf.position()) + 10)

                val samples: MutableList<GenericHeartRateSample?> = mutableListOf()

                while (buf.position() < buf.limit()) {
                    val hr = buf.get().toInt() and 0xff
                    if (hr != 0xff && hr != 0) {
                        val sample = GenericHeartRateSample()
                        sample.timestamp = timestamp.timeInMillis
                        sample.heartRate = hr
                        samples.add(sample)
                        LOG.trace("HR {}: {}", DateTimeUtils.formatIso8601(timestamp.time), hr)
                    }

                    timestamp.add(Calendar.MINUTE, 10)
                }

                LOG.debug("Persisting {} HR samples", samples.size)

                try {
                    GBApplication.acquireDB().use { handler ->
                        val session = handler.getDaoSession()
                        val sampleProvider = GenericHeartRateSampleProvider(mSupport.device, session)

                        sampleProvider.persistForDevice(mSupport.context, mSupport.device, samples)
                    }
                } catch (e: Exception) {
                    GB.toast(mSupport.context, "Error saving hr samples", Toast.LENGTH_LONG, GB.ERROR, e)
                }
            }

            value.size == 3 && value[1] == GloryFitSupport.FETCH_END -> {
                LOG.debug("Got hr fetch end")
                triggerNextFetch()
            }

            else -> return false
        }

        return true
    }

    fun handleSpO2(value: ByteArray): Boolean {
        when (value[2]) {
            GloryFitSupport.FETCH_DATA -> {
                // 34fa07e907030400ffff60ffff61ffff61ffff60
                // Data in blocks of 10 minutes
                // 04:00 - 96, 03:50 N/A
                // 03:30 - 97
                // 03:00 - 97
                // 02:30 - 96

                if (value.size != 20) {
                    LOG.error("Unexpected spo2 data length {}", value.size)
                    return true
                }

                val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                buf.get(ByteArray(2)) // discard first 2 bytes
                val timestamp = buf.getDate()
                timestamp.set(Calendar.HOUR_OF_DAY, buf.get().toInt() and 0xff)
                timestamp.set(Calendar.MINUTE, buf.get().toInt() and 0xff)

                timestamp.add(Calendar.MINUTE, -10 * (buf.limit() - buf.position()) + 10)

                val samples: MutableList<GenericSpo2Sample?> = mutableListOf()

                while (buf.position() < buf.limit()) {
                    val spo2 = buf.get().toInt() and 0xff
                    if (spo2 != 0xff && spo2 != 0) {
                        val sample = GenericSpo2Sample()
                        sample.timestamp = timestamp.timeInMillis
                        sample.spo2 = spo2
                        samples.add(sample)
                        LOG.trace("SpO2 {}: {}", DateTimeUtils.formatIso8601(timestamp.time), spo2)
                    }

                    timestamp.add(Calendar.MINUTE, 10)
                }

                LOG.debug("Persisting {} SpO2 samples", samples.size)

                try {
                    GBApplication.acquireDB().use { handler ->
                        val session = handler.getDaoSession()
                        val sampleProvider = GenericSpo2SampleProvider(mSupport.device, session)

                        sampleProvider.persistForDevice(mSupport.context, mSupport.device, samples)
                    }
                } catch (e: Exception) {
                    GB.toast(mSupport.context, "Error saving SpO2 samples", Toast.LENGTH_LONG, GB.ERROR, e)
                }
            }

            GloryFitSupport.FETCH_END -> {
                LOG.debug("Got SpO2 fetch end")
                triggerNextFetch()
            }

            else -> return false
        }

        return true
    }

    fun ByteBuffer.getDate(): Calendar {
        val timestamp = GregorianCalendar.getInstance()

        timestamp.set(Calendar.YEAR, getShort().toInt() and 0xffff)
        timestamp.set(Calendar.MONTH, (get().toInt() and 0xff) - 1)
        timestamp.set(Calendar.DATE, get().toInt() and 0xff)
        timestamp.set(Calendar.HOUR_OF_DAY, 0)
        timestamp.set(Calendar.MINUTE, 0)
        timestamp.set(Calendar.SECOND, 0)
        timestamp.set(Calendar.MILLISECOND, 0)

        return timestamp
    }
}
