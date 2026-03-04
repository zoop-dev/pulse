package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.utils.HuaweiP2PSubMsg;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.utils.HuaweiConvertTrack;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.utils.HuaweiRouteTrack;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiP2PFitnessData extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PFitnessData.class);

    public static final String MODULE = "hw.unitedevice.fitness";


    public HuaweiP2PFitnessData(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PFitness");
    }

    @Override
    public String getModule() {
        return MODULE;
    }

    @Override
    public String getPackage() {
        return "in.huawei.fitness";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    HuaweiRouteTrack currentTrack = null;

    public void sendTrack(HuaweiRouteTrack track) {
        currentTrack = track;

        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(5, track.getTrackName());

        HuaweiP2PSubMsg packet = new HuaweiP2PSubMsg(5, HuaweiP2PSubMsg.getNext(), tlv.serialize());
        LOG.info("HuaweiP2PFitness send init data b: {}", GB.hexdump(packet.getBytes()));

        sendCommand(packet.getBytes(), (code, data) -> {
            LOG.info("HuaweiP2PFitness send init data reply code: {}", code);
            if((byte) code != (byte) 0xcf) {
                cleanupUpload();
            }
        });
    }

    void cleanupUpload() {
        currentTrack = null;
    }

    void sendCurrentTrack(int maxPoints) {
        if(currentTrack == null) {
            return;
        }
        HuaweiConvertTrack trackFeatureExtraction = new HuaweiConvertTrack();
        byte[] data  = trackFeatureExtraction.convertTrackToBin(currentTrack, Math.max(maxPoints, 200));

        LOG.info("size: {}", data.length);
        LOG.info("track upload data: {}", GB.hexdump(data));

        HuaweiP2PSubMsg packet = new HuaweiP2PSubMsg(6, HuaweiP2PSubMsg.getNext(), data);
        LOG.info("size: {}", packet.getBytes().length);
        LOG.info("file upload data: {}", GB.hexdump(packet.getBytes()));

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();
        fileInfo.setFileType((byte) 7);
        fileInfo.setFileName("byte.bin");
        fileInfo.setUploadData(new HuaweiUploadManager.UploadDataBuffer(packet.getBytes()));
        fileInfo.setSrcPackage(this.getModule());
        fileInfo.setDstPackage(this.getPackage());
        fileInfo.setSrcFingerprint(this.getLocalFingerprint());
        fileInfo.setDstFingerprint(this.getFingerprint());

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
                manager.getSupportProvider().getUploadManager().setDeviceBusy();
            }

            @Override
            public void onUploadProgress(int progress) {
                manager.getSupportProvider().onUploadProgress(R.string.updatefirmwareoperation_update_in_progress, progress, true);

            }

            @Override
            public void onUploadComplete() {
                LOG.info("HuaweiP2PFitness upload complete");
                manager.getSupportProvider().getUploadManager().unsetDeviceBusy();
                manager.getSupportProvider().onUploadProgress(R.string.updatefirmwareoperation_update_complete, 100, false);
                cleanupUpload();
            }

            @Override
            public void onError(int code) {
                LOG.info("HuaweiP2PFitness upload error: {}", code);
                cleanupUpload();
                manager.getSupportProvider().handleGBDeviceEvent(new GBDeviceEventDisplayMessage("Error", Toast.LENGTH_LONG, GB.ERROR));
            }
        });

        HuaweiUploadManager huaweiUploadManager = this.manager.getSupportProvider().getUploadManager();

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(this.manager.getSupportProvider(), huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("HuaweiP2PFitness Failed to send file upload info", e);
        }

    }


    @Override
    public void registered() {
    }

    @Override
    public void unregister() {

    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PFitness handleData: {}", data.length);
        HuaweiP2PSubMsg packet = new HuaweiP2PSubMsg(data);
        if(packet.getTotalLength() > 0) {
             if(packet.getType() == 5) {
                 byte[] subData = packet.getData();
                 if(subData.length >= 4) {
                     LOG.info("subdata: {}", GB.hexdump(subData));
                     ByteBuffer buffer = ByteBuffer.wrap(subData);
                     buffer.order(ByteOrder.LITTLE_ENDIAN);
                     int code = buffer.getInt();  // 0 - error, 1 - success
                     int maxPoints = 0;
                     if (buffer.remaining() >= 4) {
                        maxPoints = buffer.getInt();
                     }
                     LOG.info("code: {}, maxpoints: {}", code, maxPoints);
                     if(code == 1) {
                         sendCurrentTrack(maxPoints);
                     } else {
                         // TODO: handle errors
                         cleanupUpload();
                     }
                 }
             } else if(packet.getType() == 6) {
                 int code = 0; // 0 - error, 1 - success
                 byte[] subData = packet.getData();
                 if(subData.length >= 4) {
                     ByteBuffer buffer = ByteBuffer.wrap(subData);
                     buffer.order(ByteOrder.LITTLE_ENDIAN);
                     code = buffer.getInt();
                 }
                 LOG.info("HuaweiP2PFitness upload done. code: {}", code);
                 cleanupUpload();
             }
        }
    }

    public static HuaweiP2PFitnessData getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PFitnessData) manager.getRegisteredService(HuaweiP2PFitnessData.MODULE);
    }
}
