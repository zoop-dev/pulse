package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFileSyncService
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.FileDownloadedDeviceEvent
import nodomain.freeyourgadget.gadgetbridge.util.protobuf.buildWith
import org.slf4j.LoggerFactory

class FileSyncServiceHandler(val deviceSupport: GarminSupport) {
    private var nextPageId: Int? = null

    fun handle(fileSyncService: GdiFileSyncService.FileSyncService): GdiFileSyncService.FileSyncService? {
        return when {
            fileSyncService.hasNewFileNotification() -> handleNewFileNotification(fileSyncService.newFileNotification)
            fileSyncService.hasFileListResponse() -> handleFileListResponse(fileSyncService.fileListResponse)
            fileSyncService.hasFileResponse() -> handleFileResponse(fileSyncService.fileResponse)
            else -> {
                LOG.warn("Unhandled file sync service: {}", fileSyncService)
                return null
            }
        }
    }

    private fun handleNewFileNotification(newFileNotification: GdiFileSyncService.NewFileNotification): GdiFileSyncService.FileSyncService? {
        LOG.debug("Got new file notification: {}, ignoring", newFileNotification)
        //deviceSupport.addFileToDownloadList(newFileNotification.file)
        return null
    }

    private fun handleFileResponse(fileResponse: GdiFileSyncService.FileResponse): GdiFileSyncService.FileSyncService? {
        LOG.debug("Got file response: {}", fileResponse)

        if (fileResponse.status != 0) {
            LOG.warn("File download failed with status {}", fileResponse.status)
            // Signal to the support class that the download failed so it can also continue to the next one
            val fileDownloadedDeviceEvent = FileDownloadedDeviceEvent()
            fileDownloadedDeviceEvent.success = false
            deviceSupport.evaluateGBDeviceEvent(fileDownloadedDeviceEvent)
        } else {
            deviceSupport.downloadFileFromServiceV2(fileResponse.handle)
        }

        return null
    }

    private fun handleFileListResponse(fileListResponse: GdiFileSyncService.FileListResponse): GdiFileSyncService.FileSyncService? {
        LOG.debug(
            "Handling file list response with {} files, nextPageId={}",
            fileListResponse.fileList.size,
            fileListResponse.nextPageId
        )

        nextPageId = fileListResponse.nextPageId

        // Only the first entry for a type seems to contain the type name, so keep track of them
        val codeMap: MutableMap<Int?, String?> = HashMap()
        for (file in fileListResponse.fileList) {
            if (!file.hasType() || !file.type.hasCode()) {
                LOG.warn("Ignoring file with unknown type: {}", file)
                continue
            }
            if (file.type.hasName()) {
                codeMap.put(file.type.code, file.type.name)
            }
            val typeName = codeMap[file.type.code]
            if (typeName == null) {
                LOG.warn("No type name found for {}", file)
                continue
            }

            if (!FILE_TYPES_TO_PROCESS.contains(typeName)) {
                LOG.warn("Ignoring file type: {}", typeName)
                continue
            }

            LOG.debug("Adding to download: {}/{} ({})", file.id.id1, file.id.id2, typeName)
            deviceSupport.addFileToDownloadList(file)
        }

        return null
    }

    fun requestFileList(): GdiFileSyncService.FileSyncService {
        LOG.debug("Requesting file list starting at page {}", nextPageId)

        return GdiFileSyncService.FileSyncService.newBuilder().buildWith {
            fileListRequest = GdiFileSyncService.FileListRequest.newBuilder().buildWith {
                startPageId = nextPageId ?: 0
                flags1 = GdiFileSyncService.FileId.newBuilder().setId1(42405).setId2(42405).build()
                flags2 = GdiFileSyncService.FileId.newBuilder().setId1(42405).setId2(42405).build()
            }
        }
    }

    fun requestFile(fileToRequest: GdiFileSyncService.File): GdiFileSyncService.FileSyncService {
        LOG.debug("Requesting file: {}/{}", fileToRequest.id.id1, fileToRequest.id.id2)
        return GdiFileSyncService.FileSyncService.newBuilder().buildWith {
            fileRequest = GdiFileSyncService.FileRequest.newBuilder().buildWith {
                file = fileToRequest
                unk2 = 24
                unk3 = 0
                unk4 = 0
                unk5 = 15
            }
        }
    }

    fun markSynced(syncFile: GdiFileSyncService.File): GdiFileSyncService.FileSyncService {
        return GdiFileSyncService.FileSyncService.newBuilder().buildWith {
            fileSetFlags = GdiFileSyncService.FileSetFlags.newBuilder().buildWith {
                file = syncFile.id
                flags = GdiFileSyncService.FileId.newBuilder().setId1(42405).setId2(42405).build()
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(FileSyncServiceHandler::class.java)

        private val FILE_TYPES_TO_PROCESS: Set<String?> = setOf(
            "FIT_TYPE_4", // ACTIVITY
            "FIT_TYPE_32", // MONITOR
            "FIT_TYPE_44", // METRICS
            "FIT_TYPE_41", // CHANGELOG
            "FIT_TYPE_68", // HRV_STATUS
            "FIT_TYPE_49", // SLEEP
            "FIT_TYPE_61", // ECG
            "FIT_TYPE_73", // SKIN_TEMP
        )
    }
}
