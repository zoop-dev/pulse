package nodomain.freeyourgadget.gadgetbridge.util.healthconnect

import android.content.Context
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.model.*
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import org.json.JSONArray
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object GadgetbridgeDataExporter {
    private val LOG = LoggerFactory.getLogger(GadgetbridgeDataExporter::class.java)
    private const val TAG = "[HC_SYNC]"

    fun export(context: Context, gbDevice: GBDevice, startInstant: Instant, endInstant: Instant): File {
        LOG.info("$TAG Export starting for device '{}', range {} to {}", gbDevice.aliasOrName, startInstant, endInstant)

        val result = JSONObject()
        result.put("export_time", Instant.now().toString())
        result.put("source", "gadgetbridge")
        result.put("device", JSONObject().apply {
            put("name", gbDevice.aliasOrName)
            put("address", gbDevice.address)
            put("model", gbDevice.model ?: "")
        })
        result.put("time_range", JSONObject().apply {
            put("start", startInstant.toString())
            put("end", endInstant.toString())
        })

        val data = JSONObject()

        val coordinator = gbDevice.deviceCoordinator

        GBApplication.acquireDbReadOnly().use { db ->
            val tsFrom = startInstant.epochSecond.toInt()
            val tsTo = endInstant.epochSecond.toInt()
            val msFrom = startInstant.toEpochMilli()
            val msTo = endInstant.toEpochMilli()

            val activitySamples = try {
                val provider = coordinator.getSampleProvider(gbDevice, db.daoSession)
                provider?.getAllActivitySamples(tsFrom, tsTo) ?: emptyList()
            } catch (e: Exception) {
                LOG.error("$TAG Failed to get activity samples", e)
                emptyList()
            }

            data.put("steps", exportSteps(activitySamples))
            data.put("heart_rate", exportHeartRate(activitySamples))
            data.put("sleep", exportSleep(activitySamples))
            data.put("spo2", exportTimeSamples(coordinator.getSpo2SampleProvider(gbDevice, db.daoSession), msFrom, msTo) { sample ->
                val spo2 = (sample as? Spo2Sample)?.spo2 ?: return@exportTimeSamples null
                if (spo2 <= 0 || spo2 > 100) return@exportTimeSamples null
                JSONObject().apply {
                    put("time", Instant.ofEpochMilli(sample.timestamp).toString())
                    put("percentage", spo2.toDouble())
                }
            })
            data.put("hrv", exportTimeSamples(coordinator.getHrvValueSampleProvider(gbDevice, db.daoSession), msFrom, msTo) { sample ->
                val hrv = (sample as? HrvValueSample)?.value ?: return@exportTimeSamples null
                if (hrv <= 0) return@exportTimeSamples null
                JSONObject().apply {
                    put("time", Instant.ofEpochMilli(sample.timestamp).toString())
                    put("rmssd_ms", hrv.toDouble())
                }
            })
            data.put("respiratory_rate", exportTimeSamples(coordinator.getRespiratoryRateSampleProvider(gbDevice, db.daoSession), msFrom, msTo) { sample ->
                val rate = (sample as? RespiratoryRateSample)?.respiratoryRate ?: return@exportTimeSamples null
                if (rate <= 0) return@exportTimeSamples null
                JSONObject().apply {
                    put("time", Instant.ofEpochMilli(sample.timestamp).toString())
                    put("rate", rate.toDouble())
                }
            })
            data.put("resting_heart_rate", exportTimeSamples(coordinator.getHeartRateRestingSampleProvider(gbDevice, db.daoSession), msFrom, msTo) { sample ->
                val bpm = (sample as? HeartRateSample)?.heartRate ?: return@exportTimeSamples null
                if (bpm <= 0) return@exportTimeSamples null
                JSONObject().apply {
                    put("time", Instant.ofEpochMilli(sample.timestamp).toString())
                    put("bpm", bpm)
                }
            })
            data.put("temperature", exportTimeSamples(coordinator.getTemperatureSampleProvider(gbDevice, db.daoSession), msFrom, msTo) { sample ->
                val temp = (sample as? TemperatureSample) ?: return@exportTimeSamples null
                JSONObject().apply {
                    put("time", Instant.ofEpochMilli(sample.timestamp).toString())
                    put("celsius", temp.temperature.toDouble())
                    put("type", temp.temperatureType)
                }
            })
            data.put("exercise_sessions", exportWorkouts(db, gbDevice, msFrom, msTo))
        }

        result.put("data", data)

        val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val timestamp = ZonedDateTime.now().format(dateFormatter)
        val fileName = "gb_export_${gbDevice.aliasOrName.replace(" ", "_")}_$timestamp.json"
        val dir = FileUtils.getExternalFilesDir()
        val outFile = File(dir, fileName)
        outFile.writeText(result.toString(2))

        LOG.info("$TAG Export complete: {}", outFile.absolutePath)
        return outFile
    }

    private fun exportSteps(samples: List<ActivitySample>): JSONArray {
        val arr = JSONArray()
        for (s in samples) {
            if (s.steps <= 0) continue
            arr.put(JSONObject().apply {
                put("time", Instant.ofEpochSecond(s.timestamp.toLong()).toString())
                put("steps", s.steps)
            })
        }
        return arr
    }

    private fun exportHeartRate(samples: List<ActivitySample>): JSONArray {
        val arr = JSONArray()
        for (s in samples) {
            val hr = s.heartRate
            if (hr <= 0 || hr >= 255) continue
            arr.put(JSONObject().apply {
                put("time", Instant.ofEpochSecond(s.timestamp.toLong()).toString())
                put("bpm", hr)
            })
        }
        return arr
    }

    private fun exportSleep(samples: List<ActivitySample>): JSONArray {
        val sleepSamples = samples.filter { isSleep(it) }
        val arr = JSONArray()
        for (s in sleepSamples) {
            arr.put(JSONObject().apply {
                put("time", Instant.ofEpochSecond(s.timestamp.toLong()).toString())
                put("stage", mapSleepStage(s.kind))
            })
        }
        return arr
    }

    private fun isSleep(sample: ActivitySample): Boolean {
        return sample.kind == ActivityKind.DEEP_SLEEP ||
                sample.kind == ActivityKind.LIGHT_SLEEP ||
                sample.kind == ActivityKind.REM_SLEEP ||
                sample.kind == ActivityKind.AWAKE_SLEEP
    }

    private fun mapSleepStage(kind: ActivityKind): String {
        return when (kind) {
            ActivityKind.LIGHT_SLEEP -> "light"
            ActivityKind.DEEP_SLEEP -> "deep"
            ActivityKind.REM_SLEEP -> "rem"
            ActivityKind.AWAKE_SLEEP -> "awake"
            else -> "unknown"
        }
    }

    private fun <T : TimeSample> exportTimeSamples(
        provider: TimeSampleProvider<out T>?,
        msFrom: Long,
        msTo: Long,
        converter: (T) -> JSONObject?
    ): JSONArray {
        val arr = JSONArray()
        if (provider == null) return arr
        try {
            @Suppress("UNCHECKED_CAST")
            val samples = provider.getAllSamples(msFrom, msTo) as List<T>
            for (s in samples) {
                val json = converter(s)
                if (json != null) arr.put(json)
            }
        } catch (e: Exception) {
            LOG.error("$TAG Failed to export time samples", e)
        }
        return arr
    }

    private fun exportWorkouts(db: DBHandler, gbDevice: GBDevice, msFrom: Long, msTo: Long): JSONArray {
        val arr = JSONArray()
        try {
            val deviceEntity = DBHelper.getDevice(gbDevice, db.daoSession) ?: return arr
            val summaries = db.daoSession.baseActivitySummaryDao.queryBuilder()
                .where(
                    BaseActivitySummaryDao.Properties.DeviceId.eq(deviceEntity.id),
                    BaseActivitySummaryDao.Properties.StartTime.ge(java.util.Date(msFrom)),
                    BaseActivitySummaryDao.Properties.StartTime.lt(java.util.Date(msTo))
                )
                .orderAsc(BaseActivitySummaryDao.Properties.StartTime)
                .list()

            for (s in summaries) {
                arr.put(JSONObject().apply {
                    put("start", s.startTime.toInstant().toString())
                    put("end", s.endTime.toInstant().toString())
                    put("activity_kind", s.activityKind)
                    put("name", s.name ?: "")
                })
            }
        } catch (e: Exception) {
            LOG.error("$TAG Failed to export workouts", e)
        }
        return arr
    }
}

