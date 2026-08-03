package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml

/** Encryption type carried in a DUML frame's flags byte (bits 0-2). */
enum class DumlEncryption(val value: Int) {
    NONE(0x00),
    AES_128(0x01),
    SELF_DEF(0x02),
    XOR(0x03),
    DES_56(0x04),
    DES_112(0x05),
    AES_192(0x06),
    AES_256(0x07),
    ;

    companion object {
        fun fromValue(value: Int): DumlEncryption = entries.find { it.value == value } ?: NONE
    }
}

/** Ack type carried in a DUML frame's flags byte (bits 5-6). */
enum class DumlAck(val value: Int) {
    NONE(0x00),
    ACK_BEFORE_EXEC(0x01),
    ACK_AFTER_EXEC(0x02),
    ;

    companion object {
        fun fromValue(value: Int): DumlAck = entries.find { it.value == value } ?: NONE
    }
}

/** Request/response bit, flags byte bit 7. */
enum class DumlPacketType(val value: Int) {
    REQUEST(0),
    RESPONSE(1);

    companion object {
        fun fromValue(value: Int): DumlPacketType = if (value and 1 == 1) RESPONSE else REQUEST
    }
}
