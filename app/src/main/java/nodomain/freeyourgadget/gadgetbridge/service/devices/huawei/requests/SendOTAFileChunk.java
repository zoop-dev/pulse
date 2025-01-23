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