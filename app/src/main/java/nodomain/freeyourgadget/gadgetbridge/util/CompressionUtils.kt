package nodomain.freeyourgadget.gadgetbridge.util

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

object CompressionUtils {
    private val LOG = LoggerFactory.getLogger(GarminSupport::class.java)

    fun inflate(bytes: ByteArray): ByteArray? {
        val inflater = Inflater()
        inflater.setInput(bytes)
        val baosInflated = ByteArrayOutputStream(bytes.size)
        val buf = ByteArray(8096)
        while (!inflater.finished()) {
            try {
                val count = inflater.inflate(buf)
                baosInflated.write(buf, 0, count)
            } catch (e: DataFormatException) {
                LOG.error("Failed to inflate", e)
                return null
            }
        }
        return baosInflated.toByteArray()
    }
}
