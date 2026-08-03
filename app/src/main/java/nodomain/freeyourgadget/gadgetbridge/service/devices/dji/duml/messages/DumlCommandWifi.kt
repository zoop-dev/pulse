package nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages

import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlCmdSet
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacketType
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlStrings

/** Commands under cmdSet=WIFI(0x07). */
sealed class Wifi(cmd: Int) : DumlCommand(DumlCmdSet.WIFI, cmd) {
    /**
     * cmd=0x45 "SET_PAIRING_PIN" - Phone sends a hardcoded app-identifier string plus a PIN;
     * device replies with a status byte and a "pairing state" flag. If that flag is 0x01, no further pairing stages
     * are needed - otherwise the app waits for a separate PAIRING_PIN_APPROVED push once a human
     * confirms pairing on the device itself.
     */
    sealed class SetPairingPin : Wifi(CMD) {
        companion object {
            const val CMD = 0x45
        }

        data class Request(val id: String, val pin: String) : SetPairingPin(), DumlEncodable {
            override fun encode(): ByteArray =
                DumlStrings.encodePackedString(id) + DumlStrings.encodePackedString(pin)
        }

        /**
         * [status] (byte 0) is usually 0x00, [pairingState] (byte 1) is 0x01 if already paired,
         * 0x02 if the user must approve the pairing.
         */
        data class Response(
            val status: Int,
            val pairingState: Int,
        ) : SetPairingPin() {
            companion object {
                fun decode(payload: ByteArray): Response {
                    require(payload.size >= 2) { "SetPairingPin response too short: ${payload.size} bytes" }
                    return Response(
                        status = payload[0].toInt() and 0xFF,
                        pairingState = payload[1].toInt() and 0xFF,
                    )
                }
            }
        }
    }

    sealed class PairingPinApproved : Wifi(CMD) {
        companion object {
            const val CMD = 0x46
        }

        data class Request(val status: Int) : PairingPinApproved(), DumlEncodable {
            override fun encode(): ByteArray = byteArrayOf(status.toByte())

            companion object {
                fun decode(payload: ByteArray): Request {
                    require(payload.isNotEmpty()) { "PairingPinApproved response too short: ${payload.size} bytes" }
                    return Request(
                        status = payload[0].toInt() and 0xFF,
                    )
                }
            }
        }
    }
}

internal val WIFI_DECODERS: Map<Pair<Int, DumlPacketType>, (ByteArray) -> DumlCommand> = mapOf(
    (Wifi.SetPairingPin.CMD to DumlPacketType.RESPONSE) to { p -> Wifi.SetPairingPin.Response.decode(p) },
    (Wifi.PairingPinApproved.CMD to DumlPacketType.REQUEST) to { p -> Wifi.PairingPinApproved.Request.decode(p) },
    (Wifi.PairingPinApproved.CMD to DumlPacketType.RESPONSE) to { p -> Wifi.PairingPinApproved.Request.decode(p) },
)
