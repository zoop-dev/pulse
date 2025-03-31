/*  Copyright (C) 2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService2C;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFileDownloadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetFileHashRequest extends Request {
    private final HuaweiFileDownloadManager.FileRequest request;

    public boolean newSync;
    public byte fileId;
    public byte[] fileHash;


    public GetFileHashRequest(HuaweiSupportProvider support, HuaweiFileDownloadManager.FileRequest request) {
        super(support);
        this.serviceId = FileDownloadService2C.id;
        this.commandId = FileDownloadService2C.FileRequestHash.id;
        this.request = request;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FileDownloadService2C.FileRequestHash.Request(paramsProvider, this.request.getFileId()).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (this.receivedPacket instanceof FileDownloadService2C.FileRequestHash.Response) {
            this.newSync = true;
            FileDownloadService2C.FileRequestHash.Response packet = (FileDownloadService2C.FileRequestHash.Response) this.receivedPacket;
            this.fileId = packet.fileId;
            this.fileHash = packet.fileHash;
        } else {
            throw new ResponseTypeMismatchException(this.receivedPacket, FileDownloadService2C.FileInfo.Response.class);
        }
    }
}
