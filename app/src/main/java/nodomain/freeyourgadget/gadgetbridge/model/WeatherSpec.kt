/*  Copyright (C) 2016-2024 Andreas Shimokawa, Arjan Schrijver, beardhatcode,
    Carsten Pfeiffer, Daniele Gobbetti, Enrico Brambilla, José Rebelo, Taavi
    Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.model

import android.location.Location
import android.os.Parcel
import android.os.Parcelable

// FIXME: document me and my fields, including units
/**
 * The package for this class must not change, since that is used by external apps such as
 * Tiny Weather Forecast Germany.
 */
class WeatherSpec() : Parcelable {
    var timestamp: Int = 0 // unix epoch timestamp, in seconds
    var location: String? = null
    var currentTemp: Int = 0 // kelvin
    var currentConditionCode: Int = 3200 // OpenWeatherMap condition code
    var currentCondition: String? = null
    var currentHumidity: Int = 0
    var todayMaxTemp: Int = 0 // kelvin
    var todayMinTemp: Int = 0 // kelvin
    var windSpeed: Float = 0f // km per hour
    var windDirection: Int = 0 // deg
    var uvIndex: Float = 0f // 0.0 to 15.0
    var precipProbability: Int = 0 // %
    var dewPoint: Int = 0 // kelvin
    var pressure: Float = 0f // mb
    var cloudCover: Int = 0 // %
    var visibility: Float = 0f // m
    var sunRise: Int = 0 // unix epoch timestamp, in seconds
    var sunSet: Int = 0 // unix epoch timestamp, in seconds
    var moonRise: Int = 0 // unix epoch timestamp, in seconds
    var moonSet: Int = 0 // unix epoch timestamp, in seconds
    var moonPhase: Int = 0 // deg [0, 360[
    var latitude: Float = 0f
    var longitude: Float = 0f
    var feelsLikeTemp: Int = 0 // kelvin
    var isCurrentLocation: Int = -1 // 0 for false, 1 for true, -1 for unknown
    var airQuality: AirQuality? = null

    // Forecasts from the next day onward, in chronological order, one entry per day.
    // It should not include the current or previous days
    var forecasts: ArrayList<Daily?> = ArrayList()

    // Hourly forecasts
    var hourly: ArrayList<Hourly?> = ArrayList()

    constructor(parcel: Parcel) : this() {
        val version = parcel.readInt()
        if (version >= 2) {
            timestamp = parcel.readInt()
            location = parcel.readString()
            currentTemp = parcel.readInt()
            currentConditionCode = parcel.readInt()
            currentCondition = parcel.readString()
            currentHumidity = parcel.readInt()
            todayMaxTemp = parcel.readInt()
            todayMinTemp = parcel.readInt()
            windSpeed = parcel.readFloat()
            windDirection = parcel.readInt()
            if (version < 4) {
                // Deserialize the old Forecast list and convert them to Daily
                val oldForecasts = ArrayList<Forecast>()
                parcel.readList(oldForecasts, Forecast::class.java.classLoader)
                for (forecast in oldForecasts) {
                    val d = Daily()
                    d.minTemp = forecast.minTemp
                    d.maxTemp = forecast.maxTemp
                    d.conditionCode = forecast.conditionCode
                    d.humidity = forecast.humidity
                    forecasts.add(d)
                }
            } else {
                parcel.readList(forecasts, Daily::class.java.classLoader)
            }
        }
        if (version >= 3) {
            uvIndex = parcel.readFloat()
            precipProbability = parcel.readInt()
        }
        if (version >= 4) {
            dewPoint = parcel.readInt()
            pressure = parcel.readFloat()
            cloudCover = parcel.readInt()
            visibility = parcel.readFloat()
            sunRise = parcel.readInt()
            sunSet = parcel.readInt()
            moonRise = parcel.readInt()
            moonSet = parcel.readInt()
            moonPhase = parcel.readInt()
            latitude = parcel.readFloat()
            longitude = parcel.readFloat()
            feelsLikeTemp = parcel.readInt()
            isCurrentLocation = parcel.readInt()
            airQuality = parcel.readParcelable(
                AirQuality::class.java.classLoader
            )
            parcel.readList(hourly, Hourly::class.java.classLoader)
        }
    }

    fun windSpeedAsBeaufort(): Int = toBeaufort(this.windSpeed)

    fun getIsCurrentLocation(): Int = isCurrentLocation

