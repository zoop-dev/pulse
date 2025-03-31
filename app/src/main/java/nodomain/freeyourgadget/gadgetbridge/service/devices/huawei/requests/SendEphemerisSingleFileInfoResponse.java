/*  Copyright (C) 2024 Me7c7

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
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.EphemerisFileUpload;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendEphemerisSingleFileInfoResponse extends Request {
    final int responseCode;
    final int fileSize;
    final short crc;

    public SendEphemerisSingleFileInfoResponse(HuaweiSupportProvider support, int responseCode, int fileSize, short crc) {
        super(support);
        this.serviceId = EphemerisFileUpload.id;
        this.commandId = EphemerisFileUpload.FileList.id;
        this.responseCode = responseCode;
        this.fileSize = fileSize;
        this.crc = crc;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new EphemerisFileUpload.QuerySingleFileInfo.QuerySingleFileInfoResponse(this.paramsProvider, this.responseCode, this.fileSize, this.crc).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }
}
