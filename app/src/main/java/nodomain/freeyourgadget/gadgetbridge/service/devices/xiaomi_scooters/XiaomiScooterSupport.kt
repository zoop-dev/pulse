package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import android.widget.Toast
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.entities.Device
import nodomain.freeyourgadget.gadgetbridge.entities.User
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import org.slf4j.LoggerFactory
import java.security.KeyPair
import java.util.ArrayDeque
import java.util.Date
import java.util.UUID

/**
 * Support for the Xiaomi Scooter 5 Max, and any other Xiaomi scooter using the same authentication
 * (ECDH-P256 + HKDF-SHA256 + AES-128-CCM, see [XiaomiScooterCrypto]), and the TLV property protocol (see
 * [XiaomiScooterProtocol]). All commands, settings, and telemetry are wrapped in a flow-control transport
 * (see [XiaomiScooterTransport]).
 */
class XiaomiScooterSupport : AbstractBTLESingleDeviceSupport(LOG) {
    // Authentication
    private var authStep = AuthStep.EXPECT_PRE_KEY_HEADER
    private lateinit var ephemeralKeyPair: KeyPair
    private var devicePublicKeyRaw: ByteArray? = null
    private var pendingLoginToken: ByteArray? = null
    private var session: XiaomiScooterCrypto.Session? = null

    // Outgoing property-protocol messages are serialized: each one needs its own SEND/RCV_RDY/
    // RCV_OK handshake on CMD before the next can be sent.
    private val outgoingQueue = ArrayDeque<ByteArray>()
    private var cmdBusy = false
    private var pendingCmdFrame: ByteArray? = null
    private var txn = 1

    // Reassembly state for a multi-frame RPT push (device writes SEND(type=0, frameCount=N>1),
    // then N indexed data frames whose payloads we concatenate before decrypting).
    private var pendingReportFrameCount = 1
    private val pendingReportPayloads = mutableListOf<ByteArray>()

    // Ride-history entries (last_ride_1..5) trickle in as separate GET_RSP entries; buffered here
    private val rideHistoryRaw = mutableMapOf<Int, String>()

    init {
        addSupportedService(UUID_SERVICE_XIAOMI)
    }

    override fun useAutoConnect(): Boolean = true

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        authStep = AuthStep.EXPECT_PRE_KEY_HEADER
        ephemeralKeyPair = XiaomiScooterCrypto.generateEphemeralKeyPair()
        devicePublicKeyRaw = null
        pendingLoginToken = null
        session = null
        outgoingQueue.clear()
        cmdBusy = false
        pendingCmdFrame = null
        txn = 1
        pendingReportFrameCount = 1
        pendingReportPayloads.clear()
        rideHistoryRaw.clear()

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        builder.requestMtu(247)

        builder.notify(UUID_CHARACTERISTIC_AUTH_CTRL, true)
        builder.notify(UUID_CHARACTERISTIC_AUTH_DATA, true)
        builder.notify(UUID_CHARACTERISTIC_COMMAND, true)
        builder.notify(UUID_CHARACTERISTIC_REPORT, true)

        builder.write(UUID_CHARACTERISTIC_AUTH_CTRL, *AUTH_START)

