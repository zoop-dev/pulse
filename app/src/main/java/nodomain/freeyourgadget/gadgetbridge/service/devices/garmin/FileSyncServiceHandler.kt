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
        LOG.debug("Got new file notification: {}", newFileNotification)
        if (!newFileNotification.file.hasType() || !newFileNotification.file.type.hasName()) {
            LOG.warn("New file has no type name")
            return null
        }
        val fetchUnknownFiles = deviceSupport.devicePrefs.fetchUnknownFiles
        val typeName = newFileNotification.file.type.name
        if (!FILE_TYPES_TO_PROCESS.contains(typeName) && !fetchUnknownFiles) {
            LOG.warn("Ignoring file type: {}", typeName)
            return null
        }

        deviceSupport.addFileToDownloadList(newFileNotification.file)
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

        val fetchUnknownFiles = deviceSupport.devicePrefs.fetchUnknownFiles

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

            if (!FILE_TYPES_TO_PROCESS.contains(typeName) && !fetchUnknownFiles) {
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

        val fileListRequestBuilder = GdiFileSyncService.FileListRequest.newBuilder().apply {
            flags1 = GdiFileSyncService.FileId.newBuilder().setId1(42405).setId2(42405).build()
            flags2 = GdiFileSyncService.FileId.newBuilder().setId1(42405).setId2(42405).build()
        }

        nextPageId?.let { fileListRequestBuilder.startPageId = it }

        return GdiFileSyncService.FileSyncService.newBuilder().buildWith {
            fileListRequest = fileListRequestBuilder.build()
        }
    }

    fun requestFile(fileToRequest: GdiFileSyncService.File): GdiFileSyncService.FileSyncService {
        LOG.debug(
            "Requesting file: {}/{} ({})",
            fileToRequest.id.id1,
            fileToRequest.id.id2,
            fileToRequest.type.name
        )
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

    fun markSynced(syncFile: GdiFileSyncService.File): GdiFileSyncService.FileSyncService? {
        val fetchUnknownFiles = deviceSupport.devicePrefs.fetchUnknownFiles
        if (fetchUnknownFiles) {
            // Since some of the unknown files are not really supposed to be marked as synced (eg. settings, courses, locations)
            // let's avoid sending the command if it's not a file that we process
            if (syncFile.type.name == null) {
                LOG.warn("Will not mark {}/{} as synced - unknown type", syncFile.id.id1, syncFile.id.id2)
                return null
            }
            if (!FILE_TYPES_TO_PROCESS.contains(syncFile.type.name)) {
                LOG.warn(
                    "Will not mark {}/{} ({}) as synced - not a file to process",
                    syncFile.id.id1,
                    syncFile.id.id2,
                    syncFile.type.name
                )
                return null
            }
        }

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
