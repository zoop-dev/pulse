package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.protobuf.ByteString
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigBoolean
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigFloat
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigInteger
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.config.DynamicAppConfig.AppConfigString
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventAppConfig
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiAppConfigService
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiAppConfigService.AppConfigService.AppConfig
import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiSmartProto
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminJson
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http.GarminJsonException
import nodomain.freeyourgadget.gadgetbridge.util.UuidUtil
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.buildWith
import org.slf4j.LoggerFactory
import java.util.UUID

class AppConfigHandler(private val deviceSupport: GarminSupport) {
    fun process(appConfigService: GdiAppConfigService.AppConfigService): Boolean {
        if (appConfigService.hasAppConfigSetStatus()) {
            val uuid = UuidUtil.fromBytes(appConfigService.appConfigSetStatus.appId.toByteArray())
            val status = appConfigService.appConfigSetStatus.status
            LOG.debug("Got app config set status for {}: {}", uuid, status)
            if (status != 1) {
                LOG.warn("Failed to get configs for {}, status={}", uuid, status)
                evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_SET_FAILED)
            } else {
                evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_SET_SUCCESS)
            }
            return true
        }

        if (appConfigService.hasAppConfigGetStatus()) {
            val uuid = UuidUtil.fromBytes(appConfigService.appConfigGetStatus.appId.toByteArray())
            val status = appConfigService.appConfigGetStatus.status
            LOG.debug("Got app config get status for {}: {}", uuid, status)
            if (status != 1) {
                LOG.warn("Failed to get configs for {}, status={}", uuid, status)
                evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED)
            }
            // On success we just wait, we'll get the configs in a future command
            return true
        }

        if (!appConfigService.hasAppConfigRet()) {
            LOG.warn("Got unknown app settings response")
            return false
        }

        val settings: AppConfig = appConfigService.appConfigRet
        val uuid = UuidUtil.fromBytes(settings.appId.toByteArray())

        LOG.debug("Got app config ret for {}", uuid)

        val jsonElement: JsonElement = try {
            GarminJson.decode(settings.appConfig.toByteArray())
        } catch (e: Exception) {
            LOG.error("Failed to decode app settings for {}", uuid, e)
            evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED)
            return true
        }

        LOG.debug("Got app settings for {}: {}", uuid, jsonElement)

        if (jsonElement !is JsonObject) {
            LOG.error(
                "App settings for {} are not a JSON object: {}",
                uuid,
                jsonElement.javaClass
            )
            evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED)
            return true
        }
        val configs = jsonElement.keySet().map { key ->
            LOG.debug("App setting {}: {}", key, jsonElement.get(key))

            val value = jsonElement.get(key)
            if (value !is JsonPrimitive) {
                LOG.error(
                    "App setting value for {} is not a json primitive: {}",
                    key,
                    value.javaClass
                )
                evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED)
                return true
            }

            if (value.isBoolean) {
                return@map AppConfigBoolean(key, value.getAsBoolean())
            } else if (value.isNumber) {
                val num = value.getAsNumber()
                val doubleValue = num.toDouble()
                val longValue = num.toLong()
                if (doubleValue != longValue.toDouble() || num is Float || num is Double) {
                    return@map AppConfigFloat(key, num.toFloat())
                } else {
                    return@map AppConfigInteger(key, num.toInt())
                }
            } else if (value.isString) {
                return@map AppConfigString(key, value.getAsString())
            } else {
                LOG.error("Unknown app setting type: {}", value)
                evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_FAILED)
                return true
            }
        }.toList()

        evaluateEvent(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_GET_SUCCESS, ArrayList(configs))

        return true
    }

    fun evaluateEvent(uuid: UUID, event: GBDeviceEventAppConfig.Event, configs: ArrayList<DynamicAppConfig> = ArrayList()) {
        deviceSupport.evaluateGBDeviceEvent(GBDeviceEventAppConfig(uuid, event, configs))
    }

    fun onAppConfigRequest(uuid: UUID): GdiSmartProto.Smart {
        LOG.debug("Encoding app config request for {}", uuid)
        return GdiSmartProto.Smart.newBuilder().buildWith {
            appConfigService = GdiAppConfigService.AppConfigService.newBuilder().buildWith {
                appConfigGet = AppConfig.newBuilder().buildWith {
                    appId = ByteString.copyFrom(UuidUtil.toBytes(uuid))
                }
            }
        }
    }

    fun onAppConfigSet(uuid: UUID, configs: List<DynamicAppConfig>): GdiSmartProto.Smart? {
        LOG.debug("Encoding app config set for {} with {} configs", uuid, configs.size)

        val jsonObject = JsonObject()
        for (config in configs) {
            when (config) {
                is AppConfigBoolean -> jsonObject.addProperty(config.key, config.value)
                is AppConfigString -> jsonObject.addProperty(config.key, config.value)
                is AppConfigFloat -> jsonObject.addProperty(config.key, config.value)
                is AppConfigInteger -> jsonObject.addProperty(config.key, config.value)
            }
        }

        val configsBytes: ByteArray
        try {
            configsBytes = GarminJson.encode(jsonObject)
        } catch (e: GarminJsonException) {
            LOG.error("Failed to encode configs to garmin json", e)
            deviceSupport.evaluateGBDeviceEvent(GBDeviceEventAppConfig(uuid, GBDeviceEventAppConfig.Event.APP_CONFIG_SET_FAILED))
            return null
        }

        return GdiSmartProto.Smart.newBuilder().buildWith {
            appConfigService = GdiAppConfigService.AppConfigService.newBuilder().buildWith {
                appConfigSet = AppConfig.newBuilder().buildWith {
                    appId = ByteString.copyFrom(UuidUtil.toBytes(uuid))
                    appConfig = ByteString.copyFrom(configsBytes)
                }
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(AppConfigHandler::class.java)
    }
}
