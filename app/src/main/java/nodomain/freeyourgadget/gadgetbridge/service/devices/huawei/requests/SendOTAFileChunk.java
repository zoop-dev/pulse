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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;


public class SendOTAFileChunk extends Request {
    private final byte[] fileChunk;
    private final int offset;
    private final int unitSize;
    private final boolean addOffset;
    private final List<Integer> bitmap;
    public SendOTAFileChunk(HuaweiSupportProvider support, byte[] fileChunk, int offset, int unitSize, boolean addOffset, List<Integer> bitmap) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.NextChunkSend.id;
        this.fileChunk = fileChunk;
        this.offset = offset;
        this.unitSize = unitSize;
        this.addOffset = addOffset;
        this.bitmap = bitmap;

        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.NextChunkSend(this.paramsProvider).serializeOTAChunk(fileChunk, offset, unitSize, addOffset,bitmap);
        } catch(HuaweiPacket.SerializeException e) {
            throw new RequestCreationException(e.getMessage());
        }
    }
}