    fun setIsCurrentLocation(currLoc: Int) {
        isCurrentLocation = currLoc
    }

    fun getLocationObject(): Location? {
        return if (latitude == 0f && longitude == 0f) null
        else Location("weatherSpec").apply {
            latitude = this@WeatherSpec.latitude.toDouble()
            longitude = this@WeatherSpec.longitude.toDouble()
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(VERSION)
        dest.writeInt(timestamp)
        dest.writeString(location)
        dest.writeInt(currentTemp)
        dest.writeInt(currentConditionCode)
        dest.writeString(currentCondition)
        dest.writeInt(currentHumidity)
        dest.writeInt(todayMaxTemp)
        dest.writeInt(todayMinTemp)
        dest.writeFloat(windSpeed)
        dest.writeInt(windDirection)
        dest.writeList(forecasts)
        dest.writeFloat(uvIndex)
        dest.writeInt(precipProbability)
        dest.writeInt(dewPoint)
        dest.writeFloat(pressure)
        dest.writeInt(cloudCover)
        dest.writeFloat(visibility)
        dest.writeInt(sunRise)
        dest.writeInt(sunSet)
        dest.writeInt(moonRise)
        dest.writeInt(moonSet)
        dest.writeInt(moonPhase)
        dest.writeFloat(latitude)
        dest.writeFloat(longitude)
        dest.writeInt(feelsLikeTemp)
        dest.writeInt(isCurrentLocation)
        dest.writeParcelable(airQuality, 0)
        dest.writeList(hourly)
    }

    /**
     * Convert the current day's forecast to a [Daily] object.
     */
    fun todayAsDaily(): Daily = Daily().apply {
        minTemp = todayMinTemp
        maxTemp = todayMaxTemp
        conditionCode = currentConditionCode
        humidity = currentHumidity
        windSpeed = this@WeatherSpec.windSpeed
        windDirection = this@WeatherSpec.windDirection
        uvIndex = this@WeatherSpec.uvIndex
        precipProbability = this@WeatherSpec.precipProbability
        sunRise = this@WeatherSpec.sunRise
        sunSet = this@WeatherSpec.sunSet
        moonRise = this@WeatherSpec.moonRise
        moonSet = this@WeatherSpec.moonSet
        moonPhase = this@WeatherSpec.moonPhase
        airQuality = this@WeatherSpec.airQuality
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WeatherSpec

        if (timestamp != other.timestamp) return false
        if (currentTemp != other.currentTemp) return false
        if (currentConditionCode != other.currentConditionCode) return false
        if (currentHumidity != other.currentHumidity) return false
        if (todayMaxTemp != other.todayMaxTemp) return false
        if (todayMinTemp != other.todayMinTemp) return false
        if (windSpeed != other.windSpeed) return false
        if (windDirection != other.windDirection) return false
        if (uvIndex != other.uvIndex) return false
        if (precipProbability != other.precipProbability) return false
        if (dewPoint != other.dewPoint) return false
        if (pressure != other.pressure) return false
        if (cloudCover != other.cloudCover) return false
        if (visibility != other.visibility) return false
        if (sunRise != other.sunRise) return false
        if (sunSet != other.sunSet) return false
        if (moonRise != other.moonRise) return false
        if (moonSet != other.moonSet) return false
        if (moonPhase != other.moonPhase) return false
        if (latitude != other.latitude) return false
        if (longitude != other.longitude) return false
        if (feelsLikeTemp != other.feelsLikeTemp) return false
        if (isCurrentLocation != other.isCurrentLocation) return false
        if (location != other.location) return false
        if (currentCondition != other.currentCondition) return false
        if (airQuality != other.airQuality) return false
        if (forecasts != other.forecasts) return false
        if (hourly != other.hourly) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp
        result = 31 * result + currentTemp
        result = 31 * result + currentConditionCode
        result = 31 * result + currentHumidity
        result = 31 * result + todayMaxTemp
        result = 31 * result + todayMinTemp
        result = 31 * result + windSpeed.hashCode()
        result = 31 * result + windDirection
        result = 31 * result + uvIndex.hashCode()
        result = 31 * result + precipProbability
        result = 31 * result + dewPoint
        result = 31 * result + pressure.hashCode()
        result = 31 * result + cloudCover
        result = 31 * result + visibility.hashCode()
        result = 31 * result + sunRise
        result = 31 * result + sunSet
        result = 31 * result + moonRise
        result = 31 * result + moonSet
        result = 31 * result + moonPhase
        result = 31 * result + latitude.hashCode()
        result = 31 * result + longitude.hashCode()
        result = 31 * result + feelsLikeTemp
        result = 31 * result + isCurrentLocation
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + (currentCondition?.hashCode() ?: 0)
        result = 31 * result + (airQuality?.hashCode() ?: 0)
        result = 31 * result + forecasts.hashCode()
        result = 31 * result + hourly.hashCode()
        return result
    }

    @Deprecated("Kept for backwards compatibility with old weather apps")
    class Forecast() : Parcelable {
        var minTemp: Int = 0 // Kelvin
        var maxTemp: Int = 0 // Kelvin
        var conditionCode: Int = 0 // OpenWeatherMap condition code
        var humidity: Int = 0

        internal constructor(parcel: Parcel) : this() {
            minTemp = parcel.readInt()
            maxTemp = parcel.readInt()
            conditionCode = parcel.readInt()
            humidity = parcel.readInt()
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(minTemp)
            dest.writeInt(maxTemp)
            dest.writeInt(conditionCode)
            dest.writeInt(humidity)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Forecast

            if (minTemp != other.minTemp) return false
            if (maxTemp != other.maxTemp) return false
            if (conditionCode != other.conditionCode) return false
            if (humidity != other.humidity) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minTemp
            result = 31 * result + maxTemp
            result = 31 * result + conditionCode
            result = 31 * result + humidity
            return result
        }

        companion object {
            const val VERSION = 1

            @JvmField
            val CREATOR: Parcelable.Creator<Forecast> = object : Parcelable.Creator<Forecast> {
                override fun createFromParcel(parcel: Parcel): Forecast = Forecast(parcel)
                override fun newArray(size: Int): Array<Forecast?> = arrayOfNulls(size)
            }
        }
    }

    class AirQuality : Parcelable {
        var aqi: Int =
            -1 // Air Quality Index - usually the max across all AQI values for pollutants
        var co: Float = -1f // Carbon Monoxide, mg/m^3
        var no2: Float = -1f // Nitrogen Dioxide, ug/m^3
        var o3: Float = -1f // Ozone, ug/m^3
        var pm10: Float = -1f // Particulate Matter, 10 microns or less in diameter, ug/m^3
        var pm25: Float = -1f // Particulate Matter, 2.5 microns or less in diameter, ug/m^3
        var so2: Float = -1f // Sulphur Dioxide, ug/m^3

        // Air Quality Index values per pollutant
        // These are expected to be in the Plume scale (see https://plumelabs.files.wordpress.com/2023/06/plume_aqi_2023.pdf)
        // Some apps such as Breezy Weather fallback to the WHO 2021 AQI for pollutants that are not mapped in the Plume AQI
        // https://www.who.int/news-room/fact-sheets/detail/ambient-(outdoor)-air-quality-and-health
        //
        // Breezy Weather implementation for reference:
        // - https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/common/basic/models/weather/AirQuality.kt
        // - https://github.com/breezy-weather/breezy-weather/blob/main/app/src/main/java/org/breezyweather/common/basic/models/options/index/PollutantIndex.kt
        var coAqi: Int = -1
        var no2Aqi: Int = -1
        var o3Aqi: Int = -1
        var pm10Aqi: Int = -1
        var pm25Aqi: Int = -1
        var so2Aqi: Int = -1

        constructor()

        internal constructor(parcel: Parcel) {
            parcel.readInt() // version
            aqi = parcel.readInt()
            co = parcel.readFloat()
            no2 = parcel.readFloat()
            o3 = parcel.readFloat()
            pm10 = parcel.readFloat()
            pm25 = parcel.readFloat()
            so2 = parcel.readFloat()
            coAqi = parcel.readInt()
            no2Aqi = parcel.readInt()
            o3Aqi = parcel.readInt()
            pm10Aqi = parcel.readInt()
            pm25Aqi = parcel.readInt()
            so2Aqi = parcel.readInt()
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(VERSION)
            dest.writeInt(aqi)
            dest.writeFloat(co)
            dest.writeFloat(no2)
            dest.writeFloat(o3)
            dest.writeFloat(pm10)
            dest.writeFloat(pm25)
            dest.writeFloat(so2)
            dest.writeInt(coAqi)
            dest.writeInt(no2Aqi)
            dest.writeInt(o3Aqi)
            dest.writeInt(pm10Aqi)
            dest.writeInt(pm25Aqi)
            dest.writeInt(so2Aqi)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AirQuality

            if (aqi != other.aqi) return false
            if (co != other.co) return false
            if (no2 != other.no2) return false
            if (o3 != other.o3) return false
            if (pm10 != other.pm10) return false
            if (pm25 != other.pm25) return false
            if (so2 != other.so2) return false
            if (coAqi != other.coAqi) return false
            if (no2Aqi != other.no2Aqi) return false
            if (o3Aqi != other.o3Aqi) return false
            if (pm10Aqi != other.pm10Aqi) return false
            if (pm25Aqi != other.pm25Aqi) return false
            if (so2Aqi != other.so2Aqi) return false

            return true
        }

        override fun hashCode(): Int {
            var result = aqi
            result = 31 * result + co.hashCode()
            result = 31 * result + no2.hashCode()
            result = 31 * result + o3.hashCode()
            result = 31 * result + pm10.hashCode()
            result = 31 * result + pm25.hashCode()
            result = 31 * result + so2.hashCode()
            result = 31 * result + coAqi
            result = 31 * result + no2Aqi
            result = 31 * result + o3Aqi
            result = 31 * result + pm10Aqi
            result = 31 * result + pm25Aqi
            result = 31 * result + so2Aqi
            return result
        }

        companion object {
            const val VERSION = 1

            @JvmField
            val CREATOR: Parcelable.Creator<AirQuality> = object : Parcelable.Creator<AirQuality> {
                override fun createFromParcel(parcel: Parcel): AirQuality = AirQuality(parcel)
                override fun newArray(size: Int): Array<AirQuality?> = arrayOfNulls(size)
            }
        }
    }

    class Daily() : Parcelable {
        var minTemp: Int = 0 // Kelvin
        var maxTemp: Int = 0 // Kelvin
        var conditionCode: Int = 0 // OpenWeatherMap condition code
        var humidity: Int = 0
        var windSpeed: Float = 0f // km per hour
        var windDirection: Int = 0 // deg
        var uvIndex: Float = 0f // 0.0 to 15.0
        var precipProbability: Int = 0 // %
        var sunRise: Int = 0
        var sunSet: Int = 0
        var moonRise: Int = 0
        var moonSet: Int = 0
        var moonPhase: Int = 0
        var airQuality: AirQuality? = null


        internal constructor(parcel: Parcel) : this() {
            parcel.readInt() // version
            minTemp = parcel.readInt()
            maxTemp = parcel.readInt()
            conditionCode = parcel.readInt()
            humidity = parcel.readInt()
            windSpeed = parcel.readFloat()
            windDirection = parcel.readInt()
            uvIndex = parcel.readFloat()
            precipProbability = parcel.readInt()
            sunRise = parcel.readInt()
            sunSet = parcel.readInt()
            moonRise = parcel.readInt()
            moonSet = parcel.readInt()
            moonPhase = parcel.readInt()
            airQuality = parcel.readParcelable(
                AirQuality::class.java.classLoader
            )
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(VERSION)
            dest.writeInt(minTemp)
            dest.writeInt(maxTemp)
            dest.writeInt(conditionCode)
            dest.writeInt(humidity)
            dest.writeFloat(windSpeed)
            dest.writeInt(windDirection)
            dest.writeFloat(uvIndex)
            dest.writeInt(precipProbability)
            dest.writeInt(sunRise)
            dest.writeInt(sunSet)
            dest.writeInt(moonRise)
            dest.writeInt(moonSet)
            dest.writeInt(moonPhase)
            dest.writeParcelable(airQuality, 0)
        }

        fun windSpeedAsBeaufort(): Int {
            return toBeaufort(this.windSpeed)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Daily

            if (minTemp != other.minTemp) return false
            if (maxTemp != other.maxTemp) return false
            if (conditionCode != other.conditionCode) return false
            if (humidity != other.humidity) return false
            if (windSpeed != other.windSpeed) return false
            if (windDirection != other.windDirection) return false
            if (uvIndex != other.uvIndex) return false
            if (precipProbability != other.precipProbability) return false
            if (sunRise != other.sunRise) return false
            if (sunSet != other.sunSet) return false
            if (moonRise != other.moonRise) return false
            if (moonSet != other.moonSet) return false
            if (moonPhase != other.moonPhase) return false
            if (airQuality != other.airQuality) return false

            return true
        }

        override fun hashCode(): Int {
            var result = minTemp
            result = 31 * result + maxTemp
            result = 31 * result + conditionCode
            result = 31 * result + humidity
            result = 31 * result + windSpeed.hashCode()
            result = 31 * result + windDirection
            result = 31 * result + uvIndex.hashCode()
            result = 31 * result + precipProbability
            result = 31 * result + sunRise
            result = 31 * result + sunSet
            result = 31 * result + moonRise
            result = 31 * result + moonSet
            result = 31 * result + moonPhase
            result = 31 * result + (airQuality?.hashCode() ?: 0)
            return result
        }

        companion object {
            const val VERSION = 1

            @JvmField
            val CREATOR: Parcelable.Creator<Daily> = object : Parcelable.Creator<Daily> {
                override fun createFromParcel(parcel: Parcel): Daily = Daily(parcel)
                override fun newArray(size: Int): Array<Daily?> = arrayOfNulls(size)
            }
        }
    }

    class Hourly() : Parcelable {
        var timestamp: Int = 0 // unix epoch timestamp, in seconds
        var temp: Int = 0 // Kelvin
        var conditionCode: Int = 0 // OpenWeatherMap condition code
        var humidity: Int = 0
        var windSpeed: Float = 0f // km per hour
        var windDirection: Int = 0 // deg
        var uvIndex: Float = 0f // 0.0 to 15.0
        var precipProbability: Int = 0 // %


        internal constructor(parcel: Parcel) : this() {
            parcel.readInt() // version
            timestamp = parcel.readInt()
            temp = parcel.readInt()
            conditionCode = parcel.readInt()
            humidity = parcel.readInt()
            windSpeed = parcel.readFloat()
            windDirection = parcel.readInt()
            uvIndex = parcel.readFloat()
            precipProbability = parcel.readInt()
        }

        override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(VERSION)
            dest.writeInt(timestamp)
            dest.writeInt(temp)
            dest.writeInt(conditionCode)
            dest.writeInt(humidity)
            dest.writeFloat(windSpeed)
            dest.writeInt(windDirection)
            dest.writeFloat(uvIndex)
            dest.writeInt(precipProbability)
        }

        fun windSpeedAsBeaufort(): Int {
            return toBeaufort(this.windSpeed)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Hourly

            if (timestamp != other.timestamp) return false
            if (temp != other.temp) return false
            if (conditionCode != other.conditionCode) return false
            if (humidity != other.humidity) return false
            if (windSpeed != other.windSpeed) return false
            if (windDirection != other.windDirection) return false
            if (uvIndex != other.uvIndex) return false
            if (precipProbability != other.precipProbability) return false

            return true
        }

        override fun hashCode(): Int {
            var result = timestamp
            result = 31 * result + temp
            result = 31 * result + conditionCode
            result = 31 * result + humidity
            result = 31 * result + windSpeed.hashCode()
            result = 31 * result + windDirection
            result = 31 * result + uvIndex.hashCode()
            result = 31 * result + precipProbability
            return result
        }

        companion object {
            const val VERSION = 1

            @JvmField
            val CREATOR: Parcelable.Creator<Hourly> = object : Parcelable.Creator<Hourly> {
                override fun createFromParcel(parcel: Parcel): Hourly = Hourly(parcel)
                override fun newArray(size: Int): Array<Hourly?> = arrayOfNulls(size)
            }
        }
    }

    companion object {
        const val VERSION: Int = 4

        @JvmField
        val CREATOR: Parcelable.Creator<WeatherSpec> = object : Parcelable.Creator<WeatherSpec> {
            override fun createFromParcel(parcel: Parcel): WeatherSpec = WeatherSpec(parcel)
            override fun newArray(size: Int): Array<WeatherSpec?> = arrayOfNulls(size)
        }

        // Lower bounds of beaufort regions 1 to 12
        // Values from https://en.wikipedia.org/wiki/Beaufort_scale
        private val beaufort = floatArrayOf(2f, 6f, 12f, 20f, 29f, 39f, 50f, 62f, 75f, 89f, 103f, 118f)

        //                                    level: 0 1  2   3   4   5   6   7   8   9   10   11   12
        fun toBeaufort(speed: Float): Int {
            var level = 0
            while (level < beaufort.size && beaufort[level] < speed) {
                level++
            }
            return level
        }
    }
}