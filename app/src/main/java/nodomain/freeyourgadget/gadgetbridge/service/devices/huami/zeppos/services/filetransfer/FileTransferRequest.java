/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.filetransfer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsFileTransferService;

/**
 * Wrapper class to keep track of ongoing file send requests and their progress.
 */
public class FileTransferRequest {
    private final String url;
    private final String filename;
    private final int rawLength;
    private final byte[] bytes;
    private final boolean compressed;
    private final int crc32;
    private final int chunkSize;
    private final ZeppOsFileTransferService.Callback callback;

    private int progress = 0;
    private int index = 0;

    public FileTransferRequest(final String url,
                               final String filename,
                               final int rawLength,
                               final byte[] bytes,
                               final boolean compressed,
                               final int crc32,
                               final int chunkSize,
                               final ZeppOsFileTransferService.Callback callback) {
        this.url = url;
        this.filename = filename;
        this.rawLength = rawLength;
        this.bytes = bytes;
        this.compressed = compressed;
        this.crc32 = crc32;
        this.chunkSize = chunkSize;
        this.callback = callback;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

    public int getRawLength() {
        return rawLength;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getSize() {
        return bytes.length;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public int getCrc32() {
        return crc32;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public ZeppOsFileTransferService.Callback getCallback() {
        return callback;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(final int progress) {
        this.progress = progress;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(final int index) {
        this.index = index & 0xff;
    }
}
