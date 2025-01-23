package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTAProgress extends Request {

    private final byte progress;
    private final byte state;
    private final byte mode;

    public SendOTAProgress(HuaweiSupportProvider support,
                          byte progress,
                           byte state,
                           byte mode) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.Progress.id;

        this.progress = progress;
        this.state = state;
        this.mode = mode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.Progress.Request(paramsProvider, this.progress, this.state, this.mode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
