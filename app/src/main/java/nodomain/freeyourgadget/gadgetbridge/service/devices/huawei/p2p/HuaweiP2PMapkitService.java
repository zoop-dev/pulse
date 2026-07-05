package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventDisplayMessage;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.UriHelper;

public class HuaweiP2PMapkitService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PMapkitService.class);

    public static final String MODULE = "hw.unitedevice.mapkit";

    public HuaweiP2PMapkitService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PMapkitService");
    }

    @Override
    public String getModule() {
        return HuaweiP2PMapkitService.MODULE;
    }

    @Override
    public String getPackage() {
        return "in.huawei.mapkit";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    @Override
    public void registered() {
        LOG.info("HuaweiP2PMapkitService registered");
        sendSetup(-1);
    }

    @Override
    public void unregister() {
    }

    public static HuaweiP2PMapkitService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PMapkitService) manager.getRegisteredService(HuaweiP2PMapkitService.MODULE);
    }

    private static final AtomicInteger counter = new AtomicInteger(0);

    public static int nextId() {
        return counter.updateAndGet(n -> (n + 1) % 10000);
    }

    public static byte[] IntToBytes(int value) {
        if (value < 256) {
            return new byte[]{(byte) value};
        }

        int numBytes = 0;
        int temp = value;
        while (temp > 0) {
            temp >>= 8;
            numBytes++;
        }

        byte[] byteArray = new byte[numBytes];
        for (int j = numBytes - 1; j >= 0; j--) {
            byteArray[j] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return byteArray;
    }

    public void sendSetup(int msgId) {
        HuaweiTLV tlv = new HuaweiTLV();
        if(msgId == -1) {
            msgId = nextId();
        }
        tlv.put(0x2, (short) 0x5541); // 0x434E
        tlv.put(0x14);
        tlv.put(0x13, IntToBytes(msgId));

        byte[] data = tlv.serialize();
        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x2);
        packet.put(data);
        packet.flip();
        sendCommand(packet.array(), (code, data1) -> LOG.info("HuaweiP2PMapkitService sendCommand 2 onResponse: {}", code));
    }

    public void queryFreeSpace() {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x1, (byte) 0);

        byte[] data = tlv.serialize();
        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x1);
        packet.put(data);
        packet.flip();
        sendCommand(packet.array(), (code, data1) -> LOG.info("HuaweiP2PMapkitService sendCommand 1 onResponse: {}", code));
    }

    public void queryUploadedMaps() {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x13, IntToBytes(nextId()));


        byte[] data = tlv.serialize();
        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x3);
        packet.put(data);
        packet.flip();
        sendCommand(packet.array(), (code, data1) -> LOG.info("HuaweiP2PMapkitService sendCommand onResponse: {}", code));
    }

    public static class MapInfo {
        private final long mapId;
        private final byte mapType; // 0 - regular map, 1 - contour map
        private final int version;

        public MapInfo(long mapId, byte mapType, int version) {
            this.mapId = mapId;
            this.mapType = mapType;
            this.version = version;
        }

        public MapInfo(long mapId, byte mapType) {
            this(mapId, mapType, -1);
        }

        public long getMapId() {
            return this.mapId;
        }

        public byte getMapType() {
            return this.mapType;
        }

        public int getVersion() {
            return version;
        }
    }

    private void deleteMapsList(byte totalFrames, byte currentFrame, int totalCount, int msgId, List<MapInfo> items) {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x4, totalFrames);
        tlv.put(0x5, currentFrame);
        tlv.put(0x6, totalCount);
        tlv.put(0x13, msgId);

        HuaweiTLV containerTlv = new HuaweiTLV();
        for (MapInfo item : items) {
            HuaweiTLV tlvItem = new HuaweiTLV();
            tlvItem.put(0x9, item.getMapId());
            tlvItem.put(0xa, item.getMapType());
            tlvItem.put(0xb, (byte) 4);
            containerTlv.put(0x91, tlvItem);
        }
        tlv.put(0x90, containerTlv);

        byte[] data = tlv.serialize();
        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x5);
        packet.put(data);
        packet.flip();
        sendCommand(packet.array(), (code, data1) -> LOG.info("HuaweiP2PMapkitService deleteMapsList sendCommand onResponse: {}", code));
    }

    public void deleteMaps() {
        int MAX_ITEM_PER_FRAME_DEL = 40;

        List<MapInfo> items = new ArrayList<>();
        items.add(new MapInfo(633838286457224343L, (byte) 0));

        int size = items.size();
        int totalFrames = size % MAX_ITEM_PER_FRAME_DEL == 0 ? size / MAX_ITEM_PER_FRAME_DEL : (size / MAX_ITEM_PER_FRAME_DEL) + 1;

        int msgId = nextId();

        for (int start = 0, currentFrame = 0; start < size; start += MAX_ITEM_PER_FRAME_DEL, currentFrame++) {
            int end = Math.min(size, start + MAX_ITEM_PER_FRAME_DEL);
            deleteMapsList((byte) totalFrames, (byte) currentFrame, size, msgId, items.subList(start, end));
        }
    }

    public void startUpload(String filename, UriHelper uriHelper) {
        sendPing((code, data) -> {
            if ((byte) code != (byte) 0xca)
                return;
            startUpload2(filename, uriHelper);
        });
    }
    private String currentFilename;
    private UriHelper currentUriHelper;

    private void cleanupUpload() {
        currentFilename = null;
        currentUriHelper = null;
    }

    public void startUpload2(String filename, UriHelper uriHelper) {
        queryFreeSpace();
        queryUploadedMaps();

        currentFilename = filename;
        currentUriHelper = uriHelper;

        byte mapType = 0;
        String withoutExt = currentFilename.substring(0, currentFilename.length() - 4);
        if(withoutExt.equals("global")) {
            mapType = 2;
        } else if(withoutExt.endsWith("_contour")) {
            mapType = 1;
        }

        long size = uriHelper.getFileSize() / 1024 / 1024;
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x2, (short) 0x5541);
        tlv.put(0x14);
        tlv.put(0xd, (short) size);
        tlv.put(0xa, mapType); //map type 0 - regular, 1 - contour, 2 - global
        tlv.put(0xe, (byte) 0); // sync type 0 - install, 1 - update
        tlv.put(0x13, nextId());
        //tlv.put(0x16, (byte) 0); // unknown

        byte[] data = tlv.serialize();
        ByteBuffer packet = ByteBuffer.allocate(1 + data.length);
        packet.put((byte) 0x4);
        packet.put(data);
        packet.flip();
        sendCommand(packet.array(), (code, data1) -> LOG.info("HuaweiP2PMapkitService sendCommand 4 onResponse: {}", code));
    }


    public void uploadMapToDevice(String filename, UriHelper uriHelper) {

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 7);
        fileInfo.setFileName(filename);
        fileInfo.setUploadData(new HuaweiUploadManager.UploadDataFile(uriHelper));
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
                LOG.info("HuaweiP2PMapkitService upload complete");
                manager.getSupportProvider().getUploadManager().unsetDeviceBusy();
                manager.getSupportProvider().onUploadProgress(R.string.updatefirmwareoperation_update_complete, 100, false);
                cleanupUpload();
                queryUploadedMaps();
            }

            @Override
            public void onError(int code) {
                LOG.info("HuaweiP2PMapkitService upload error: {}", code);
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
            LOG.error("HuaweiP2PAppIcon Failed to send file upload info", e);
        }
    }

    private int count = 0;
    private void handleSetupResponse(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
        String result = tlv.getString(0x02); //"0" - success, "1" - error, resent on error at least 3 times.
        int msgId = tlv.getAsInteger(0x13, -1);
        boolean unknown = tlv.contains(0x14);
        LOG.info("Result: {} MsgId: {} Unknown: {}", result, msgId, unknown);
        if(result.equals("0")) {
            return;
        }
        final Timer timer = new Timer();
        if (this.count < 3) {
            this.count++;
            timer.schedule(new TimerTask() {
                public void run() {
                    sendSetup(msgId);
                    timer.cancel();
                }
            }, 3000);
        }
    }

    private final HashMap<Integer, ArrayList<MapInfo>> responses = new HashMap<>();

    private void handleMapList(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
        int totalFrames = tlv.getAsInteger(0x4, -1);
        int currentFrame = tlv.getAsInteger(0x5, -1);
        int totalNum = tlv.getAsInteger(0x6, -1);
        int msgId = tlv.getAsInteger(0x13, -1);

        LOG.info("handleMapList totalFrames: {} currentFrame: {} TotalNum: {} MsgId: {}", totalFrames, currentFrame, totalNum, msgId);
        if (totalNum > 0) {
            ArrayList<MapInfo> list = new ArrayList<>();
            HuaweiTLV ArrTlv = tlv.getObject(0x87);
            for (HuaweiTLV subTlv : ArrTlv.getObjects(0x88)) {
                long mapId = subTlv.getAsLong(0x09);
                byte mapType = subTlv.getByte(0x0a);
                int version = subTlv.getAsInteger(0x0b, -1);
                LOG.info("MapId: {} MapType: {} Version: {}", mapId, mapType, version);
                list.add(new MapInfo(mapId, mapType, version));
            }

            if (totalFrames == 1) {
                // all done, nothing to do, call callback.
                return;
            }

            if (this.responses.get(msgId) == null) {
                this.responses.put(msgId, list);
            } else {
                this.responses.get(msgId).addAll(list);
            }
            if (this.responses.get(msgId).size() == totalNum) {
                // all done, nothing to do, call callback.
                this.responses.remove(msgId);
            } else if (this.responses.get(msgId).size() > totalNum) {
                //Error
                this.responses.remove(msgId);
            } else {
                LOG.info("HuaweiP2PMapkitService handleMapList wait next");
            }
        }
    }

    private void handleStartUploadResponse(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
        int result = tlv.getAsInteger(0x0c, -1);
        LOG.info("Result: {}", result); //0 - success
        LOG.info("MsgId: {}", tlv.getAsInteger(0x13, -1));

        if(result == 0) {
            uploadMapToDevice(currentFilename, currentUriHelper);
        } else {
            cleanupUpload();
        }
    }

    private void handleDeleteMaps(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
        int totalFrames = tlv.getAsInteger(0x4, -1);
        int currentFrame = tlv.getAsInteger(0x5, -1);
        int totalNum = tlv.getAsInteger(0x6, -1);
        int msgId = tlv.getAsInteger(0x13, -1);
        LOG.info("handleDeleteMaps totalFrames: {} currentFrame: {} TotalNum: {} MsgId: {}", totalFrames, currentFrame, totalNum, msgId);

        ArrayList<MapInfo> list = new ArrayList<>();
        if (totalNum > 0) {
            HuaweiTLV ArrTlv = tlv.getObject(0x90);
            for (HuaweiTLV subTlv : ArrTlv.getObjects(0x91)) {
                long mapId = subTlv.getAsLong(0x09);
                byte mapType = subTlv.getByte(0x0a);
                int version = subTlv.getAsInteger(0x12, -1); // 0 - success
                LOG.info("Delete MapId: {} MapType: {} Version: {}", mapId, mapType, version);
                list.add(new MapInfo(mapId, mapType, version));
            }
        }
    }

    @Override
    public void handleData(byte[] data) {
        byte type = data[0];
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.parse(data, 1, data.length - 1);

        LOG.info("HuaweiP2PMapkitService handleData type: {} TLV: {}", type, tlv);
        try {
            switch (type) {
                case 1: // free space
                    LOG.info("Device free space: {}", tlv.getAsLong(0x01));
                    break;
                case 2: // unknown - maybe setup or capabilities
                    handleSetupResponse(tlv);
                    break;
                case 3: // maps list on device
                    handleMapList(tlv);
                    break;
                case 4: // start upload
                    handleStartUploadResponse(tlv);
                    break;
                case 5: // delete maps
                    handleDeleteMaps(tlv);
                    break;
                case 7: // unknown device request
                    sendSetup(-1);
                    break;
                case 8: //unknown
                    LOG.info("MapId: {}", tlv.getAsLong(0x09));
                    LOG.info("MapType: {}", tlv.getAsInteger(0x0a, -1));
                    LOG.info("ret: {}", tlv.getAsInteger(0x15, -1)); // 0 - success ??
                default:
                    LOG.info("Unknown type: {}", type);
            }
        } catch (HuaweiPacket.MissingTagException e) {
            throw new RuntimeException(e);
        }
    }

}

