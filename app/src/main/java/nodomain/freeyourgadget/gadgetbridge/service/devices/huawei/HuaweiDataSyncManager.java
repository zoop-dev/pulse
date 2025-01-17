package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync.HuaweiDataSyncCommon;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiBaseP2PService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetHiChainRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.GetPincodeRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDataSyncConfigCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDataSyncDataCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDataSyncDictDataCommand;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendDataSyncEventCommand;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiDataSyncManager {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiDataSyncManager.class);

    private final HuaweiSupportProvider support;

    private final  Map<String, HuaweiDataSyncCommon.DataCallback> registeredCallbacks = new HashMap<>();

    public HuaweiDataSyncManager(HuaweiSupportProvider support) {
        this.support = support;
    }

    public void registerCallback(String pkg, HuaweiDataSyncCommon.DataCallback callback) {
        registeredCallbacks.put(pkg, callback);
    }

    public void unregisterCallback(String pkg) {
        registeredCallbacks.remove(pkg);
    }

    public void unregisterAll() {
        registeredCallbacks.clear();
    }

    public void handleConfigCommandResponse(String srcPackage, String dstPackage, HuaweiDataSyncCommon.ConfigCommandData data) {
        LOG.info("DataSync handleConfigCommandResponse SRC: {}, DST: {}", srcPackage, dstPackage);
        HuaweiDataSyncCommon.DataCallback callback = registeredCallbacks.get(srcPackage);
        if(callback != null) {
            callback.onConfigCommand(data);
        }
    }

    public void handleEventCommandResponse(String srcPackage, String dstPackage, HuaweiDataSyncCommon.EventCommandData data) {
        LOG.info("DataSync handleSampleEventCommandResponse SRC: {}, DST: {}", srcPackage, dstPackage);
        HuaweiDataSyncCommon.DataCallback callback = registeredCallbacks.get(srcPackage);
        if(callback != null) {
            callback.onEventCommand(data);
        }
    }

    public void handleDataCommandResponse(String srcPackage, String dstPackage, HuaweiDataSyncCommon.DataCommandData data) {
        LOG.info("DataSync handleSampleDataCommandResponse SRC: {}, DST: {}", srcPackage, dstPackage);
        HuaweiDataSyncCommon.DataCallback callback = registeredCallbacks.get(srcPackage);
        if(callback != null) {
            callback.onDataCommand(data);
        }
    }

    public void handleDictDataCommandResponse(String srcPackage, String dstPackage, HuaweiDataSyncCommon.DictDataCommandData data) {
        LOG.info("DataSync handleDictDataCommandResponse SRC: {}, DST: {}", srcPackage, dstPackage);
        HuaweiDataSyncCommon.DataCallback callback = registeredCallbacks.get(srcPackage);
        if(callback != null) {
            callback.onDictDataCommand(data);
        }
    }

    public boolean sendConfigCommand(String srcPackage, String dstPackage, HuaweiDataSyncCommon.ConfigCommandData data) {
        if(!this.support.getHuaweiCoordinator().supportsDeviceCommandConfig()) {
            LOG.info("sendConfigCommand is not supported");
            return false;
        }
        try {
            SendDataSyncConfigCommand sendDataSyncConfigCommand = new SendDataSyncConfigCommand(this.support, srcPackage, dstPackage, data);
            sendDataSyncConfigCommand.doPerform();
        } catch (IOException e) {
            LOG.error("SendDataSyncConfigCommand failed", e);
            return false;
        }
        return true;
    }

    public boolean sendEventCommand(String srcPackage, String dstPackage, HuaweiDataSyncCommon.EventCommandData data) {
        if(!this.support.getHuaweiCoordinator().supportsDeviceCommandEvent()) {
            LOG.info("sendEventCommand is not supported");
            return false;
        }
        try {
            SendDataSyncEventCommand sendDataSyncEventCommand = new SendDataSyncEventCommand(this.support, srcPackage, dstPackage, data);
            sendDataSyncEventCommand.doPerform();
        } catch (IOException e) {
            LOG.error("SendDataSyncEventCommand failed", e);
            return false;
        }
        return true;
    }

    public boolean sendDataCommand(String srcPackage, String dstPackage, HuaweiDataSyncCommon.DataCommandData data) {
        if(!this.support.getHuaweiCoordinator().supportsDeviceCommandData()) {
            LOG.info("sendDataCommand is not supported");
            return false;
        }
        try {
            SendDataSyncDataCommand sendDataSyncDataCommand = new SendDataSyncDataCommand(this.support, srcPackage, dstPackage, data);
            sendDataSyncDataCommand.doPerform();
        } catch (IOException e) {
            LOG.error("SendDataSyncDataCommand failed", e);
            return false;
        }
        return true;
    }

    public boolean sendDictDataCommand(String srcPackage, String dstPackage, HuaweiDataSyncCommon.DictDataCommandData data) {
        if(!this.support.getHuaweiCoordinator().supportsDeviceCommandDictData()) {
            LOG.info("sendDictDataCommand is not supported");
            return false;
        }
        try {
            SendDataSyncDictDataCommand sendDataSyncDictDataCommand = new SendDataSyncDictDataCommand(this.support, srcPackage, dstPackage, data);
            sendDataSyncDictDataCommand.doPerform();
        } catch (IOException e) {
            LOG.error("SendDataSyncDictDataCommand failed", e);
            return false;
        }
        return true;
    }

}
