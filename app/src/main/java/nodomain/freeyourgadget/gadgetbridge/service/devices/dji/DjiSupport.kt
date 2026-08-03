package nodomain.freeyourgadget.gadgetbridge.service.devices.dji

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.os.Bundle
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceState
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLESingleDeviceSupport
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlAck
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlAddress
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlCodec
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlFrameReassembler
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacket
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.DumlPacketType
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages.DumlCommand
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages.DumlEncodable
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages.Wifi
import nodomain.freeyourgadget.gadgetbridge.service.devices.dji.duml.messages.toPacket
import org.slf4j.LoggerFactory
import java.util.UUID


class DjiSupport : AbstractBTLESingleDeviceSupport(LOG) {
    private val reassembler = DumlFrameReassembler()
    private var nextSeq: Int = 0

    init {
        addSupportedService(UUID_SERVICE_DJI)
    }

    override fun useAutoConnect(): Boolean = true

    override fun getDevicePrefs(): DjiPrefs {
        return DjiPrefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.address), gbDevice)
    }

    override fun initializeDevice(builder: TransactionBuilder): TransactionBuilder {
        reassembler.reset()

        builder.setDeviceState(GBDevice.State.INITIALIZING)
        builder.requestMtu(247)
        builder.notify(UUID_CHARACTERISTIC_DJI_FFF4, true)

        sendPacket(
            builder,
            receiver = DumlAddress.WIFI,
            command = Wifi.SetPairingPin.Request(
                devicePrefs.getOrCreatePairingId(),
                devicePrefs.getOrCreatePairingPin(),
            )
        )

        builder.setDeviceState(GBDevice.State.AUTHENTICATING)
        return builder
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Boolean {
        if (UUID_CHARACTERISTIC_DJI_FFF4 == characteristic.uuid) {
            for (packet in reassembler.feed(value)) {
                try {
                    handlePacket(packet)
                } catch (e: Exception) {
                    LOG.error("Failed to handle packet {}", packet, e)
                }
            }
        }

        return super.onCharacteristicChanged(gatt, characteristic, value)
    }

    private fun handlePacket(packet: DumlPacket) {
        when (val cmd = DumlCommand.decode(packet)) {
            is Wifi.PairingPinApproved.Request -> {
                if (cmd.status == 0x01) {
                    evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED))
                    sendPacket(
                        "pairing ack",
                        receiver = DumlAddress.WIFI,
                        seq = packet.seq,
                        packetType = DumlPacketType.RESPONSE,
                        command = Wifi.PairingPinApproved.Request(0x00)
                    )
                } else {
                    LOG.warn("Unexpected pairing pin status {}", cmd.status)
                }
            }

            is Wifi.SetPairingPin.Response -> {
                when (cmd.pairingState) {
                    0x01 -> evaluateGBDeviceEvent(GBDeviceEventUpdateDeviceState(GBDevice.State.INITIALIZED))
                    0x02 -> return // waiting for user to approve
                    else -> LOG.warn("Unexpected pairing state {}", cmd.pairingState)
                }
            }

            else -> LOG.warn("Unhandled DUML command {}", cmd)
        }
    }

    private fun allocateSeq(): Int {
        val s = nextSeq
        nextSeq = (nextSeq + 1) and 0xFFFF
        return s
    }

    private fun sendPacket(taskName: String, packet: DumlPacket) {
        val builder = createTransactionBuilder(taskName)
        sendPacket(builder, packet)
        builder.queue()
    }

    private fun sendPacket(builder: TransactionBuilder, packet: DumlPacket) {
        builder.write(UUID_CHARACTERISTIC_DJI_FFF5, *DumlCodec.encode(packet))
    }

    private fun <T> sendPacket(
        builder: TransactionBuilder,
        sender: DumlAddress = DumlAddress.APP,
        receiver: DumlAddress,
        seq: Int = allocateSeq(),
        ack: DumlAck = DumlAck.ACK_AFTER_EXEC,
        packetType: DumlPacketType = DumlPacketType.REQUEST,
        command: T
    ) where T : DumlCommand, T : DumlEncodable {
        sendPacket(
            builder,
            command.toPacket(sender = sender, receiver = receiver, seq = seq, ack = ack, packetType = packetType)
        )
    }

    private fun <T> sendPacket(
        taskName: String,
        sender: DumlAddress = DumlAddress.APP,
        receiver: DumlAddress,
        seq: Int = allocateSeq(),
        ack: DumlAck = DumlAck.ACK_AFTER_EXEC,
        packetType: DumlPacketType = DumlPacketType.REQUEST,
        command: T
    ) where T : DumlCommand, T : DumlEncodable {
        sendPacket(
            taskName,
            command.toPacket(sender = sender, receiver = receiver, seq = seq, ack = ack, packetType = packetType)
        )
    }

    var testIdx = 0

    override fun onTestNewFunction(options: Bundle?) {
        when (testIdx++) {
            0 -> {
                sendPacket(
                    "test new function 0",
                    DumlPacket(
                        sender = DumlAddress.APP,
                        receiver = DumlAddress.APP,
                        seq = allocateSeq(),
                        packetType = DumlPacketType.RESPONSE,
                        cmdSet = 0,
                        cmd = 0,
                        payload = byteArrayOf(0x00),
                    )
                )
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DjiSupport::class.java)

        private val UUID_SERVICE_DJI: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_DJI_FFF4: UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
        private val UUID_CHARACTERISTIC_DJI_FFF5: UUID = UUID.fromString("0000fff5-0000-1000-8000-00805f9b34fb")
    }
}
