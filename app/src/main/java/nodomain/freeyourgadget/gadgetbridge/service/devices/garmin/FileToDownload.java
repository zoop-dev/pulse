package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.proto.garmin.GdiFileSyncService;

public class FileToDownload {
    private final FileTransferHandler.DirectoryEntry directoryEntry;
    private final GdiFileSyncService.File syncFile;

    public FileToDownload(final FileTransferHandler.DirectoryEntry directoryEntry) {
        this.directoryEntry = directoryEntry;
        this.syncFile = null;
    }

    public FileToDownload(final GdiFileSyncService.File syncFile) {
        this.directoryEntry = null;
        this.syncFile = syncFile;
    }

    @Nullable
    public FileTransferHandler.DirectoryEntry getDirectoryEntry() {
        return directoryEntry;
    }

    @Nullable
    public GdiFileSyncService.File getSyncFile() {
        return syncFile;
    }

    public long getSize() {
        return directoryEntry != null ? directoryEntry.getFileSize() :
                syncFile != null ? syncFile.getSize() : -1;
    }
}
