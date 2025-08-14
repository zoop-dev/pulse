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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Handler
import android.os.Looper
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.database.AppSpecificNotificationSettingsRepository
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCameraRemote
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.entities.AppSpecificNotificationSetting
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser
import nodomain.freeyourgadget.gadgetbridge.model.Alarm
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec
import nodomain.freeyourgadget.gadgetbridge.model.Contact
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.notification.defines.VibrationKind
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol
import nodomain.freeyourgadget.gadgetbridge.util.MediaManager
import org.apache.commons.lang3.ArrayUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

class GloryFitSupport() : AbstractBTLESingleDeviceSupport(LOG) {
    init {
        addSupportedService(UUID_SERVICE_GLORYFIT_CMD)
        addSupportedService(UUID_SERVICE_GLORYFIT_DATA)
    }

    private var mMediaManager: MediaManager? = null
    private var mAppNotificationSettingsRepository: AppSpecificNotificationSettingsRepository? = null

    private val fetcher: GloryFitFetcher = GloryFitFetcher(this)

    private var mHandler: Handler = Handler(Looper.getMainLooper())
    private val mBatteryStateRequestRunnable: Runnable = Runnable {
        val builder = createTransactionBuilder("get battery")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(CMD_BATTERY)
        )
        builder.queue()
    }

    override fun useAutoConnect(): Boolean {
        return true
    }

    override fun setContext(gbDevice: GBDevice, btAdapter: BluetoothAdapter, context: Context) {
        super.setContext(gbDevice, btAdapter, context)
        mMediaManager = MediaManager(context)
        mAppNotificationSettingsRepository = AppSpecificNotificationSettingsRepository(gbDevice)
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        fetcher.reset()

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        builder.requestMtu(247)

        builder.notify(UUID_CHARACTERISTIC_GLORYFIT_CMD_READ, true)
        builder.notify(UUID_CHARACTERISTIC_GLORYFIT_DATA_READ, true)

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(CMD_VERSION)
        )
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(CMD_BATTERY)
        )
        if (GBApplication.getPrefs().syncTime()) {
            setTime(builder)
        }
        setUserInfo(builder)
        setGoalSteps(builder)
        setGoalCalories(builder)
        setGoalDistance(builder)
        setSedentaryReminder(builder)
        setLanguage(builder)
        setUnits(builder)
        setHeartRateMeasurementInterval(builder)
        if (device.deviceCoordinator.supportsSpo2(device)) {
            setSpo2Measurement(builder)
        }
        setEnableCallRejection(builder)
        setEnableSmsReply(builder)

        // FIXME this is probably too early
        builder.setDeviceState(GBDevice.State.INITIALIZED)

        return builder
    }

    override fun dispose() {
        synchronized(ConnectionMonitor) {
            mHandler.removeCallbacksAndMessages(null)
            super.dispose()
        }
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        when (characteristic.uuid) {
            UUID_CHARACTERISTIC_GLORYFIT_CMD_READ -> {
                handleCommand(value)
                return true
            }

            UUID_CHARACTERISTIC_GLORYFIT_DATA_READ -> {
                handleData(value)
                return true
            }

        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    private fun handleCommand(value: ByteArray) {
        when (value[0]) {
            CMD_VERSION -> {
                val versionInfoEvent = GBDeviceEventVersionInfo()
                versionInfoEvent.fwVersion = String(ArrayUtils.subarray(value, 1, value.size))
                evaluateGBDeviceEvent(versionInfoEvent)
            }

            CMD_BATTERY -> {
                val batteryInfoEvent = GBDeviceEventBatteryInfo()
                batteryInfoEvent.state = BatteryState.BATTERY_NORMAL
                batteryInfoEvent.level = value[1].toInt() and 0xff
                if (value.size > 2 && value[2].toInt() == 0x01) {
                    batteryInfoEvent.state = BatteryState.BATTERY_CHARGING
                }

                evaluateGBDeviceEvent(batteryInfoEvent)
                // It only sends proactive updates during charging
                rearmBatteryStateRequestTimer()
            }

            CMD_DATE_TIME -> {
                LOG.debug("Got date time ack")
            }

            CMD_CAMERA -> {
                val cameraEvent = GBDeviceEventCameraRemote()

                when (value[1]) {
                    CAMERA_OPEN ->
                        cameraEvent.event = GBDeviceEventCameraRemote.Event.OPEN_CAMERA

                    CAMERA_TRIGGER ->
                        cameraEvent.event = GBDeviceEventCameraRemote.Event.TAKE_PICTURE

                    CAMERA_CLOSE ->
                        cameraEvent.event = GBDeviceEventCameraRemote.Event.CLOSE_CAMERA

                    else -> {
                        LOG.warn("Unknown camera event {}", value[1].toHexString())
                        return
                    }
                }

                evaluateGBDeviceEvent(cameraEvent)
            }

            CMD_ACTION -> {
                when (value[1]) {
                    ACTION_CALL_HANGUP -> {
                        val rejectMethodPref = devicePrefs.getString(
                            DeviceSettingsPreferenceConst.PREF_CALL_REJECT_METHOD,
                            "reject"
                        )
                        var rejectMethod = GBDeviceEventCallControl.Event.REJECT
                        if (rejectMethodPref == "ignore") rejectMethod = GBDeviceEventCallControl.Event.IGNORE
                        evaluateGBDeviceEvent(GBDeviceEventCallControl(rejectMethod))

                        // Signal to the watch that the call ended, or it might get stuck indefinitely
                        val callSpec = CallSpec()
                        callSpec.command = CallSpec.CALL_END
                        onSetCallState(callSpec)
                    }

                    ACTION_CAMERA_CLOSE -> {
                        evaluateGBDeviceEvent(
                            GBDeviceEventCameraRemote(GBDeviceEventCameraRemote.Event.CLOSE_CAMERA)
                        )
                    }

                    ACTION_FIND_PHONE -> {
                        val findPhoneEvent = GBDeviceEventFindPhone()

                        if (value.size == 2) {
                            findPhoneEvent.event = GBDeviceEventFindPhone.Event.START
                            evaluateGBDeviceEvent(findPhoneEvent)
                        } else if (value.size == 4 && value[2] == 0x01.toByte() && value[3] == 0x00.toByte()) {
                            findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP
                            evaluateGBDeviceEvent(findPhoneEvent)
                        } else {
                            LOG.warn("Unhandled find phone action")
                        }
                    }

                    ACTION_MUSIC_BTN_PLAY -> {
                        evaluateGBDeviceEvent(GBDeviceEventMusicControl(GBDeviceEventMusicControl.Event.PLAYPAUSE))
                    }

                    ACTION_MUSIC_BTN_NEXT -> {
                        evaluateGBDeviceEvent(GBDeviceEventMusicControl(GBDeviceEventMusicControl.Event.NEXT))
                    }

                    ACTION_MUSIC_BTN_PREV -> {
                        evaluateGBDeviceEvent(GBDeviceEventMusicControl(GBDeviceEventMusicControl.Event.PREVIOUS))
                    }

                    ACTION_MUSIC_BTN_VOL_DOWN -> {
                        evaluateGBDeviceEvent(GBDeviceEventMusicControl(GBDeviceEventMusicControl.Event.VOLUMEDOWN))
                    }

                    ACTION_MUSIC_BTN_VOL_UP -> {
                        evaluateGBDeviceEvent(GBDeviceEventMusicControl(GBDeviceEventMusicControl.Event.VOLUMEUP))
                    }

                    else -> {
                        LOG.warn("Unknown action event event {}", value[1].toHexString())
                        return
                    }
                }
            }

            CMD_SLEEP_INFO -> {
                fetcher.handleSleepInfo(value)
            }

            CMD_STEPS -> {
                fetcher.handleSteps(value)
            }

            CMD_HEART_RATE -> {
                if (!fetcher.handleHeartRate(value)) {
                    LOG.warn("Unhandled heart rate command {}", value.toHexString())
                }
            }

            CMD_SPO2 -> {
                if (!fetcher.handleSpO2(value)) {
                    LOG.warn("Unknown SpO2 command {}", value.toHexString())
                }
            }

            else -> LOG.warn("Unhandled command on characteristic 33: {}", value.toHexString())
        }
    }

    private fun handleData(value: ByteArray) {
        when (value[0]) {
            CMD_SLEEP_STAGES -> {
                fetcher.handleSleepStages(value)
            }

            CMD_SMS_QUICK_REPLY -> {
                if (value.size > 5) {
                    val buf = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN)
                    buf.get()

                    val numberLength = buf.get().toInt() and 0xff
                    val numberBytes = ByteArray(numberLength)
                    buf.get(numberBytes)
                    val number = String(numberBytes)

                    val messageLength = buf.get().toInt() and 0xff
                    val messageBytes = ByteArray(messageLength)
                    buf.get(messageBytes)
                    val message = String(messageBytes)

                    if (number.isBlank()) {
                        LOG.warn("No number for quick reply")
                        return
                    }
                    if (message.isBlank()) {
                        LOG.warn("No message for quick reply")
                        return
                    }

                    LOG.debug("Replying to {}: {}", number, message)

                    val devEvtNotificationControl = GBDeviceEventNotificationControl()
                    devEvtNotificationControl.handle = -1
                    devEvtNotificationControl.phoneNumber = number
                    devEvtNotificationControl.reply = message
                    devEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.REPLY
                    evaluateGBDeviceEvent(devEvtNotificationControl)

                    val rejectCallCmd = GBDeviceEventCallControl(GBDeviceEventCallControl.Event.REJECT)
                    evaluateGBDeviceEvent(rejectCallCmd)

                    val builder = createTransactionBuilder("sms reply ack")
                    builder.write(
                        UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                        CMD_SMS_QUICK_REPLY,
                        SMS_QUICK_REPLY_SUCCESS
                    )
                    builder.queue()
                } else {
                    LOG.warn("Unhandled sms quick reply command")
                }
            }

            else -> LOG.warn("Unhandled command on characteristic 34: {}", value.toHexString())
        }
    }

    override fun onSendConfiguration(config: String) {
        val builder = createTransactionBuilder("set config $config")

        when (config) {
            ActivityUser.PREF_USER_HEIGHT_CM,
            ActivityUser.PREF_USER_WEIGHT_KG,
            ActivityUser.PREF_USER_DATE_OF_BIRTH,
            ActivityUser.PREF_USER_GENDER,
            DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_HIGH_THRESHOLD,
            DeviceSettingsPreferenceConst.PREF_HEARTRATE_ALERT_LOW_THRESHOLD,
            DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED -> {
                setUserInfo(builder)
            }

            ActivityUser.PREF_USER_STEPS_GOAL -> {
                setUserInfo(builder) // user info also has steps goal
                setGoalSteps(builder)
            }

            ActivityUser.PREF_USER_CALORIES_BURNT -> {
                setGoalCalories(builder)
            }

            ActivityUser.PREF_USER_DISTANCE_METERS -> {
                setGoalDistance(builder)
            }

            DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE,
            DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD,
            DeviceSettingsPreferenceConst.PREF_INACTIVITY_START,
            DeviceSettingsPreferenceConst.PREF_INACTIVITY_END,
            DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND -> {
                setSedentaryReminder(builder)
            }

            DeviceSettingsPreferenceConst.PREF_LANGUAGE -> {
                setLanguage(builder)
            }

            SettingsActivity.PREF_MEASUREMENT_SYSTEM -> {
                setUserInfo(builder) // user info also has temperature unit
                setUnits(builder)
            }

            DeviceSettingsPreferenceConst.PREF_TIMEFORMAT -> {
                setUnits(builder)
            }

            DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE -> {
                setHeartRateMeasurementInterval(builder)
            }

            DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING,
            DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_INTERVAL,
            DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_TIME,
            DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_START,
            DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_END -> {
                setSpo2Measurement(builder)
            }

            DeviceSettingsPreferenceConst.PREF_ENABLE_SMS_QUICK_REPLY -> {
                setEnableSmsReply(builder)
            }

            else -> {
                super.onSendConfiguration(config)
                return
            }
        }

        builder.queue()
    }

    override fun onFindPhone(start: Boolean) {
        val builder = createTransactionBuilder("find phone $start")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_ACTION,
                ACTION_FIND_PHONE,
                0x01,
                if (start) 0x01 else 0x00
            )
        )
        builder.queue()
    }

    override fun onFindDevice(start: Boolean) {
        if (start) {
            val buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
            buf.put(CMD_VIBRATE)
            buf.put(0x00) // ?
            buf.put(0x00) // ?
            buf.put(0x00) // ?
            buf.put(0x01) // ?
            buf.put(0x02) // count?
            buf.put(0x07) // ?
            buf.put(0x01) // ?

            val builder = createTransactionBuilder("find device")
            builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
            builder.queue()
        }
    }

    override fun onSetContacts(contacts: ArrayList<out Contact>) {
        // command + count + 0x37 + 0xfb + numberLength + nameLength + 15 + 20
        val minSize = 2 + 1 + 4 + 15 + 20
        var chunkSize = calcMaxWriteChunk(mtu)
        if (mtu < minSize) {
            LOG.warn("MTU is too low - setting contacts might fail")
            chunkSize = 244
        }

        LOG.debug("Setting {} contacts", contacts.size)

        val encodedContacts: MutableList<ByteArray> = mutableListOf()

        for (contact in contacts) {
            val numberBytes = contact.number.toByteArray()
            val nameBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateUtf16BE(contact.name, 20)

            if (numberBytes.size > 15) {
                LOG.warn("Contact number length {} too long, skipping", contact.number.length)
                continue
            }

            val buf = ByteBuffer.allocate(4 + numberBytes.size + nameBytes.size).order(ByteOrder.BIG_ENDIAN)
            buf.put(CMD_CONTACTS)
            buf.put(CONTACTS_ADD)
            buf.put(numberBytes.size.toByte())
            buf.put(nameBytes.size.toByte())
            buf.put(numberBytes)
            buf.put(nameBytes)

            encodedContacts.add(buf.array())
        }

        val builder = createTransactionBuilder("set contacts")

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
            *byteArrayOf(
                CMD_CONTACTS,
                CONTACTS_START,
                encodedContacts.size.toByte()
            )
        )

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.BIG_ENDIAN)
        var numAdded = 0
        for (contactBytes in encodedContacts) {
            if (numAdded == 0) {
                buf.put(CMD_CONTACTS)
                buf.put(CONTACTS_ADD)
                buf.put(0) // numAdded set at the end
            }

            if (buf.position() + contactBytes.size < buf.limit()) {
                buf.put(contactBytes)
                numAdded++
            } else {
                LOG.debug("Buffer limit reached, writing {} contacts", numAdded)

                buf.put(2, numAdded.toByte())
                builder.write(
                    UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
                    *buf.array()
                )
                buf.position(0)
                buf.put(ByteArray(buf.limit()), 0, buf.limit())
                buf.clear()

                buf.put(CMD_CONTACTS)
                buf.put(CONTACTS_ADD)
                buf.put(0) // numAdded set at the end
                buf.put(contactBytes)

                numAdded = 1
            }
        }

        // Send the last few
        if (numAdded > 0) {
            LOG.debug("Flushing last {} contacts", numAdded)

            buf.put(2, numAdded.toByte())
            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
                *buf.array()
            )
        }

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
            *byteArrayOf(
                CMD_CONTACTS,
                CONTACTS_END,
                0xfd.toByte()
            )
        )
        builder.queue()
    }

    override fun onSetAlarms(alarms: ArrayList<out Alarm>) {
        val builder = createTransactionBuilder("set alarms")

        for ((i, alarm) in alarms.withIndex()) {
            // Watch and Gb use a different bitmask
            var repeatMask = 0
            if ((alarm.repetition and Alarm.ALARM_MON.toInt()) != 0) repeatMask = repeatMask or ALARM_MON
            if ((alarm.repetition and Alarm.ALARM_TUE.toInt()) != 0) repeatMask = repeatMask or ALARM_TUE
            if ((alarm.repetition and Alarm.ALARM_WED.toInt()) != 0) repeatMask = repeatMask or ALARM_WED
            if ((alarm.repetition and Alarm.ALARM_THU.toInt()) != 0) repeatMask = repeatMask or ALARM_THU
            if ((alarm.repetition and Alarm.ALARM_FRI.toInt()) != 0) repeatMask = repeatMask or ALARM_FRI
            if ((alarm.repetition and Alarm.ALARM_SAT.toInt()) != 0) repeatMask = repeatMask or ALARM_SAT
            if ((alarm.repetition and Alarm.ALARM_SUN.toInt()) != 0) repeatMask = repeatMask or ALARM_SUN

            val buf = ByteBuffer.allocate(9).order(ByteOrder.BIG_ENDIAN)
            buf.put(CMD_VIBRATE)
            buf.put(repeatMask.toByte())
            buf.put(alarm.hour.toByte())
            buf.put(alarm.minute.toByte())
            if (alarm.enabled) {
                buf.put(0x02) // ?
                buf.put(0x0a) // ?
                buf.put(0x02) // ?
            } else {
                buf.put(0x00) // ?
                buf.put(0x00) // ?
                buf.put(0x00) // ?
            }
            buf.put(0x00) // ?
            buf.put((i + 1).toByte()) // alarm index, starting at 1

            builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array().clone())
        }

        builder.queue()
    }

    override fun onNotification(notificationSpec: NotificationSpec) {
        if (!devicePrefs.getBoolean("send_app_notifications", true)) {
            LOG.debug("App notifications disabled - ignoring")
            return
        }

        val senderOrTitle = notificationSpec.sender.takeIf { !it.isNullOrBlank() } ?: notificationSpec.title

        val payloadString = when {
            StringUtils.isNotBlank(senderOrTitle) && StringUtils.isNotBlank(notificationSpec.body)
                -> "${senderOrTitle}: ${notificationSpec.body}"

            StringUtils.isNotBlank(senderOrTitle) -> senderOrTitle
            StringUtils.isNotBlank(notificationSpec.body) -> notificationSpec.body

            else -> "?"
        }

        val builder = createTransactionBuilder("send notification")
        sendNotification(
            builder,
            payloadString,
            GloryFitNotificationType.fromNotificationType(notificationSpec.type)
        )

        var vibrationKind = VibrationKind.BASIC
        var vibrationCount = 1

        if (notificationSpec.sourceAppId != null) {
            val appSpecificSetting: AppSpecificNotificationSetting? =
                mAppNotificationSettingsRepository!!.getSettingsForAppId(notificationSpec.sourceAppId)

            if (appSpecificSetting != null) {
                if (appSpecificSetting.vibrationPattern != null) {
                    vibrationKind = VibrationKind.valueOf(appSpecificSetting.vibrationPattern.uppercase(Locale.ROOT))
                }

                if (appSpecificSetting.vibrationRepetition != null) {
                    vibrationCount = appSpecificSetting.vibrationRepetition.toInt()
                }
            }
        }

        if (vibrationKind != VibrationKind.NONE) {
            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                *byteArrayOf(
                    CMD_VIBRATE,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    vibrationCount.toByte(),
                    0x00,
                    0x00
                )
            )
        }

        builder.queue()
    }

    override fun onSetTime() {
        if (!GBApplication.getPrefs().syncTime()) {
            return
        }

        val builder = createTransactionBuilder("set time")
        setTime(builder)
        builder.queue()
    }

    override fun onSetCallState(callSpec: CallSpec) {
        if (callSpec.command == CallSpec.CALL_OUTGOING) {
            return
        }

        if (callSpec.command == CallSpec.CALL_INCOMING) {
            val caller = when {
                StringUtils.isNotBlank(callSpec.name) && StringUtils.isNotBlank(callSpec.number)
                    -> "${callSpec.name}: ${callSpec.number}"

                StringUtils.isNotBlank(callSpec.name) -> callSpec.name
                StringUtils.isNotBlank(callSpec.name) -> callSpec.number
                else -> "?"
            }

            val builder = createTransactionBuilder("call incoming")

            sendNotification(
                builder,
                caller,
                GloryFitNotificationType.CALL
            )

            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                *byteArrayOf(
                    CMD_VIBRATE,
                    0x00,
                    0x00,
                    0x00,
                    0x01,
                    0x0a,
                    0x02,
                    0x01
                )
            )

            val callerBytes = (callSpec.number ?: "").toByteArray()
            val buf = ByteBuffer.allocate(3 + callerBytes.size).order(ByteOrder.BIG_ENDIAN)
            buf.put(CMD_QUICK_REPLY_TARGET)
            buf.put(0xfa.toByte())
            buf.put(callerBytes.size.toByte())
            buf.put(callerBytes)
            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                *buf.array()
            )

            builder.queue()

            return
        }

        val builder = createTransactionBuilder("call end")

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_CALL_STATUS,
                CALL_END
            )
        )

        builder.queue()
    }

    override fun onSetCannedMessages(cannedMessagesSpec: CannedMessagesSpec) {
        val builder = createTransactionBuilder("set canned messages")

        for ((i, message) in cannedMessagesSpec.cannedMessages.withIndex()) {
            if (i >= device.deviceCoordinator.getCannedRepliesSlotCount(device)) {
                LOG.warn(
                    "Got {} canned messages, over the limit of {}",
                    cannedMessagesSpec.cannedMessages.size,
                    device.deviceCoordinator.getCannedRepliesSlotCount(device)
                )
                break
            }

            val messageBytes = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncate(message, 24).toByteArray()
            val buf = ByteBuffer.allocate(5 + messageBytes.size).order(ByteOrder.BIG_ENDIAN)
            buf.put(CMD_CANNED_MESSAGES)
            buf.put(CANNED_MESSAGES_SET)
            buf.put(cannedMessagesSpec.cannedMessages.size.toByte())
            buf.put(i.toByte())
            buf.put(messageBytes.size.toByte())
            buf.put(messageBytes)
            builder.write(UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE, *buf.array())
            // TODO do we need to throttle?
        }

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
            *byteArrayOf(
                CMD_CANNED_MESSAGES,
                CANNED_MESSAGES_END,
                0x00,
            )
        )

        builder.queue()
    }

    override fun onSetMusicState(stateSpec: MusicStateSpec?) {
        if (!mMediaManager!!.onSetMusicState(stateSpec)) {
            return
        }

        val stateByte: Byte = if (stateSpec?.state?.toInt() == MusicStateSpec.STATE_PLAYING) {
            MUSIC_STATE_PLAYING
        } else {
            MUSIC_STATE_PAUSED
        }

        val builder = createTransactionBuilder("set music state")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
            *byteArrayOf(
                CMD_MUSIC,
                MUSIC_STATE_2,
                stateByte,
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte(),
                0xff.toByte()
            )
        )
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_ACTION,
                ACTION_MUSIC_VOLUME,
                mMediaManager!!.phoneVolume.toByte()
            )
        )
        builder.queue()
    }

    override fun onSetMusicInfo(musicSpec: MusicSpec?) {
        if (!mMediaManager!!.onSetMusicInfo(musicSpec)) {
            return
        }

        val builder = createTransactionBuilder("set music info")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE,
            *byteArrayOf(
                CMD_MUSIC,
                MUSIC_STATE_1
            )
        )
        builder.queue()
    }

    override fun onSetPhoneVolume(volume: Float) {
        val builder = createTransactionBuilder("set phone volume")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_ACTION,
                ACTION_MUSIC_VOLUME,
                volume.toInt().toByte()
            )
        )
        builder.queue()
    }

    override fun onReset(flags: Int) {
        if ((flags and GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) != 0) {
            val builder = createTransactionBuilder("factory reset")
            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                CMD_FACTORY_RESET
            )
            builder.queue()
        }
    }

    override fun onEnableRealtimeHeartRateMeasurement(enable: Boolean) {
        // TODO onEnableRealtimeHeartRateMeasurement
    }

    override fun onEnableRealtimeSteps(enable: Boolean) {
        // TODO onEnableRealtimeSteps
    }

    override fun onSetHeartRateMeasurementInterval(seconds: Int) {
        val builder = createTransactionBuilder("set heart rate measurement interval = $seconds")
        setHeartRateMeasurementInterval(builder)
        builder.queue()
    }

    override fun onSendWeather() {
        // TODO onSendWeather
    }

    override fun onTestNewFunction() {

    }

    override fun onCameraStatusChange(
        event: GBDeviceEventCameraRemote.Event,
        filename: String?
    ) {
        val builder = createTransactionBuilder("camera status change to $event")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_CAMERA,
                when (event) {
                    GBDeviceEventCameraRemote.Event.OPEN_CAMERA -> CAMERA_OPEN
                    GBDeviceEventCameraRemote.Event.CLOSE_CAMERA -> CAMERA_CLOSE
                    else -> {
                        LOG.warn("Unknown camera status change {}", event)
                        return
                    }
                }
            )
        )
        builder.queue()
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        fetcher.onFetchRecordedData(dataTypes)
    }

    private fun setTime(builder: TransactionBuilder) {
        LOG.debug("Setting time")

        val timestamp = GregorianCalendar.getInstance()
        val buf = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_DATE_TIME)
        buf.putShort(timestamp.get(Calendar.YEAR).toShort())
        buf.put((timestamp.get(Calendar.MONTH) + 1).toByte())
        buf.put(timestamp.get(Calendar.DATE).toByte())
        buf.put(timestamp.get(Calendar.HOUR_OF_DAY).toByte())
        buf.put(timestamp.get(Calendar.MINUTE).toByte())
        buf.put(timestamp.get(Calendar.SECOND).toByte())
        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    private fun setUserInfo(builder: TransactionBuilder) {
        LOG.debug("Setting user info")

        val activityUser = ActivityUser()
        val devicePrefs = getDevicePrefs()

        val raiseHandToActivateDisplay = devicePrefs.getBoolean(
            DeviceSettingsPreferenceConst.PREF_LIFTWRIST_NOSHED,
            false
        )

        val heartRateAlertHigh = if (devicePrefs.heartRateHighThreshold > 0) {
            devicePrefs.heartRateHighThreshold.coerceIn(100, 200).toByte()
        } else {
            0xff.toByte()
        }

        val heartRateAlertLow = if (devicePrefs.heartRateLowThreshold > 0) {
            devicePrefs.heartRateLowThreshold.coerceIn(40, 100).toByte()
        } else {
            0x00.toByte()
        }

        val measurementSystem =
            GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric")

        val buf = ByteBuffer.allocate(19).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_USER_INFO)
        buf.putShort(activityUser.heightCm.coerceIn(91, 241).toShort())
        buf.putShort(activityUser.weightKg.coerceIn(20, 255).toShort())
        buf.put(0x05) // ?
        buf.put(0x00) // ?
        buf.put(0x00) // ?
        buf.putShort(((activityUser.stepsGoal / 1000.0).roundToInt() * 1000).coerceIn(1000, 30000).toShort())
        buf.put(if (raiseHandToActivateDisplay) 0x01 else 0x00)
        buf.put(heartRateAlertHigh)
        buf.put(0x00) // ?
        buf.put(activityUser.age.coerceIn(3, 100).toByte())
        buf.put(
            when (activityUser.gender) {
                ActivityUser.GENDER_MALE -> 0x01
                ActivityUser.GENDER_FEMALE -> 0x02
                else -> 0x01 // other?
            }
        )
        buf.put(0x00) // ?
        buf.put(if (measurementSystem == "metric") 0x02 else 0x01) // 0x02 celsius, 0x01 fahrenheit
        buf.put(0x01) // ?
        buf.put(heartRateAlertLow)

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    fun setGoalSteps(builder: TransactionBuilder) {
        val activityUser = ActivityUser()

        val stepsCoerced = ((activityUser.stepsGoal / 1000.0).roundToInt() * 1000).coerceIn(1000, 30000).toShort()

        LOG.debug("Setting steps goal to {}", stepsCoerced)

        val buf = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_GOALS)
        buf.put(GOAL_STEPS)
        buf.put(0x01) // ?
        buf.put(0x00) // ?
        buf.put(0x00) // ?
        buf.putShort(stepsCoerced)

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    fun setGoalCalories(builder: TransactionBuilder) {
        val activityUser = ActivityUser()

        val caloriesCoerced = ((activityUser.caloriesBurntGoal / 50.0).roundToInt() * 50).coerceIn(50, 1000).toShort()

        LOG.debug("Setting calories goal to {}", caloriesCoerced)

        val buf = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_GOALS)
        buf.put(GOAL_CALORIES)
        buf.put(0x01) // ?
        buf.putShort(caloriesCoerced)

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    fun setGoalDistance(builder: TransactionBuilder) {
        val activityUser = ActivityUser()

        val distanceCoerced =
            ((activityUser.distanceGoalMeters / 1000f).roundToInt() * 1000).coerceIn(1000, 20000).toShort()

        LOG.debug("Setting distance goal to {}", distanceCoerced)

        val buf = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_GOALS)
        buf.put(GOAL_DISTANCE)
        buf.put(0x01) // ?
        buf.put(0x00) // ?
        buf.put(0x00) // ?
        buf.putShort(distanceCoerced)

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    private fun setSedentaryReminder(builder: TransactionBuilder) {
        val devicePrefs = getDevicePrefs()

        val enabled = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_ENABLE, false)
        val startTime = devicePrefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_INACTIVITY_START, "06:00")
        val endTime = devicePrefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_INACTIVITY_END, "22:00")
        val duration = devicePrefs.getInt(DeviceSettingsPreferenceConst.PREF_INACTIVITY_THRESHOLD, 60)
        val lunchBreak = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_INACTIVITY_DND, false)

        LOG.debug(
            "Setting sedentary reminder, enabled={} startTime={} endTime={} duration={} lunchBreak={}",
            enabled, startTime, endTime, duration, lunchBreak
        )

        val buf = ByteBuffer.allocate(12).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_SEDENTARY_REMINDER)
        buf.put(if (enabled) 0x01 else 0x00)
        buf.put(((duration / 5.0).roundToInt() * 5).coerceIn(30, 180).toByte())
        buf.put(0x02) // ?
        buf.put(0x03) // ?
        buf.put(0x01) // ?
        buf.put(0x00) // ?
        if (endTime.isAfter(startTime)) {
            buf.put(startTime.hour.toByte())
            buf.put(startTime.minute.toByte())
            buf.put(endTime.hour.toByte())
            buf.put(endTime.minute.toByte())
        } else {
            buf.put(endTime.hour.toByte())
            buf.put(endTime.minute.toByte())
            buf.put(startTime.hour.toByte())
            buf.put(startTime.minute.toByte())
        }
        buf.put(if (lunchBreak) 0x01 else 0x00) // 12:00 - 14:00 hardcoded

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    private fun setLanguage(builder: TransactionBuilder) {
        var localeString: String = devicePrefs.getString("language", "auto")
        if (StringUtils.isBlank(localeString) || localeString == "auto") {
            val language = Locale.getDefault().language
            val country = Locale.getDefault().country

            localeString = language + "_" + country.uppercase(Locale.getDefault())
        }

        val language = GloryFitLanguage.fromLocale(localeString) ?: run {
            LOG.warn("Unable to map language for {}, falling back to english", localeString)
            GloryFitLanguage.ENGLISH
        }

        LOG.debug("Setting language to {} -> {}", localeString, language)

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_LANGUAGE,
                LANGUAGE_SET,
                language.code
            )
        )
    }

    private fun setUnits(builder: TransactionBuilder) {
        val measurementSystem =
            GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, "metric")
        val devicePrefs = getDevicePrefs()

        val metric = measurementSystem == "metric"
        val timeFormat24h = DeviceSettingsPreferenceConst.PREF_TIMEFORMAT_24H == devicePrefs.timeFormat

        LOG.debug("Setting units metric={} 24h={}", metric, timeFormat24h)

        val buf = ByteBuffer.allocate(3).order(ByteOrder.BIG_ENDIAN)
        buf.put(CMD_UNITS)
        buf.put(if (metric) 0x01 else 0x02)
        buf.put(if (timeFormat24h) 0x01 else 0x02)

        builder.write(UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE, *buf.array())
    }

    private fun setHeartRateMeasurementInterval(builder: TransactionBuilder) {
        val enabled = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HEARTRATE_AUTOMATIC_ENABLE, false)
        LOG.debug("Setting heart rate measurement enabled = {}", enabled)
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_HEART_RATE,
                if (enabled) HEART_RATE_MEASURING_ON else HEART_RATE_MEASURING_OFF
            )
        )
    }

    private fun setSpo2Measurement(builder: TransactionBuilder) {
        val enabled = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SPO2_ALL_DAY_MONITORING, false)
        val intervalSeconds = devicePrefs.getInt(DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_INTERVAL, 600)
        val restrictTime = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_TIME, false)
        val startTime = devicePrefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_START, "06:00")
        val endTime = devicePrefs.getLocalTime(DeviceSettingsPreferenceConst.PREF_SPO2_MEASUREMENT_END, "22:00")

        LOG.debug(
            "Setting sedentary reminder, enabled={} intervalSeconds={} restrictTime={} startTime={} endTime={}",
            enabled, intervalSeconds, restrictTime, startTime, endTime
        )

        val bufEnabled = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
        bufEnabled.put(CMD_SPO2)
        bufEnabled.put(SPO2_MONITORING_ENABLED)
        bufEnabled.put(if (enabled) 0x01 else 0x00)
        bufEnabled.putShort((intervalSeconds / 60).toShort())

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *bufEnabled.array()
        )

        val bufTimeInterval: ByteBuffer = ByteBuffer.allocate(7).order(ByteOrder.BIG_ENDIAN)
        bufTimeInterval.put(CMD_SPO2)
        bufTimeInterval.put(SPO2_MONITORING_TIME)
        bufTimeInterval.put(if (restrictTime) 0x01 else 0x00)
        bufTimeInterval.put(startTime.hour.toByte())
        bufTimeInterval.put(startTime.minute.toByte())
        bufTimeInterval.put(endTime.hour.toByte())
        bufTimeInterval.put(endTime.minute.toByte())

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *bufTimeInterval.array()
        )
    }

    private fun setEnableCallRejection(builder: TransactionBuilder) {
        // In the official app, this is configurable
        // but if we disable it, the watch shows the button anyway and freezes?
        LOG.debug("Setting enable call reject button")
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_CALL_REJECT_WITH_BUTTON,
                0x08, // 0x00 for disabled
                0x16,
                0x00,
                0x08,
                0x00,
                0x01,
            )
        )
    }

    private fun setEnableSmsReply(builder: TransactionBuilder) {
        val enabled = devicePrefs.getBoolean(DeviceSettingsPreferenceConst.PREF_ENABLE_SMS_QUICK_REPLY, true)
        LOG.debug("Setting enable sms reply = {}", enabled)
        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(
                CMD_SMS_QUICK_REPLY,
                if (enabled) 0x01 else 0x00,
            )
        )
    }

    private fun rearmBatteryStateRequestTimer() {
        mHandler.removeCallbacks(mBatteryStateRequestRunnable)
        val devicePrefs = getDevicePrefs()
        if (devicePrefs.batteryPollingEnabled) {
            LOG.debug("Rearming battery state request timer")
            mHandler.postDelayed(
                mBatteryStateRequestRunnable,
                devicePrefs.batteryPollingIntervalMinutes * 60 * 1000L
            )
        }
    }

    private fun sendNotification(
        builder: TransactionBuilder,
        content: String,
        type: GloryFitNotificationType
    ) {
        // Split into chunks. We might be able to send this all in a single chunk that fits the MTU?
        // official app send to send in chunks of 20 bytes though

        val truncatedPayload = nodomain.freeyourgadget.gadgetbridge.util.StringUtils.truncateUtf16BE(content, 240)
        var byteIdx = 0
        var chunkIdx = 0
        val buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)

        while (byteIdx < truncatedPayload.size) {
            buf.clear()
            buf.put(CMD_NOTIFICATION)
            buf.put(chunkIdx.toByte())
            if (chunkIdx == 0) {
                buf.put(type.code)
                buf.put(truncatedPayload.size.toByte())
            }
            val endPos = truncatedPayload.size.coerceAtMost(byteIdx + buf.limit() - buf.position())
            buf.put(
                truncatedPayload.copyOfRange(byteIdx, endPos)
            )
            builder.write(
                UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
                *buf.array().copyOfRange(0, buf.position())
            )
            byteIdx = endPos
            chunkIdx++
        }

        LOG.debug("Sending notification in {} chunks", chunkIdx)

        builder.write(
            UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE,
            *byteArrayOf(CMD_NOTIFICATION, 0xfd.toByte())
        )
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(GloryFitSupport::class.java)

        val UUID_SERVICE_GLORYFIT_CMD: UUID = UUID.fromString("000055ff-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_GLORYFIT_CMD_WRITE: UUID = UUID.fromString("000033f1-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_GLORYFIT_CMD_READ: UUID = UUID.fromString("000033f2-0000-1000-8000-00805f9b34fb")

        val UUID_SERVICE_GLORYFIT_DATA: UUID = UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_GLORYFIT_DATA_WRITE: UUID = UUID.fromString("000034f1-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_GLORYFIT_DATA_READ: UUID = UUID.fromString("000034f2-0000-1000-8000-00805f9b34fb")

        const val CMD_VERSION: Byte = 0xa1.toByte()
        const val CMD_BATTERY: Byte = 0xa2.toByte()
        const val CMD_DATE_TIME: Byte = 0xa3.toByte()
        const val CMD_CAMERA: Byte = 0xc4.toByte()
        const val CAMERA_OPEN: Byte = 0x01.toByte()
        const val CAMERA_TRIGGER: Byte = 0x02.toByte()
        const val CAMERA_CLOSE: Byte = 0x03.toByte()
        const val CMD_CANNED_MESSAGES: Byte = 0x46.toByte()
        const val CANNED_MESSAGES_SET: Byte = 0xfa.toByte()
        const val CANNED_MESSAGES_END: Byte = 0xfd.toByte()
        const val CMD_NOTIFICATION: Byte = 0xc5.toByte()
        const val CMD_SMS_QUICK_REPLY: Byte = 0x52.toByte()
        const val SMS_QUICK_REPLY_SUCCESS: Byte = 0xfe.toByte()
        const val CMD_CALL_REJECT_WITH_BUTTON: Byte = 0xd7.toByte()
        const val CMD_STEPS: Byte = 0xb2.toByte()
        const val FETCH_START: Byte = 0xfa.toByte()
        const val FETCH_DATA: Byte = 0x07.toByte()
        const val FETCH_END: Byte = 0xfd.toByte()
        const val CMD_HEART_RATE: Byte = 0xf7.toByte()
        const val HEART_RATE_MEASURING_ON: Byte = 0x01.toByte()
        const val HEART_RATE_MEASURING_OFF: Byte = 0x02.toByte()
        const val CMD_SPO2: Byte = 0x34.toByte()
        const val SPO2_MONITORING_ENABLED: Byte = 0x03.toByte()
        const val SPO2_MONITORING_TIME: Byte = 0x04.toByte()
        const val CMD_SLEEP_INFO: Byte = 0x31.toByte()
        const val SLEEP_INFO_DATE: Byte = 0x01.toByte()
        const val SLEEP_INFO_END: Byte = 0x02.toByte()
        const val CMD_SLEEP_STAGES: Byte = 0x32.toByte()
        const val CMD_ACTION: Byte = 0xd1.toByte()
        const val ACTION_CAMERA_CLOSE: Byte = 0x0f.toByte()
        const val ACTION_FIND_PHONE: Byte = 0x0a.toByte()
        const val ACTION_CALL_HANGUP: Byte = 0x02.toByte()
        const val ACTION_MUSIC_BTN_PLAY: Byte = 0x07.toByte()
        const val ACTION_MUSIC_BTN_NEXT: Byte = 0x08.toByte()
        const val ACTION_MUSIC_BTN_PREV: Byte = 0x09.toByte()
        const val ACTION_MUSIC_BTN_VOL_DOWN: Byte = 0x0e.toByte()
        const val ACTION_MUSIC_BTN_VOL_UP: Byte = 0x0d.toByte()
        const val ACTION_MUSIC_VOLUME: Byte = 0x0d.toByte()
        const val CMD_USER_INFO: Byte = 0xa9.toByte()
        const val CMD_UNITS: Byte = 0xa0.toByte()
        const val CMD_SEDENTARY_REMINDER: Byte = 0xd3.toByte()
        const val CMD_GOALS: Byte = 0x3f.toByte()
        const val GOAL_CALORIES: Byte = 0x03.toByte()
        const val GOAL_DISTANCE: Byte = 0x05.toByte()
        const val GOAL_STEPS: Byte = 0x04.toByte()
        const val CMD_LANGUAGE: Byte = 0xaf.toByte()
        const val LANGUAGE_LIST: Byte = 0xaa.toByte()
        const val LANGUAGE_SET: Byte = 0xab.toByte()
        const val CMD_VIBRATE: Byte = 0xab.toByte()
        const val CMD_CALL_STATUS: Byte = 0xc1.toByte()
        const val CMD_QUICK_REPLY_TARGET: Byte = 0x45.toByte()
        const val CALL_END: Byte = 0x04.toByte()
        const val ALARM_MON: Int = (1 shl 1)
        const val ALARM_TUE: Int = (1 shl 2)
        const val ALARM_WED: Int = (1 shl 3)
        const val ALARM_THU: Int = (1 shl 4)
        const val ALARM_FRI: Int = (1 shl 5)
        const val ALARM_SAT: Int = (1 shl 6)
        const val ALARM_SUN: Int = (1 shl 0)
        const val CMD_MUSIC: Byte = 0x3a.toByte()
        const val MUSIC_STATE_1: Byte = 0xfa.toByte()
        const val MUSIC_STATE_2: Byte = 0x01.toByte()
        const val MUSIC_STATE_PLAYING: Byte = 0x02.toByte()
        const val MUSIC_STATE_PAUSED: Byte = 0x01.toByte()
        const val CMD_CONTACTS: Byte = 0x37.toByte()
        const val CONTACTS_START: Byte = 0xfa.toByte()
        const val CONTACTS_ADD: Byte = 0xfb.toByte()
        const val CONTACTS_END: Byte = 0xfc.toByte()
        const val CMD_FACTORY_RESET: Byte = 0xad.toByte()
    }
}