        // Initialization finishes asynchronously, once the login handshake completes with
        // LOGIN_OK -- see onLoginOk().

        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
    ): Boolean {
        when (characteristic.uuid) {
            UUID_CHARACTERISTIC_AUTH_CTRL -> {
                handleAuthCtrl(value)
                return true
            }

            UUID_CHARACTERISTIC_AUTH_DATA -> {
                handleAuthData(value)
                return true
            }

            UUID_CHARACTERISTIC_COMMAND -> {
                handleCommandAck(value)
                return true
            }

            UUID_CHARACTERISTIC_REPORT -> {
                handleReport(value)
                return true
            }

            UUID_CHARACTERISTIC_DEVICE_INFO -> {
                LOG.debug("Unhandled device info: {}", value.toHexString())
                return true
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    // =======================================================================
    // Login handshake
    // =======================================================================

    private fun handleAuthCtrl(value: ByteArray) {
        if (authStep == AuthStep.EXPECT_LOGIN_OK && value.contentEquals(LOGIN_OK)) {
            onLoginOk()
        } else if (value.contentEquals(LOGIN_FAIL)) {
            LOG.error("Authentication failed, disconnecting")
            GB.toast(context, R.string.authentication_failed_check_key, Toast.LENGTH_LONG, GB.WARN)
            GBApplication.deviceService(device).disconnect()
        } else {
            LOG.warn("Unexpected AUTH_CTRL frame in step {}: {}", authStep, GB.hexdump(value))
        }
    }

    private fun handleAuthData(value: ByteArray) {
        when (val frame = XiaomiScooterTransport.parse(value)) {
            is XiaomiScooterTransport.ParsedFrame.KeyExchange -> {
                if (frame.fromDevice && authStep == AuthStep.EXPECT_PRE_KEY_HEADER) {
                    LOG.debug("Got pre-key header, replaying response")
                    write(UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.replayKeyExchangePdu(value))
                    authStep = AuthStep.EXPECT_PRE_KEY_DATA
                } else if (frame.fromDevice && authStep == AuthStep.EXPECT_PRE_KEY_DATA) {
                    LOG.debug("Got pre-key data, starting login")
                    write(UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.replayKeyExchangePdu(value))
                    beginPublicKeyExchange()
                } else {
                    LOG.warn("Unexpected key-exchange PDU in step {}", authStep)
                }
            }

            is XiaomiScooterTransport.ParsedFrame.RcvRdy -> when (authStep) {
                AuthStep.EXPECT_APP_PUBKEY_RDY -> {
                    LOG.debug("Got RCV_RDY, sending public key")
                    write(
                        UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.buildDataFrame(
                            XiaomiScooterCrypto.publicKeyToRaw(ephemeralKeyPair.public)
                        )
                    )
                    authStep = AuthStep.EXPECT_APP_PUBKEY_OK
                }

                AuthStep.EXPECT_TOKEN_RDY -> {
                    val token = pendingLoginToken
                    if (token == null) {
                        LOG.error("Got RCV_RDY on {}, but login token is null - this should never happen", authStep)
                        return
                    }
                    LOG.debug("Got RCV_RDY, sending login token")
                    write(UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.buildDataFrame(token))
                    authStep = AuthStep.EXPECT_TOKEN_OK
                }

                else -> LOG.warn("Unexpected RCV_RDY in auth step {}", authStep)
            }

            is XiaomiScooterTransport.ParsedFrame.RcvOk -> when (authStep) {
                AuthStep.EXPECT_APP_PUBKEY_OK -> authStep = AuthStep.EXPECT_DEVICE_PUBKEY
                AuthStep.EXPECT_TOKEN_OK -> authStep = AuthStep.EXPECT_LOGIN_OK
                else -> LOG.warn("Unexpected RCV_OK in auth step {}", authStep)
            }

            is XiaomiScooterTransport.ParsedFrame.Data -> {
                if (frame.type == XiaomiScooterTransport.TYPE_DEV_PUBKEY && authStep == AuthStep.EXPECT_DEVICE_PUBKEY) {
                    onDevicePublicKey(frame.payload)
                } else {
                    LOG.warn(
                        "Unexpected AUTH_DATA data frame (type=0x{}) in step {}",
                        Integer.toHexString(frame.type),
                        authStep
                    )
                }
            }

            else -> LOG.warn("Unhandled AUTH_DATA frame in auth step {}: {}", authStep, frame)
        }
    }

    private fun beginPublicKeyExchange() {
        write(UUID_CHARACTERISTIC_AUTH_CTRL, LOGIN_START)
        write(UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.buildSend(XiaomiScooterTransport.TYPE_DEV_PUBKEY))
        authStep = AuthStep.EXPECT_APP_PUBKEY_RDY
    }

    private fun onDevicePublicKey(devicePubKey: ByteArray) {
        LOG.debug("Got device public key, preparing to send login token")

        devicePublicKeyRaw = devicePubKey
        write(UUID_CHARACTERISTIC_AUTH_DATA, XiaomiScooterTransport.ACK)

        val authKey = getAuthKey()
        if (authKey == null) {
            LOG.error("No auth key configured, cannot log in")
            return
        }

        val sharedSecret = XiaomiScooterCrypto.sharedSecret(ephemeralKeyPair.private, devicePubKey)
        val sessionKeys = XiaomiScooterCrypto.deriveSessionKeys(sharedSecret, authKey)

        LOG.debug(
            "Derived session keys devKey={} appKey={} devIv={} appIv={}",
            GB.hexdump(sessionKeys.devKey),
            GB.hexdump(sessionKeys.appKey),
            GB.hexdump(sessionKeys.devIv),
            GB.hexdump(sessionKeys.appIv)
        )

        session = XiaomiScooterCrypto.Session(sessionKeys)
        pendingLoginToken = XiaomiScooterCrypto.buildLoginConfirmationToken(sessionKeys.appKey, devicePubKey)

        write(
            UUID_CHARACTERISTIC_AUTH_DATA,
            XiaomiScooterTransport.buildSend(XiaomiScooterTransport.TYPE_DEV_LOGIN_INFO)
        )
        authStep = AuthStep.EXPECT_TOKEN_RDY
    }

    private fun onLoginOk() {
        LOG.info("Login successful")
        authStep = AuthStep.DONE

        enqueueMessage(XiaomiScooterProtocol.encodeHello(nextTxn()))
        subscribeTelemetry()
        requestInitialProperties()
        requestRideHistory()

        gbDevice.setUpdateState(GBDevice.State.INITIALIZED, context)
    }

    private fun getAuthKey(): ByteArray? {
        val prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address)
        val raw = prefs?.getString(DeviceSettingsPreferenceConst.PREF_AUTH_KEY, "")?.trim() ?: return null
        val hex = if (raw.startsWith("0x")) raw.substring(2) else raw
        if (hex.length != 64) {
            LOG.error("Auth key must be 64 hex chars (32 bytes), got {}", hex.length)
            return null
        }
        return runCatching { GB.hexStringToByteArray(hex) }.getOrNull()
    }

    // =======================================================================
    // Outgoing property-protocol messages (CMD channel)
    // =======================================================================

    private fun nextTxn(): Int {
        val current = txn
        txn = (txn + 1) and 0xffff
        return current
    }

    private fun enqueueMessage(message: ByteArray) {
        outgoingQueue.addLast(message)
        if (!cmdBusy) {
            sendNextQueuedMessage()
        }
    }

    private fun sendNextQueuedMessage() {
        val message = outgoingQueue.pollFirst()
        if (message == null) {
            LOG.debug("Got no more queued messages")
            cmdBusy = false
            return
        }
        LOG.debug("Sending next queued message")
        val activeSession = session
        if (activeSession == null) {
            LOG.warn("Dropping outgoing message, no active session")
            sendNextQueuedMessage()
            return
        }
        cmdBusy = true

        val frame = activeSession.encryptForDevice(message)
        val counterBytes = BLETypeConversions.fromUint16(frame.wireCounter)
        pendingCmdFrame = XiaomiScooterTransport.buildDataFrame(counterBytes + frame.ciphertext)

        write(UUID_CHARACTERISTIC_COMMAND, XiaomiScooterTransport.buildSend(XiaomiScooterTransport.TYPE_APP_MESSAGE))
    }

    private fun handleCommandAck(value: ByteArray) {
        when (XiaomiScooterTransport.parse(value)) {
            is XiaomiScooterTransport.ParsedFrame.RcvRdy -> {
                val payload = pendingCmdFrame
                if (payload == null) {
                    LOG.error("Got RCV_RDY, but no pending frame")
                    return
                }
                LOG.debug("Got RCV_RDY, sending frame")
                write(UUID_CHARACTERISTIC_COMMAND, payload)
            }

            is XiaomiScooterTransport.ParsedFrame.RcvOk -> {
                LOG.debug("Got RCV_OK")
                pendingCmdFrame = null
                sendNextQueuedMessage()
            }

            else -> LOG.warn("Unexpected CMD frame: {}", GB.hexdump(value))
        }
    }

    private fun subscribeTelemetry() {
        LOG.debug("Subscribing telemetry")
        val entries = XiaomiScooterProperties.NOTIFY_SUBSCRIBE_CODES.map {
            XiaomiScooterProtocol.SetEntry(it, XiaomiScooterProtocol.TYPE_BOOL, byteArrayOf(1))
        }
        enqueueMessage(XiaomiScooterProtocol.encodeSet(nextTxn(), entries))
    }

    private fun requestInitialProperties() {
        LOG.debug("Requesting initial properties")
        enqueueMessage(XiaomiScooterProtocol.encodeGet(nextTxn(), XiaomiScooterProperties.INITIAL_GET_CODES))
    }

    private fun requestRideHistory() {
        LOG.debug("Requesting ride history")
        enqueueMessage(XiaomiScooterProtocol.encodeGet(nextTxn(), XiaomiScooterProperties.RIDE_HISTORY_CODES))
    }

    // =======================================================================
    // Incoming telemetry (RPT channel)
    // =======================================================================

    private fun handleReport(value: ByteArray) {
        when (val frame = XiaomiScooterTransport.parse(value)) {
            is XiaomiScooterTransport.ParsedFrame.Send -> {
                LOG.debug("Got RPT SEND, frameCount={}", frame.frameCount)
                pendingReportFrameCount = frame.frameCount
                pendingReportPayloads.clear()
                write(UUID_CHARACTERISTIC_REPORT, XiaomiScooterTransport.RCV_RDY)
            }

            is XiaomiScooterTransport.ParsedFrame.IndexedData -> {
                LOG.debug("Got RPT indexed data frame {}/{}", frame.index, pendingReportFrameCount)
                pendingReportPayloads.add(frame.payload)
                if (pendingReportPayloads.size < pendingReportFrameCount) {
                    return
                }
                val fullPayload = pendingReportPayloads.reduce { acc, bytes -> acc + bytes }
                pendingReportPayloads.clear()
                pendingReportFrameCount = 1
                write(UUID_CHARACTERISTIC_REPORT, XiaomiScooterTransport.ACK)
                decryptAndHandleReport(fullPayload)
            }

            is XiaomiScooterTransport.ParsedFrame.Data -> {
                LOG.debug("Got RPT frame, sending ack")
                write(UUID_CHARACTERISTIC_REPORT, XiaomiScooterTransport.ACK)
                decryptAndHandleReport(frame.payload)
            }

            else -> LOG.warn("Unexpected RPT frame: {}", frame)
        }
    }

    private fun decryptAndHandleReport(payload: ByteArray) {
        if (payload.size < 2) {
            LOG.warn("RPT payload too short: {}", payload.size)
            return
        }

        val activeSession = session
        if (activeSession == null) {
            LOG.error("Got no active session to decrypt RPT frame")
            return
        }

        val wireCounter = ((payload[1].toInt() and 0xff) shl 8) or (payload[0].toInt() and 0xff)
        val ciphertext = payload.copyOfRange(2, payload.size)

        val plaintext = try {
            activeSession.decryptFromDevice(wireCounter, ciphertext)
        } catch (e: Exception) {
            LOG.warn("Failed to decrypt RPT frame", e)
            return
        }

        handleDecryptedMessage(plaintext)
    }

    private fun handleDecryptedMessage(plaintext: ByteArray) {
        val message = XiaomiScooterProtocol.decode(plaintext)
        if (message == null) {
            LOG.warn("Failed to decode property message: {}", GB.hexdump(plaintext))
            return
        }

        LOG.debug("Got decrypted message opcode={} with {} entries", message.opcode, message.entries)

        when (message.opcode) {
            XiaomiScooterProtocol.OPCODE_GET_RSP, XiaomiScooterProtocol.OPCODE_NOTIFY -> {
                val event = GBDeviceEventUpdatePreferences()
                var hasPreferences = false
                for (entry in message.entries) {
                    if (handlePropertyEntry(entry, event)) {
                        hasPreferences = true
                    }
                }
                if (hasPreferences) {
                    evaluateGBDeviceEvent(event)
                }
            }

            XiaomiScooterProtocol.OPCODE_SET_ACK -> {
                LOG.debug("SET_ACK: {}", message.entries)
            }

            XiaomiScooterProtocol.OPCODE_HELLO -> {
                // Ack was already sent upstream
                LOG.debug("Got HELLO")
            }

            else -> LOG.warn("Unhandled opcode 0x{}", Integer.toHexString(message.opcode))
        }
    }

    private fun handlePropertyEntry(
        entry: XiaomiScooterProtocol.DecodedEntry,
        event: GBDeviceEventUpdatePreferences
    ): Boolean {
        LOG.debug("Processing entry {} = {}", entry.code, entry.value)

        val value = entry.value ?: return false

        when (entry.code) {
            XiaomiScooterProperties.CODE_BATTERY_PERCENT -> {
                value.asU8()?.let {
                    val batteryEvent = GBDeviceEventBatteryInfo()
                    batteryEvent.level = it
                    evaluateGBDeviceEvent(batteryEvent)
                }
                return false
            }

            XiaomiScooterProperties.CODE_SERIAL_NUMBER -> {
                value.asString()?.let {
                    val deviceInfoEvent = GBDeviceEventUpdateDeviceInfo("SERIAL: ", it)
                    evaluateGBDeviceEvent(deviceInfoEvent)
                }
                return false
            }

            XiaomiScooterProperties.CODE_FIRMWARE_VERSION -> {
                value.asString()?.let {
                    val versionEvent = GBDeviceEventVersionInfo()
                    versionEvent.fwVersion = it
                    evaluateGBDeviceEvent(versionEvent)
                }
                return false
            }

            in XiaomiScooterProperties.RIDE_HISTORY_CODES -> {
                value.asString()?.let { onRideHistoryEntry(entry.code, it) }
                return false
            }
        }

        XiaomiScooterProperties.SETTINGS_BY_CODE[entry.code]?.let { mapping ->
            val decoded = mapping.decode(value)
            if (decoded == null) {
                LOG.warn("Failed to decode setting by code for {}", entry.code)
                return false
            }
            event.withPreference(mapping.prefKey, decoded)
            return true
        }

        XiaomiScooterProperties.TELEMETRY_BY_CODE[entry.code]?.let { telemetry ->
            val decoded = telemetry.decode(value)
            if (decoded == null) {
                LOG.warn("Failed to decode telemetry by code for {}", entry.code)
                return false
            }
            event.withPreference(telemetry.prefKey, decoded)
            return true
        }

        return false
    }

    // =======================================================================
    // Ride history (last_ride_1..5)
    // =======================================================================

    private fun onRideHistoryEntry(code: Int, raw: String) {
        rideHistoryRaw[code] = raw
        if (XiaomiScooterProperties.RIDE_HISTORY_CODES.any { it !in rideHistoryRaw }) {
            // We're still missing some of the codes
            return
        }

        LOG.debug("Got all ride history entries")

        val entries = XiaomiScooterProperties.RIDE_HISTORY_CODES.map { rideHistoryRaw.getValue(it) }
        rideHistoryRaw.clear()
        persistNewRides(XiaomiScooterRideHistory.flatten(entries))
    }

    /**
     * [current]: individual 16-digit ride records, oldest first (see [XiaomiScooterRideHistory.flatten]).
     *
     * Dedupes against rides already imported for this device, reading their raw records back
     * from [BaseActivitySummary.getRawSummaryData].
     */
    private fun persistNewRides(current: List<String>) {
        if (current.isEmpty()) {
            return
        }

        try {
            GBApplication.acquireDB().use { db ->
                val daoSession = db.daoSession
                val device = DBHelper.getDevice(gbDevice, daoSession)
                val user = DBHelper.getUser(daoSession)
                val summaryDao = daoSession.baseActivitySummaryDao

                val previouslySeen = summaryDao.queryBuilder()
                    .where(BaseActivitySummaryDao.Properties.DeviceId.eq(device.id))
                    .orderDesc(BaseActivitySummaryDao.Properties.StartTime)
                    .limit(current.size)
                    .list()
                    .mapNotNull { it.rawSummaryData?.toString(Charsets.US_ASCII) }

                val newRides = XiaomiScooterRideHistory.newRidesSince(previouslySeen, current)
                if (newRides.isEmpty()) {
                    LOG.debug("No new rides to be persisted")
                    return
                }

                insertNewRideSummaries(summaryDao, device, user, newRides)
            }
        } catch (e: Exception) {
            LOG.error("Error saving ride history", e)
        }
    }

    /**
     * Inserts the newly-observed rides as activity summaries. The device sends no timestamp for a
     * ride, so times are synthesized: the newest new ride ends "now" (fetch time), and each older
     * one is stacked back-to-back before it using its duration.
     */
    private fun insertNewRideSummaries(
        summaryDao: BaseActivitySummaryDao,
        device: Device,
        user: User,
        newRides: List<String>,
    ) {
        var endTime = Date()
        for (raw in newRides.asReversed()) {
            val ride = XiaomiScooterRideHistory.decodeRecord(raw)
            if (ride == null) {
                LOG.warn("Failed to decode ride history entry {}", raw)
                continue
            }
            if (ride.isEmpty()) {
                continue
            }

            val durationMs = (ride.durationMinutes * 60_000f).toLong()
            val startTime = Date(endTime.time - durationMs)

            val summaryData = ActivitySummaryData()
            summaryData.add(
                ActivitySummaryEntries.ACTIVE_SECONDS,
                durationMs / 1000,
                ActivitySummaryEntries.UNIT_SECONDS
            )
            summaryData.add(
                ActivitySummaryEntries.DISTANCE_METERS,
                ride.distanceKm * 1000,
                ActivitySummaryEntries.UNIT_METERS
            )
            summaryData.add(
                ActivitySummaryEntries.SPEED_AVG,
                ride.avgSpeedKmh,
                ActivitySummaryEntries.UNIT_KMPH
            )

            val summary = BaseActivitySummary()
            summary.device = device
            summary.user = user
            summary.name = ActivityKind.E_SCOOTER.getLabel(context)
            summary.activityKind = ActivityKind.E_SCOOTER.code
            summary.startTime = startTime
            summary.endTime = endTime
            summary.summaryData = summaryData.toString()
            summary.rawSummaryData = raw.toByteArray(Charsets.US_ASCII)
            summaryDao.insert(summary)

            endTime = startTime
        }
    }

    // =======================================================================
    // Settings apply
    // =======================================================================

    override fun onSendConfiguration(config: String) {
        val mapping = XiaomiScooterProperties.SETTINGS_BY_PREF_KEY[config]
        if (mapping != null) {
            val prefs = Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address))
            val data = mapping.encode(prefs)
            enqueueMessage(
                XiaomiScooterProtocol.encodeSet(
                    nextTxn(),
                    listOf(XiaomiScooterProtocol.SetEntry(mapping.code, mapping.wireType, data)),
                )
            )
            return
        } else {
            LOG.warn("No setting mapping for {}", config)
        }

        super.onSendConfiguration(config)
    }

    override fun onFindDevice(start: Boolean) {
        if (start) {
            enqueueMessage(
                XiaomiScooterProtocol.encodeSet(
                    nextTxn(),
                    listOf(
                        XiaomiScooterProtocol.SetEntry(
                            XiaomiScooterProperties.CODE_FIND_SCOOTER,
                            XiaomiScooterProtocol.TYPE_BOOL,
                            byteArrayOf(1)
                        )
                    ),
                )
            )
        }
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    override fun onTestNewFunction(options: Bundle?) {

    }

    // All the relevant characteristics are Write-Without-Response, so there is no real ack that
    // the device actually received/processed a write (GATT_SUCCESS here only means the local
    // stack queued it). Queuing several such writes back-to-back with no pacing risks one being
    // dropped before the device is ready for it -- pace every write with a short settle delay
    // (at least one connection interval) rather than firing bursts.
    private fun write(characteristic: UUID, data: ByteArray) {
        createTransactionBuilder("xiaomi_scooter_write_${characteristic}")
            .write(characteristic, *data)
            .sleep(30)
            .queue()
    }

    /** Steps of the login handshake, driven by [onCharacteristicChanged] on AUTH_CTRL/AUTH_DATA. */
    private enum class AuthStep {
        EXPECT_PRE_KEY_HEADER,
        EXPECT_PRE_KEY_DATA,
        EXPECT_APP_PUBKEY_RDY,
        EXPECT_APP_PUBKEY_OK,
        EXPECT_DEVICE_PUBKEY,
        EXPECT_TOKEN_RDY,
        EXPECT_TOKEN_OK,
        EXPECT_LOGIN_OK,
        DONE,
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(XiaomiScooterSupport::class.java)

        val UUID_SERVICE_XIAOMI: UUID = UUID.fromString("0000fe95-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_AUTH_CTRL: UUID = UUID.fromString("00000010-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_AUTH_DATA: UUID = UUID.fromString("00000016-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_COMMAND: UUID = UUID.fromString("0000001a-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_REPORT: UUID = UUID.fromString("0000001b-0000-1000-8000-00805f9b34fb")
        val UUID_CHARACTERISTIC_DEVICE_INFO: UUID = UUID.fromString("0000001c-0000-1000-8000-00805f9b34fb")

        private val AUTH_START = byteArrayOf(0xa4.toByte())
        private val LOGIN_START = byteArrayOf(0x20, 0x00, 0x00, 0x00)
        private val LOGIN_OK = byteArrayOf(0x21, 0x00, 0x00, 0x00)
        private val LOGIN_FAIL = byteArrayOf(0x22, 0x00, 0x00, 0x00)
    }
}
