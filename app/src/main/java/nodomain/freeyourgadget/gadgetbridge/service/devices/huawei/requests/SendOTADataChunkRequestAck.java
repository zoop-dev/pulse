package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTADataChunkRequestAck extends Request {

    private final OTA.DataChunkRequest.Response response;

    public SendOTADataChunkRequestAck(HuaweiSupportProvider support, OTA.DataChunkRequest.Response response) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.DataChunkRequest.id;
        this.response = response;

        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return response.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
