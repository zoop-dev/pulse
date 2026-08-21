/*  Copyright (C) 2026 Toby Murray

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.una

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import androidx.core.content.edit
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.R
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo
import nodomain.freeyourgadget.gadgetbridge.devices.BaseActivitySummaryProvider
import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaConstants
import nodomain.freeyourgadget.gadgetbridge.devices.una.UnaDailySampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.UnaDailySample
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.NativeFITMessages
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData
import nodomain.freeyourgadget.gadgetbridge.util.GB
import nodomain.freeyourgadget.gadgetbridge.util.Prefs
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils
import org.slf4j.LoggerFactory
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * Connect, battery, DIS, time sync, workout sync over the File Transfer Service, and daily totals
 * plus a per-minute heart rate timeline over the Custom Command Service.
 *
 * Workout sync reads `/Apps/latest_activity.txt`, a manifest of pending `.fit` paths the firmware
 * maintains, which avoids walking every app's Activity directory. Imported paths are remembered
 * locally; nothing is deleted or acknowledged on the watch.
 *
 * Health data is split by age, because the watch exposes the same data two ways:
 *
 *  - Past days come from `/DailyHealth/<YYYYMM>/dh_<YYYYMMDD>.json` over FTS. One read of about
 *    3.5 KB yields a whole day, and roughly a fortnight is retained. Each day is read once and
 *    then remembered, so routine syncs skip them.
 *  - Today comes from CCS, since the dated file is not written until the day closes. `0x10` gives
 *    the running aggregate and `0x14` gives 60 per-minute heart rates for one elapsed hour.
 *
 * The two carry the same data, so re-reading a day from its file is corrective as well as
 * idempotent: minutes that read as no-data while an hour was still in progress are filled in by
 * the time the file exists.
 *
 * `/DailyHealth/dh.tmp` holds the day in progress but is the firmware's CRC-checked crash-recovery
 * record, not JSON, so today stays on CCS.
 *
 * Both CCS commands share one characteristic and are told apart by echoed opcode; all three kinds
 * of FTS read share another and are told apart by `currentReadKind`, since an FTS response
 * identifies nothing about its request.
 *
 * No request is guarded by a timeout. The drain advances only on a response, so a request the
 * firmware never answers leaves the sync stuck until the next reconnect resets it. The firmware
 * answers every CCS request, including nonsensical ones, which is why this is tolerable, but it
 * is also why the windows below stay bounded.
 */
class UnaDeviceSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val deviceInfoProfile: DeviceInfoProfile<UnaDeviceSupport>
    private val batteryInfoProfile: BatteryInfoProfile<UnaDeviceSupport>

    /** What the in-flight FTS read is for, since all three ride the same request/response path. */
    private enum class ReadKind { MANIFEST, ACTIVITY, DAILY_HEALTH }

    private var draining = false
    private val pendingManifestPaths = ArrayDeque<String>()
    private var currentReadPath: String? = null
    private var currentReadKind = ReadKind.MANIFEST
    private var currentReadWindow = UnaReadWindow(UnaConstants.READ_WINDOW_SIZE)
    private val pendingHealthFileDays = ArrayDeque<Calendar>()
    private var currentHealthFileDay: Calendar? = null
    private val pendingHealthDays = ArrayDeque<Calendar>()
    private var currentHealthDay: Calendar? = null
    private val pendingHrHours = ArrayDeque<Calendar>()
    private var currentHrHour: Calendar? = null

    init {
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION)
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE)
        addSupportedService(GattService.UUID_SERVICE_CURRENT_TIME)
        addSupportedService(UnaConstants.UUID_SERVICE_FTS)
        addSupportedService(UnaConstants.UUID_SERVICE_CCS)

        val listener = IntentListener { intent ->
            when (intent.action) {
                DeviceInfoProfile.ACTION_DEVICE_INFO ->
                    handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO)!!)
                BatteryInfoProfile.ACTION_BATTERY_INFO ->
                    handleBatteryInfo(intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO)!!)
            }
        }

        deviceInfoProfile = DeviceInfoProfile(this)
        deviceInfoProfile.addListener(listener)
        addSupportedProfile(deviceInfoProfile)

        batteryInfoProfile = BatteryInfoProfile(this)
        batteryInfoProfile.addListener(listener)
        addSupportedProfile(batteryInfoProfile)
    }

    override fun useAutoConnect(): Boolean = false

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        // Without this a disconnect mid-sync leaves `draining` stuck true, silently no-oping every
        // later onFetchRecordedData() for the process lifetime.
        draining = false
        pendingManifestPaths.clear()
        currentReadPath = null
        currentReadKind = ReadKind.MANIFEST
        currentReadWindow = UnaReadWindow(UnaConstants.READ_WINDOW_SIZE)
        pendingHealthFileDays.clear()
        currentHealthFileDay = null
        pendingHealthDays.clear()
        currentHealthDay = null
        pendingHrHours.clear()
        currentHrHour = null

        builder.setDeviceState(GBDevice.State.INITIALIZING)

        builder.requestMtu(UnaConstants.MTU_REQUEST)

        deviceInfoProfile.requestDeviceInfo(builder)

        batteryInfoProfile.requestBatteryInfo(builder)
        batteryInfoProfile.enableNotify(builder, true)

        if (GBApplication.getPrefs().syncTime()) {
            writeCurrentTime(builder)
        }

        builder.notify(UnaConstants.UUID_CHARACTERISTIC_FTS, true)
        builder.notify(UnaConstants.UUID_CHARACTERISTIC_CCS_COMMAND, true)

        builder.setDeviceState(GBDevice.State.INITIALIZED)
        return builder
    }

    override fun onSetTime() {
        if (!GBApplication.getPrefs().syncTime()) return
        val builder = createTransactionBuilder("set time")
        writeCurrentTime(builder)
        builder.queue()
    }

    private fun writeCurrentTime(builder: TransactionBuilder) {
        val now = GregorianCalendar()
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME, *BLETypeConversions.calendarToCurrentTime(now, 0))
        builder.write(GattCharacteristic.UUID_CHARACTERISTIC_LOCAL_TIME, *BLETypeConversions.calendarToLocalTime(now))
    }

    private fun handleDeviceInfo(info: DeviceInfo) {
        LOG.debug("Device info: {}", info)
        for (event in DeviceInfoProfile.toDeviceEvents(info)) {
            evaluateGBDeviceEvent(event)
        }
    }

    private fun handleBatteryInfo(info: BatteryInfo) {
        LOG.debug("Battery info: {}", info)
        val battery = GBDeviceEventBatteryInfo()
        battery.level = info.percentCharged
        evaluateGBDeviceEvent(battery)
    }

    override fun onFetchRecordedData(dataTypes: Int) {
        if (dataTypes and RecordedDataTypes.TYPE_ACTIVITY == 0) return
        if (draining) return
        draining = true
        pendingManifestPaths.clear()
        device.setBusyTask(R.string.busy_task_fetch_activity_data, context)
        device.sendDeviceUpdateIntent(context)

        // FTS must complete before CCS is touched at all: afterwards the firmware's FTS read
        // handler rejects every subsequent request, and no settle delay recovers it.
        startReadFile(MANIFEST_PATH, ReadKind.MANIFEST)
    }

    private fun requestNextDailyHealth() {
        val day = pendingHealthDays.removeFirstOrNull()
        if (day == null) {
            currentHealthDay = null
            LOG.debug("Daily health sync complete, starting hourly HR sync")
            startHourlyHrSync()
            return
        }
        currentHealthDay = day
        val builder = createTransactionBuilder("daily health ${day.time}")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_CCS_COMMAND,
            *UnaDailyHealthProtocol.buildRequest(
                year = day.get(Calendar.YEAR),
                month = day.get(Calendar.MONTH) + 1,
                day = day.get(Calendar.DAY_OF_MONTH),
            ),
        )
        builder.queue()
    }

    /**
     * Today's elapsed hours, oldest first, so the current still-filling hour is fetched last.
     * An hour with no data answers normally with zeros and persists nothing. About 90 ms each.
     */
    private fun startHourlyHrSync() {
        pendingHrHours.clear()
        val hour = GregorianCalendar().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Earlier days come from their file in one read, so walking back past midnight would
        // re-fetch what the file already gave, an hour per round trip.
        for (i in hour.get(Calendar.HOUR_OF_DAY) downTo 0) {
            pendingHrHours.addLast((hour.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, -i) })
        }
        requestNextHourlyHr()
    }

    private fun requestNextHourlyHr() {
        val hour = pendingHrHours.removeFirstOrNull()
        if (hour == null) {
            currentHrHour = null
            LOG.debug("Hourly HR sync complete")
            endDrain()
            return
        }
        currentHrHour = hour
        val builder = createTransactionBuilder("hourly hr ${hour.time}")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_CCS_COMMAND,
            *UnaHourlyHrProtocol.buildRequest(
                year = hour.get(Calendar.YEAR),
                month = hour.get(Calendar.MONTH) + 1,
                day = hour.get(Calendar.DAY_OF_MONTH),
                hour = hour.get(Calendar.HOUR_OF_DAY),
            ),
        )
        builder.queue()
    }

    private fun startReadFile(path: String, kind: ReadKind) {
        currentReadPath = path
        currentReadKind = kind
        currentReadWindow = UnaReadWindow(UnaConstants.READ_WINDOW_SIZE)
        val builder = createTransactionBuilder("read $path")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_FTS,
            *UnaFtsProtocol.buildReadRequest(path, 0, UnaConstants.READ_WINDOW_SIZE),
        )
        builder.queue()
    }

    private fun requestNextWindow(offset: Int) {
        val builder = createTransactionBuilder("read window @ $offset")
        builder.write(
            UnaConstants.UUID_CHARACTERISTIC_FTS,
            *UnaFtsProtocol.buildReadPacingRequest(offset, UnaConstants.READ_WINDOW_SIZE),
        )
        builder.queue()
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        when (characteristic.uuid) {
            UnaConstants.UUID_CHARACTERISTIC_FTS -> {
                val chunk = UnaFtsProtocol.parseReadChunk(value)
                if (chunk == null) {
                    handleReadFailure(value)
                    return true
                }
                handleReadChunk(chunk)
            }
            UnaConstants.UUID_CHARACTERISTIC_CCS_COMMAND -> handleCcsResponse(value)
            else -> return super.onCharacteristicChanged(gatt, characteristic, value)
        }
        return true
    }

    /**
     * Dispatches on the echoed opcode rather than on whichever queue is draining, so a late or
     * duplicated response cannot reach the wrong parser.
     */
    private fun handleCcsResponse(value: ByteArray) {
        when (value.firstOrNull()?.toInt()?.and(0xFF)) {
            UnaConstants.CMD_DAILY_HEALTH -> handleDailyHealth(UnaDailyHealthProtocol.parseResponse(value))
            UnaConstants.CMD_HOURLY_HR -> handleHourlyHr(UnaHourlyHrProtocol.parseResponse(value))
            else -> LOG.warn("Unrecognized CCS notification: {}", StringUtils.bytesToHex(value))
        }
    }

    /**
     * A null [health] still has to advance the queue, or the rest of the sync is stranded. A day
     * with no data is not null; it returns a normal response full of zeros.
     */
    private fun handleDailyHealth(health: UnaDailyHealth?) {
        val day = currentHealthDay
        if (day == null) {
            LOG.warn("Got a daily health response with no pending request: {}", health)
            return
        }
        if (health == null) {
            LOG.debug("No daily health data for {}", day.time)
        } else {
            persistDailyHealth(day, health)
        }
        requestNextDailyHealth()
    }

    /** Null [hr] means the response did not parse. An hour with no readings is not null; it
     * parses fine and simply has no measured minutes. */
    private fun handleHourlyHr(hr: UnaHourlyHr?) {
        val hour = currentHrHour
        if (hour == null) {
            LOG.warn("Got an hourly HR response with no pending request")
            return
        }
        if (hr == null) {
            LOG.debug("No hourly HR data for {}", hour.time)
        } else {
            persistHourlyHr(hour, hr)
        }
        requestNextHourlyHr()
    }

    /**
     * A non-`0x11` reply, in practice a short frame such as `10 03` (ERROR_NO_FILE) rather than
     * the 16-byte header. Treated as an empty file: the manifest then means zero pending
     * activities, and an activity file is skipped rather than retried forever.
     */
    private fun handleReadFailure(rawResponse: ByteArray) {
        val path = currentReadPath ?: return
        LOG.warn(
            "FTS read of {} failed (response: {}), treating as not found",
            path,
            StringUtils.bytesToHex(rawResponse),
        )
        onFileReadComplete(path, ByteArray(0))
    }

    private fun handleReadChunk(chunk: UnaFtsReadChunk) {
        val path = currentReadPath
        if (path == null) {
            LOG.warn("Got a read chunk with no pending read: offset={} total={}", chunk.offset, chunk.total)
            return
        }
        val window = currentReadWindow
        when (window.accept(chunk)) {
            UnaReadWindow.Next.WAIT -> Unit
            UnaReadWindow.Next.REQUEST_NEXT_WINDOW -> requestNextWindow(window.firstByteNotHeld)
            UnaReadWindow.Next.COMPLETE -> {
                LOG.debug(
                    "Read {} ({} bytes) in {} notifications over {} requests, shortDelivery={}",
                    path,
                    window.firstByteNotHeld,
                    window.notifications,
                    window.requests,
                    window.sawShortDelivery,
                )
                onFileReadComplete(path, window.assemble())
            }
        }
    }

    private fun onFileReadComplete(path: String, bytes: ByteArray) {
        when (currentReadKind) {
            ReadKind.MANIFEST -> handleManifest(bytes)
            ReadKind.ACTIVITY -> if (handleActivityFile(path, bytes)) markSynced(path)
            ReadKind.DAILY_HEALTH -> handleDailyHealthFile(path, bytes)
        }
        advanceFtsPhase()
    }

    /**
     * Activity files, then past days' health records, then CCS for today. Each phase drains before
     * the next, since the firmware answers one FTS request at a time and responses are anonymous.
     */
    private fun advanceFtsPhase() {
        pendingManifestPaths.removeFirstOrNull()?.let {
            startReadFile(it, ReadKind.ACTIVITY)
            return
        }
        if (currentReadKind == ReadKind.ACTIVITY || currentReadKind == ReadKind.MANIFEST) {
            queueHealthFileDays()
        }
        val day = pendingHealthFileDays.removeFirstOrNull()
        if (day != null) {
            currentHealthFileDay = day
            startReadFile(UnaDailyHealthFile.pathFor(day), ReadKind.DAILY_HEALTH)
            return
        }
        currentHealthFileDay = null
        LOG.debug("FTS sync complete, starting CCS sync for today")
        startTodayCcsSync()
    }

    /**
     * Past days not yet imported, oldest first. Only days before today are eligible, since the
     * dated file is not written until a day closes.
     *
     * A day is marked imported once read, successfully or not: a closed day's record cannot
     * change, and a day with no file would otherwise be re-requested forever. A read that fails
     * partway rather than returning empty is not marked, so transport errors still retry.
     */
    private fun queueHealthFileDays() {
        pendingHealthFileDays.clear()
        val imported = importedHealthDates()
        val day = GregorianCalendar()
        for (i in HEALTH_HISTORY_DAYS downTo 1) {
            val candidate = (day.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -i) }
            if (healthDateKey(candidate) !in imported) {
                pendingHealthFileDays.addLast(candidate)
            }
        }
        LOG.debug("{} past day(s) of health records to fetch", pendingHealthFileDays.size)
    }

    /** Today only, from CCS, since the dated file does not exist yet. */
    private fun startTodayCcsSync() {
        pendingHealthDays.clear()
        pendingHealthDays.addLast(GregorianCalendar())
        requestNextDailyHealth()
    }

    /**
     * Empty [bytes] means no file for that day, which the read path produces from the firmware's
     * ERROR_NO_FILE reply. A normal outcome, recorded as imported so it is not requested again.
     */
    private fun handleDailyHealthFile(path: String, bytes: ByteArray) {
        val day = currentHealthFileDay
        if (day == null) {
            LOG.warn("Read {} as a health file with no pending day", path)
            return
        }
        if (bytes.isEmpty()) {
            LOG.debug("No health record on watch for {}", path)
            markHealthDateImported(day)
            return
        }
        val file = UnaDailyHealthFile.parse(bytes)
        if (file == null) {
            LOG.warn("Could not parse health record {} ({} bytes)", path, bytes.size)
            return
        }
        persistDailyHealthFile(day, file)
        markHealthDateImported(day)
    }

    private fun handleManifest(bytes: ByteArray) {
        val alreadySynced = syncedPaths()
        val paths = String(bytes, Charsets.UTF_8).lineSequence()
            .map { it.trim() }
            .filter { it.endsWith(".fit") }
            .filter { it !in alreadySynced }
            .toList()
        LOG.debug("Manifest has {} new activity file(s) to sync", paths.size)
        pendingManifestPaths.addAll(paths)
    }

    /** True once parsed and persisted, which is the condition for marking the path synced. */
    private fun handleActivityFile(path: String, bytes: ByteArray): Boolean {
        return try {
            val fitFile = FitFile.parseIncoming(bytes)
            val records = fitFile.getRecordsByNativeMessage(NativeFITMessages.FIT_RECORD())
            persistWorkout(records, activityKindForPath(path))
        } catch (e: Exception) {
            LOG.error("Failed to parse activity file {}", path, e)
            false
        }
    }

    private fun activityKindForPath(path: String): ActivityKind = when {
        path.contains("/Running/") -> ActivityKind.RUNNING
        path.contains("/Cycling/") -> ActivityKind.CYCLING
        path.contains("/Hiking/") -> ActivityKind.HIKING
        path.contains("/Treadmill/") -> ActivityKind.TREADMILL
        path.contains("/Workout/") -> ActivityKind.EXERCISE
        else -> ActivityKind.ACTIVITY
    }

    // Each file is a single discrete recorded workout, not continuous all-day monitoring,
    // so it's stored as a BaseActivitySummary (like Garmin's FitImporter does for ACTIVITY-type
    // FIT files) rather than a per-second AbstractActivitySample stream.
    private fun persistWorkout(records: List<RecordData>, kind: ActivityKind): Boolean {
        val timestamps = records.mapNotNull { it.computedTimestamp }
        val startTimestamp = timestamps.minOrNull() ?: return true
        val endTimestamp = timestamps.maxOrNull()!!

        val heartRates = records.mapNotNull { (it.getFieldByName("heart_rate") as? Number)?.toInt() }
            .filter { it > 0 }
        val distances = records.mapNotNull { (it.getFieldByName("distance") as? Number)?.toDouble() }

        return try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val summary = ActivitySummaryParser.findOrCreateBaseActivitySummary(session, device, startTimestamp)
                summary.endTime = Date(endTimestamp * 1000L)
                summary.activityKind = kind.code

                val summaryData = ActivitySummaryData()
                if (heartRates.isNotEmpty()) {
                    summaryData.add(ActivitySummaryEntries.HR_AVG, heartRates.average(), ActivitySummaryEntries.UNIT_BPM)
                }
                distances.maxOrNull()?.let {
                    summaryData.add(ActivitySummaryEntries.DISTANCE_METERS, it, ActivitySummaryEntries.UNIT_METERS)
                }
                summary.summaryData = summaryData.toJson()

                BaseActivitySummaryProvider(device, session).persistSamples(listOf(summary), context)
            }
        } catch (e: Exception) {
            LOG.error("Error saving {} workout summary", kind, e)
            false
        }
    }

    /**
     * Persists today's running aggregate as exactly one sample, carrying the steps and floors
     * that have no per-minute source on this firmware, plus the day's average and resting HR.
     *
     * One sample rather than a fan-out. Spreading the aggregate over fabricated per-minute samples
     * would make sample-gap arithmetic report the watch's active-minutes figure, at the cost of
     * inventing a timeline that never existed.
     *
     * [DAILY_TOTALS_OFFSET_SECONDS] is what keeps the two apart: per-minute HR samples always
     * land exactly on a minute boundary, so anchoring the daily total just off one guarantees it
     * can never be overwritten by (or overwrite) a real reading, in any sync order.
     */
    private fun persistDailyHealth(day: Calendar, health: UnaDailyHealth) {
        val anchor = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val anchorSeconds = (anchor.timeInMillis / 1000L).toInt() + DAILY_TOTALS_OFFSET_SECONDS

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val dbUser = DBHelper.getUser(session)
                val dbDevice = DBHelper.getDevice(device, session)
                val provider = UnaDailySampleProvider(device, session)
                val sample = UnaDailySample().apply {
                    timestamp = anchorSeconds
                    rawKind = ActivityKind.ACTIVITY.code
                    heartRate = health.averageHeartRate
                    restingHeartRate = health.restingHeartRate
                    steps = health.steps
                    floorsClimbed = health.floors
                    setDevice(dbDevice)
                    setUser(dbUser)
                    setProvider(provider)
                }
                provider.addGBActivitySample(sample)
            }
        } catch (e: Exception) {
            LOG.error("Error saving daily health for {}", day.time, e)
        }
    }

    /**
     * Persists a whole day from its on-watch record: the aggregate on one sample, plus a real
     * per-minute HR sample for every minute of the day that carries a reading.
     *
     * Same shape as [persistDailyHealth] plus [persistHourlyHr] together, since the file and the
     * CCS commands are the same data. Writing both is idempotent: today is written from CCS now
     * and re-written from the file tomorrow, at the same timestamps.
     *
     * Only [UnaDailyHealthFile.floorsUp] is stored; the schema has a single floors column, and
     * floors *climbed* is what it means. [UnaDailyHealthFile.floorsDown] is parsed but dropped.
     */
    private fun persistDailyHealthFile(day: Calendar, file: UnaDailyHealthFile) {
        val midnight = (day.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val midnightSeconds = (midnight.timeInMillis / 1000L).toInt()
        val measured = file.measuredMinutes()

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val dbUser = DBHelper.getUser(session)
                val dbDevice = DBHelper.getDevice(device, session)
                val provider = UnaDailySampleProvider(device, session)

                val samples = mutableListOf<UnaDailySample>()
                samples += UnaDailySample().apply {
                    timestamp = midnightSeconds + DAILY_TOTALS_OFFSET_SECONDS
                    rawKind = ActivityKind.ACTIVITY.code
                    heartRate = file.averageHeartRate
                    restingHeartRate = file.restingHeartRate
                    steps = file.steps
                    floorsClimbed = file.floorsUp
                    setDevice(dbDevice)
                    setUser(dbUser)
                    setProvider(provider)
                }
                for ((minute, bpm) in measured) {
                    samples += UnaDailySample().apply {
                        timestamp = midnightSeconds + minute * 60
                        rawKind = ActivityKind.ACTIVITY.code
                        heartRate = bpm
                        restingHeartRate = ActivitySample.NOT_MEASURED
                        steps = 0
                        floorsClimbed = 0
                        setDevice(dbDevice)
                        setUser(dbUser)
                        setProvider(provider)
                    }
                }
                provider.addGBActivitySamples(samples)
                LOG.debug(
                    "Saved {} of {} minutes of HR plus totals for {}",
                    measured.size, file.hrPerMinute.size, midnight.time,
                )
            }
        } catch (e: Exception) {
            LOG.error("Error saving health record for {}", midnight.time, e)
        }
    }

    /**
     * Persists one hour's HR matrix as real per-minute samples, one per minute that actually
     * carries a reading. Zero means no reading rather than a heart rate of zero, so those minutes
     * are dropped instead of stored.
     *
     * Steps and floors are left at zero here because this command carries neither; the day's
     * totals live on the single aggregate sample written by `persistDailyHealth`. Resting HR is
     * likewise per-day, not per-minute, so it is marked not-measured on these samples.
     */
    private fun persistHourlyHr(hour: Calendar, hr: UnaHourlyHr) {
        val measured = hr.measuredMinutes()
        if (measured.isEmpty()) {
            LOG.debug("Hourly HR for {} has no readings", hour.time)
            return
        }
        val hourSeconds = (hour.timeInMillis / 1000L).toInt()

        try {
            GBApplication.acquireDB().use { dbHandler ->
                val session = dbHandler.daoSession
                val dbUser = DBHelper.getUser(session)
                val dbDevice = DBHelper.getDevice(device, session)
                val provider = UnaDailySampleProvider(device, session)
                val samples = measured.map { (minute, bpm) ->
                    UnaDailySample().apply {
                        timestamp = hourSeconds + minute * 60
                        rawKind = ActivityKind.ACTIVITY.code
                        heartRate = bpm
                        restingHeartRate = ActivitySample.NOT_MEASURED
                        steps = 0
                        floorsClimbed = 0
                        setDevice(dbDevice)
                        setUser(dbUser)
                        setProvider(provider)
                    }
                }
                provider.addGBActivitySamples(samples)
                LOG.debug("Saved {}/60 HR readings for {}", samples.size, hour.time)
            }
        } catch (e: Exception) {
            LOG.error("Error saving hourly HR for {}", hour.time, e)
        }
    }

    private fun healthDateKey(day: Calendar): String = String.format(
        Locale.ROOT, "%04d%02d%02d",
        day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH),
    )

    private fun importedHealthDates(): Set<String> =
        devicePrefs.getStringSet(PREF_KEY_IMPORTED_HEALTH_DATES, emptySet())

    /**
     * Records a past day as done. Old keys are pruned rather than accumulated forever: only dates
     * still inside the fetch window can ever be consulted again, so anything older is dead weight
     * in the preferences file. The keys are fixed-width `YYYYMMDD`, so comparing them as strings
     * orders them by date.
     */
    private fun markHealthDateImported(day: Calendar) {
        val cutoff = GregorianCalendar().apply { add(Calendar.DAY_OF_MONTH, -HEALTH_HISTORY_DAYS) }
        val cutoffKey = healthDateKey(cutoff)
        val updated = importedHealthDates().filterTo(HashSet()) { it >= cutoffKey }
        updated.add(healthDateKey(day))
        devicePrefs.preferences.edit {
            Prefs.putStringSet(this, PREF_KEY_IMPORTED_HEALTH_DATES, updated)
        }
    }

    private fun syncedPaths(): Set<String> = devicePrefs.getStringSet(PREF_KEY_SYNCED_PATHS, emptySet())

    private fun markSynced(path: String) {
        val updated = HashSet(syncedPaths())
        updated.add(path)
        devicePrefs.preferences.edit {
            Prefs.putStringSet(this, PREF_KEY_SYNCED_PATHS, updated)
        }
    }

    private fun endDrain() {
        LOG.debug("Activity fetch complete")
        draining = false
        if (device.isBusy) {
            device.unsetBusyTask()
            device.sendDeviceUpdateIntent(context)
        }
        GB.signalActivityDataFinish(device)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UnaDeviceSupport::class.java)
        private const val MANIFEST_PATH = "/Apps/latest_activity.txt"
        private const val PREF_KEY_SYNCED_PATHS = "una_synced_activity_paths"
        private const val PREF_KEY_IMPORTED_HEALTH_DATES = "una_imported_health_dates"

        // Bounded by how long the watch keeps dated records. Imported days are skipped, so this
        // is a one-time cost, and a day with no file costs one wasted read.
        private const val HEALTH_HISTORY_DAYS = 14

        // Seconds past local midnight for a day's aggregate sample. Must not be a whole number of
        // minutes; see persistDailyHealth.
        private const val DAILY_TOTALS_OFFSET_SECONDS = 30
    }
}
