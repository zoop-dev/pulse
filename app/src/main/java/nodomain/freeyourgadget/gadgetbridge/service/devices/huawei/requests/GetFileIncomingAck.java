package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.FileDownloadService2C;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiFileDownloadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetFileIncomingAck extends Request {
    private final HuaweiFileDownloadManager.FileRequest request;
    private final byte status;

    public GetFileIncomingAck(HuaweiSupportProvider support, HuaweiFileDownloadManager.FileRequest request, byte status) {
        super(support);
        this.serviceId = FileDownloadService2C.id;
        this.commandId = FileDownloadService2C.IncomingInitRequest.id;
        this.request = request;
        this.status = status;

        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new FileDownloadService2C.IncomingInitRequest.Request(this.paramsProvider, this.request.getFilename(), this.request.getInFileType(), this.request.getFileId(), this.request.getFileSize(), this.request.getSrcPackage(), this.request.getDstPackage(), this.request.getSrcFingerprint(), this.request.getDstFingerprint(), this.status).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
