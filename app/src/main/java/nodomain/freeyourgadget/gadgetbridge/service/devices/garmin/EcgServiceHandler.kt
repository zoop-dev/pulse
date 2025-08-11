package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiEcgService
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File

class EcgServiceHandler(val deviceSupport: GarminSupport) {
    fun handle(ecgService: GdiEcgService.EcgService): GdiEcgService.EcgService? {
        if (true) {
            LOG.error("EcgServiceHandler is disabled")
            return null
        }

        if (!ecgService.hasFileTransfer()) {
            LOG.warn("No file transfer in ecg service")
            return null
        }
        val fileTransfer = ecgService.fileTransfer
        return when {
            fileTransfer.hasTransferStartReq() -> handleTransferStartReq(fileTransfer.transferStartReq)
            fileTransfer.hasEcgFile() -> handleEcgFile(fileTransfer.ecgFile)
            else -> {
                LOG.warn("Unhandled ecg service: {}", ecgService)
                return null
            }
        }
    }

    private fun handleTransferStartReq(transferStartReq: GdiEcgService.EcgTransferStartReq): GdiEcgService.EcgService? {
        LOG.debug("Got transfer start request")

        val compressedTransferStartAck: GdiEcgService.EcgTransferStartAck? =
            GdiEcgService.EcgTransferStartAck.newBuilder()
                .setUnk2(1)
                .build()
        return GdiEcgService.EcgService.newBuilder().setFileTransfer(
            GdiEcgService.EcgFileTransfer.newBuilder()
                .setTransferStartAck(compressedTransferStartAck)
        ).build()
    }

    private fun handleEcgFile(ecgFile: GdiEcgService.EcgFile): GdiEcgService.EcgService? {
        if (!ecgFile.name.lowercase().endsWith(".zip")) {
            LOG.warn("File is not zip: {}", ecgFile.name)
            return null
        }

        LOG.debug("Handling ecg zip file: {}", ecgFile.name)
        val outputEcgFile: File
        try {
            val deviceDir: File? = deviceSupport.writableExportDirectory
            val compressedDir = File(deviceDir, "COMPRESSED")
            compressedDir.mkdirs()
            outputEcgFile = File(compressedDir, ecgFile.name)
            FileUtils.copyStreamToFile(ByteArrayInputStream(ecgFile.data.toByteArray()), outputEcgFile)
        } catch (e: Exception) {
            LOG.error("Failed to handle ecg zip file", e)
            return null // do not signal file as saved
        }

        val fileDownloadedDeviceEvent = FileDownloadedDeviceEvent()
        fileDownloadedDeviceEvent.localPath = outputEcgFile.absolutePath
        deviceSupport.evaluateGBDeviceEvent(fileDownloadedDeviceEvent)

        // TODO mark as synced
        return null
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(EcgServiceHandler::class.java)
    }
}
