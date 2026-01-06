package nodomain.freeyourgadget.gadgetbridge.service.devices.onetouch

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

sealed class OneTouchMessage {
    object ThresholdHighGet : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            return byteArrayOf(0x0a, 0x02, 0x0a)
        }
    }

    data class ThresholdHighRet(val threshold: Int) : OneTouchMessage()

    data class ThresholdHighSet(val threshold: Int) : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            val buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(byteArrayOf(0x0a, 0x01, 0x0a))
            buf.putInt(threshold)
            return buf.array()
        }
    }

    object ThresholdLowGet : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            return byteArrayOf(0x0a, 0x02, 0x09)
        }
    }

    data class ThresholdLowRet(val threshold: Int) : OneTouchMessage()

    data class ThresholdLowSet(val threshold: Int) : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            val buf = ByteBuffer.allocate(7).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(byteArrayOf(0x0a, 0x01, 0x09))
            buf.putInt(threshold)
            return buf.array()
        }
    }

    object ReadingCountGet : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            return byteArrayOf(0x27, 0x00)
        }
    }

    data class ReadingCountRet(val count: Short) : OneTouchMessage()

    data class ReadingGet(val offset: Short) : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            val buf = ByteBuffer.allocate(5).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(byteArrayOf(0x31, 0x02))
            buf.putShort(offset)
            buf.put(0x00)
            return buf.array()
        }
    }

    data class ReadingRet(val offset: Int, val index: Int, val timestampMillis: Long, val value: Int) :
        OneTouchMessage()

    object TimeGet : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            return byteArrayOf(0x20, 0x02)
        }
    }

    data class TimeRet(val timestampMillis: Long) : OneTouchMessage()

    data class TimeSet(val timestampMillis: Long) : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            val buf = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN)
            buf.put(byteArrayOf(0x20, 0x01))
            buf.putInt(millisToOneTouchEpoch(timestampMillis))
            return buf.array()
        }
    }

    data class ValueRet(private val payload: ByteArray) : OneTouchMessage() {
        override fun encodePayload(): ByteArray {
            return payload
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ValueRet

            return payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            return payload.contentHashCode()
        }
    }

    open fun encodePayload(): ByteArray {
        throw IllegalAccessException("encodePayload() not implemented")
    }

    fun encode(): ByteArray {
        val payload = encodePayload()
        val packetSize = payload.size + 8
        if (packetSize > 20) {
            // Does not fit the MTU, and we do not support chunking
            throw IllegalStateException("Packet too long, and chunks are not supported")
        }

        val buf = ByteBuffer.allocate(packetSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0x01)
        buf.put(0x02)
        buf.putShort((buf.limit() - 1).toShort())
        if (this is ThresholdLowSet || this is ThresholdHighSet) {
            buf.put(0x03)
        } else {
            buf.put(0x04)
        }
        buf.put(payload)
        buf.put(0x03)
        buf.putShort(CheckSums.crc16_ccitt(buf.array(), 1, buf.limit() - 2, 0xffff).toShort())

        return buf.array()
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(OneTouchMessage::class.java)

        fun oneTouchEpochToMillis(timestamp: Int): Long {
            return (timestamp + EPOCH_OFFSET_SECS) * 1000L
        }

        fun millisToOneTouchEpoch(dateMillis: Long): Int {
            return (dateMillis / 1000).toInt() - EPOCH_OFFSET_SECS
        }

        // OneTouch uses 2000-01-01 00:00:00 UTC as epoch
        private const val EPOCH_OFFSET_SECS = 946684800

        fun decode(packet: ByteArray, previousMessage: OneTouchMessage): OneTouchMessage? {
            if (packet.size < 8) {
                LOG.warn("Packet too short: {} bytes", packet.size)
                return null
            }

            if (packet[1] != 2.toByte()) {
                LOG.error("Expected 2 at position 1, got {}", packet[1])
                return null
            }

            val length = BLETypeConversions.toUint16(packet, 2)
            if (packet.size < length + 1) {
                LOG.warn("Packet shorter than advertised length")
                return null
            }

            if (packet[4] != 3.toByte() && packet[4] != 4.toByte()) {
                LOG.error("Expected 3 or 4 at position 4, got {}", packet[4])
                return null
            }

            if (packet[packet.size - 3] != 3.toByte()) {
                LOG.error("Expected 3 before checksum, got {}", packet[packet.size - 2])
                return null
            }

            val computedCrc = CheckSums.crc16_ccitt(packet, 1, packet.size - 2, 0xffff)
            val packetCrc = BLETypeConversions.toUint16(packet, packet.size - 2)
            if (computedCrc != packetCrc) {
                LOG.error("Invalid checksum, expected {}, computed {}", packetCrc, computedCrc)
                return null
            }

            val payload = packet.copyOfRange(5, packet.size - 3)

            val command = payload[0].toInt() and 0xFF
            if (command != 0x06) {
                LOG.warn("Unknown command 0x{}", command.toHexString())
                return null
            }

            when (previousMessage) {
                is TimeGet -> {
                    if (payload.size == 5) {
                        val timestamp = BLETypeConversions.toUint32(payload, 1)
                        return TimeRet(oneTouchEpochToMillis(timestamp))
                    }
                }

                is ReadingGet -> {
                    if (payload.size == 17) {
                        val offset = BLETypeConversions.toUint16(payload, 1)
                        val index = BLETypeConversions.toUint16(payload, 4)
                        val timestamp = BLETypeConversions.toUint32(payload, 6)
                        val value = BLETypeConversions.toUint16(payload, 10)
                        // ? 5 unknown bytes after the value

                        val date = oneTouchEpochToMillis(timestamp)

                        return ReadingRet(offset, index, date, value)
                    }
                }

                is ReadingCountGet -> {
                    if (payload.size == 3) {
                        val count = BLETypeConversions.toUint16(payload, 1)
                        return ReadingCountRet(count.toShort())
                    }
                }

                is ThresholdLowGet -> {
                    if (payload.size == 5) {
                        val limit = BLETypeConversions.toUint32(payload, 1)
                        return ThresholdLowRet(limit)
                    }
                }

                is ThresholdHighGet -> {
                    if (payload.size == 5) {
                        val limit = BLETypeConversions.toUint32(payload, 1)
                        return ThresholdHighRet(limit)
                    }
                }

                else -> {
                    LOG.error("Unknown previous message {}", previousMessage)
                    return null
                }
            }

            LOG.error("Unexpected payload size as response for {}", previousMessage)
            return null
        }
    }
}
