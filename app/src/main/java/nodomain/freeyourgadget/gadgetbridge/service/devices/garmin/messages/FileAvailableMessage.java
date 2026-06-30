/*  Copyright (C) 2026 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileTransferHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GFDIStatusMessage;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.GenericStatusMessage;

public class FileAvailableMessage extends GFDIMessage {
    protected static final Logger LOG = LoggerFactory.getLogger(FileAvailableMessage.class);

    private final FileTransferHandler.DirectoryEntry directoryEntry;

    public FileAvailableMessage(GarminMessage garminMessage, FileTransferHandler.DirectoryEntry directoryEntry) {
        this.garminMessage = garminMessage;
        this.directoryEntry = directoryEntry;
        this.statusMessage = this.getStatusMessage();
    }

    public static FileAvailableMessage parseIncoming(MessageReader reader, GarminMessage originalGarminMessage) {
        final int fileIndex = reader.readShort(); // 0-1
        final int fileDataType = reader.readByte(); // 2
        final int fileSubType = reader.readByte(); // 3
        final int fileNumber = reader.readShort(); // 4-5
        final int specificFlags = reader.readByte(); // 6
        final int fileFlags = reader.readByte(); // 7
        final int fileSize = reader.readInt(); // 8-11
        final int wireTimestamp = reader.readInt(); // 12-15
        // unknown: reader.readByte(); // 16 - seems to be a flag (0 / 1)

        final FileType.FILETYPE filetype = FileType.FILETYPE.fromDataTypeSubType(fileDataType, fileSubType);
        final Date fileDate = wireTimestamp == 0 ? null
                : new Date(GarminTimeUtils.garminTimestampToJavaMillis(wireTimestamp));
        final FileTransferHandler.DirectoryEntry directoryEntry = new FileTransferHandler.DirectoryEntry(fileIndex, filetype, fileNumber, specificFlags, fileFlags, fileSize, fileDate);
        LOG.info("Received not-yet supported FILE_AVAILABLE message for type {}/{}: {}", fileDataType, fileSubType, directoryEntry);

        return new FileAvailableMessage(originalGarminMessage, directoryEntry);
    }

    @Override
    protected GFDIStatusMessage getStatusMessage() {
        return new GenericStatusMessage(garminMessage, Status.UNSUPPORTED);
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }
}
