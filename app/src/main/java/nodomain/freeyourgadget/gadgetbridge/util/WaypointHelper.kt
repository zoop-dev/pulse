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
package nodomain.freeyourgadget.gadgetbridge.util

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.core.net.toUri
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import nodomain.freeyourgadget.gadgetbridge.BuildConfig
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FitFile
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordData
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileCreator
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitFileId
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages.FitLocation
import java.util.Locale
import kotlin.math.abs

@Parcelize
@Serializable
data class WaypointHelper(
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val elevation: Double?
) : Parcelable {
    fun toUri(): Uri {
        return toString().toUri()
    }

    override fun toString(): String {
        val n = Uri.encode(name?.trim())
        val lat = latitude ?: 0.0
        val long = longitude ?: 0.0
        val ele = elevation

        val format = if (ele != null) {
            if (n != null && n.isNotEmpty()) {
                $$"geo:%1$.6f,%2$.6f,%3$.6f?q=%1$.6f,%2$.6f(%4$s)"
            } else {
                "geo:%1$.6f,%2$.6f,%3$.6f?q=%1$.6f,%2$.6f"
            }
        } else if (lat != 0.0 || long != 0.0) {
            if (n != null && n.isNotEmpty()) {
                $$"geo:%1$.6f,%2$.6f?q=%1$.6f,%2$.6f(%4$s)"
            } else {
                "geo:%1$.6f,%2$.6f?q=%1$.6f,%2$.6f"
            }
        } else {
            $$"geo:0,0?q=%4$s"
        }

        val root = String.format(Locale.ROOT, format, lat, long, ele, n)

        return root
    }

    companion object {
        // can't use named regex groups due too low min API
        private val REGEX_GEO by lazy { Regex("""/([0-9.+-]+),([0-9.+-]+)(?:,([0-9.+-]*))?(?:$|;|[(](.*)[)])""") }
        private val REGEX_GEO_LABEL by lazy { Regex("""^([0-9.+-]+),([0-9.+-]+)(?:,([0-9.+-]*))?(?:[(](.*)[)])?$""") }
        private val REGEX_FIND_GEO by lazy { Regex("""(?:^|\s)(geo:[0-9.+-]+,[0-9.+-]+\S*)""") }
        private val REGEX_OSMAND by lazy { Regex("""osmand[.]net/map[/?]\S*#[0-9]*/([+-]?[0-9.]+)/([+-]?[0-9.]+)""") }
        private val REGEX_GOOGLE by lazy { Regex("""(?:https?://)?(?:maps[.]google[.]\w+/|(?:www[.])?google[.]\w+/maps|goo.gl/maps)\S*(?:[/=]@|[&?](?:query=|query=loc:|center=|viewpoint=|q=loc:|q=))([0-9.+-]*)(?:,|%2C)([0-9.+-]*)""") }

        private val REGEX_DMS by lazy { Regex("""^([NnSs+-])?\s*(\d+)[°ºd:_\s/-]\s*(\d+)[’'′:_\s/-]\s*(\d+(?:[.]\d+)?)[″"¨˝]?\s*([NnSs]?)[/\\|,;\s]*([EeWw+-])?\s*(\d+)[°ºd:_\s/-]\s*(\d+)[’'′:_\s/-]\s*(\d+(?:[.]\d+)?)[″"¨˝]?\s*([EeWw]?)""") }
        private val REGEX_DM by lazy { Regex("""^([NnSs+-])?\s*(\d+)[°ºd:_\s/-]\s*(\d+(?:[.]\d+)?)[’'′:]?\s*([NnSs]?)[/\\|,;\s]*([EeWw+-])?\s*(\d+)[°ºd:_\s/-]\s*(\d+(?:[.]\d+)?)[’'′:]?\s*([EeWw]?)""") }
        private val REGEX_DD by lazy { Regex("""^([NnSs+-])?\s*(\d+[.]\d+)[°º]?\s*([NnSs]?)[/\\|,;\s]*([EeWw+-])?\s*(\d+[.]\d+)[°º]?\s*([EeWw]?)""") }

        fun fromIntent(intent: Intent?): WaypointHelper? {
            if (intent == null) {
                return null
            }

            var uri = intent.data
            if (uri == null) {
                uri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (uri != null) {
                val wh = fromGeoUri(uri)
                if (wh != null) {
                    return wh
                }
            }

            var text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)
            if (text == null) {
                text = intent.getStringExtra(Intent.EXTRA_TEXT)
            }

            return fromText(text)
        }

        // see WaypointHelperTest for example text formats
        fun fromText(text: CharSequence?): WaypointHelper? {
            if (text == null || text.length < 1) {
                return null
            }

            val clean = text.trim().toString()
            val geoMatchResult = REGEX_FIND_GEO.find(clean)
            if(geoMatchResult != null) {
                try {
                    val uriText = geoMatchResult.groupValues[1]
                    val pastedUri = uriText.toUri()
                    val wh = fromGeoUri(pastedUri)
                    if (wh != null) {
                        return wh
                    }
                } catch (_: Exception) {
                    // ignore
                }
            }

            var ns1 = ""
            var ns2 = ""
            var latDeg: Double? = null
            var latMin: Double? = null
            var latSec: Double? = null

            var ew1 = ""
            var ew2 = ""
            var lonDeg: Double? = null
            var lonMin: Double? = null
            var lonSec: Double? = null

            val dmsMatch = REGEX_DMS.matchAt(clean, 0)
            if (dmsMatch != null) {
                val groups = dmsMatch.groupValues
                ns1 = groups[1]
                latDeg = groups[2].toDoubleOrNull()
                latMin = groups[3].toDoubleOrNull()
                latSec = groups[4].toDoubleOrNull()
                ns2 = groups[5]

                ew1 = groups[6]
                lonDeg = groups[7].toDoubleOrNull()
                lonMin = groups[8].toDoubleOrNull()
                lonSec = groups[9].toDoubleOrNull()
                ew2 = groups[10]
            } else {
                val dmMatch = REGEX_DM.matchAt(clean, 0)
                if (dmMatch != null) {
                    val groups = dmMatch.groupValues
                    ns1 = groups[1]
                    latDeg = groups[2].toDoubleOrNull()
                    latMin = groups[3].toDoubleOrNull()
                    ns2 = groups[4]

                    ew1 = groups[5]
                    lonDeg = groups[6].toDoubleOrNull()
                    lonMin = groups[7].toDoubleOrNull()
                    ew2 = groups[8]
                } else {
                    val ddMatch = REGEX_DD.matchAt(clean, 0)
                    if (ddMatch != null) {
                        val groups = ddMatch.groupValues
                        ns1 = groups[1]
                        latDeg = groups[2].toDoubleOrNull()
                        ns2 = groups[3]

                        ew1 = groups[4]
                        lonDeg = groups[5].toDoubleOrNull()
                        ew2 = groups[6]
                    }
                }
            }

            if (latDeg == null || lonDeg == null) {
                val osmand = REGEX_OSMAND.find(clean)
                if (osmand != null) {
                    latDeg = osmand.groupValues[1].toDoubleOrNull();
                    lonDeg = osmand.groupValues[2].toDoubleOrNull()
                }
            }

            if (latDeg == null || lonDeg == null) {
                val google = REGEX_GOOGLE.find(clean)
                if (google != null) {
                    latDeg = google.groupValues[1].toDoubleOrNull();
                    lonDeg = google.groupValues[2].toDoubleOrNull()
                }
            }

            if (latDeg == null || lonDeg == null) {
                return null
            }

            var lat: Double = latDeg
            if (latMin != null) {
                lat += latMin / 60.0
            }
            if (latSec != null) {
                lat += latSec / 3600.0
            }
            if (ns1.length == 1 && "Ss-".contains(ns1)) {
                lat *= -1.0
            } else if (ns2.length == 1 && "Ss-".contains(ns2)) {
                lat *= -1.0
            }

            var lon: Double = lonDeg
            if (lonMin != null) {
                lon += lonMin / 60.0
            }
            if (lonSec != null) {
                lon += lonSec / 3600.0
            }
            if (ew1.length == 1 && "Ww-".contains(ew1)) {
                lon *= -1.0
            } else if (ew2.length == 1 && "Ww-".contains(ew2)) {
                lon *= -1.0
            }

            return WaypointHelper(null, lat, lon, null)
        }

        // see WaypointHelperTest for example Uris
        fun fromGeoUri(uri: Uri?): WaypointHelper? {
            if (uri == null) {
                return null
            }
            if (!"geo".contentEquals(uri.scheme)) {
                return null
            }

            // Uri class has some rather strange limitations ...
            val cleanUri = ("http://dummy/" + uri.encodedSchemeSpecificPart).toUri()

            var label: String? = null
            var lat: Double? = null
            var long: Double? = null
            var ele: Double? = null

            val path = cleanUri.path
            if (!(path.isNullOrEmpty() || "/".contentEquals(path))) {
                val geoMatch = REGEX_GEO.matchAt(path, 0)
                if (geoMatch != null) {
                    val geoGroups = geoMatch.groupValues

                    lat = geoGroups[1].toDoubleOrNull()
                    long = geoGroups[2].toDoubleOrNull()
                    ele = geoGroups[3].toDoubleOrNull()
                    label = geoGroups[4]
                }
            }

            if (label.isNullOrEmpty()) {
                label = cleanUri.getQueryParameter("q")?.trim()
                if (!label.isNullOrEmpty()) {
                    val labelMatch = REGEX_GEO_LABEL.matchEntire(label)

                    if (labelMatch != null) {
                        val labelGroups = labelMatch.groupValues

                        val latLabel = labelGroups[1].toDoubleOrNull()
                        val longLabel = labelGroups[2].toDoubleOrNull()
                        val eleLabel = labelGroups[3].toDoubleOrNull()
                        label = labelGroups[4].trim()

                        if ((latLabel == null || !latLabel.isFinite()) && (longLabel == null || !longLabel.isFinite())) {
                            // ignore
                        } else if (latLabel != 0.0 || longLabel != 0.0 || eleLabel != 0.0) {
                            lat = latLabel
                            long = longLabel
                            if (eleLabel != null) {
                                ele = eleLabel
                            }
                        }
                    }
                }
            }

            if (label.isNullOrEmpty()) {
                // fun label encoding
                // geo:37.78918,-122.40335?z=14&(Wikimedia+Foundation)
                for (name in cleanUri.queryParameterNames) {
                    if (name.startsWith('(') && name.endsWith(')')) {
                        label = Uri.decode(name.substring(1, name.length - 1))
                        break
                    }
                }
            }

            if (lat?.isFinite() == true || long?.isFinite() == true || ele?.isFinite() == true || !label.isNullOrEmpty()) {
                return WaypointHelper(label, lat, long, ele)
            }
            return null
        }

        fun formatPosition(latitude: Double?, longitude: Double?): Array<String>? {
            if (latitude == null || longitude == null) {
                return null
            } else if (!latitude.isFinite() || !longitude.isFinite()) {
                return null
            }

            var x = abs(latitude)
            var y = abs(longitude)
            val lats = x
            val longs = y

            val latDeg = x.toInt()
            val longDeg = y.toInt()
            x = (x - latDeg) * 60.0
            y = (y - longDeg) * 60.0

            val latMins = x
            val longMins = y
            val latMin = x.toInt()
            val longMin = y.toInt()
            x = (x - latMin) * 60
            y = (y - longMin) * 60

            val latSec = x
            val longSec = y

            val ns = if (latitude < 0.0) {
                'S'
            } else {
                'N'
            }

            val ew = if (longitude < 0.0) {
                'W'
            } else {
                'E'
            }

            val dms = String.format(
                Locale.ROOT,
                "%d° %02d′ %04.1f″ %s, %d° %02d′ %04.1f″ %s",
                latDeg,
                latMin,
                latSec,
                ns,
                longDeg,
                longMin,
                longSec,
                ew
            )

            val dm = String.format(
                Locale.ROOT,
                "%d° %06.3f′ %s, %d° %06.3f′ %s",
                latDeg,
                latMins,
                ns,
                longDeg,
                longMins,
                ew
            )

            val dh = String.format(
                Locale.ROOT, "%.6f°%s %.6f°%s", lats, ns, longs, ew
            )

            val dd = String.format(
                Locale.ROOT, "%.6f; %.6f", latitude, longitude
            )

            val formated = arrayOf(
                dms,
                dm,
                dh,
                dd,
            )

            return formated
        }

        fun generateFitLocationFile(
            waypoint: WaypointHelper
        ): FitFile {
            val timestamp = (System.currentTimeMillis() / 1000L)
            val version = BuildConfig.VERSION_CODE
            val waypoints = Array(1) { waypoint }
            return generateFitLocationFile(waypoints, timestamp, version)
        }

        fun generateFitLocationFile(
            waypoints: Array<WaypointHelper>, timestamp: Long, softwareVersion: Int
        ): FitFile {
            val dataRecords: MutableList<RecordData?> = ArrayList(2 + waypoints.size)

            dataRecords.add(
                FitFileId.Builder()
                    .setSerialNumber(1L)
                    .setTimeCreated(timestamp)
                    .setManufacturer(1) // Garmin
                    .setProduct(65534) // Connect
                    .setNumber(1)
                    .setType(FileType.FILETYPE.LOCATION)
                    .setProductName("Gadgetbridge")
                    .build()
            )

            dataRecords.add(
                FitFileCreator.Builder()
                    .setSoftwareVersion(softwareVersion)
                    .build()
            )

            var messageIndex = 0
            for (waypoint in waypoints) {
                dataRecords.add(
                    FitLocation.Builder()
                        .setTimestamp(timestamp)
                        .setName(waypoint.name)
                        .setPositionLat(waypoint.latitude)
                        .setPositionLong(waypoint.longitude)
                        .setMessageIndex(messageIndex++)
                        .setAltitude(waypoint.elevation?.toFloat())
                        .build()
                )
            }

            return FitFile(dataRecords)
        }
    }

}