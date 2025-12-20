/*  Copyright (C) 2024-2025 Me7c7, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendP2PCommand;

public abstract class HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiBaseP2PService.class);

    public interface HuaweiP2PCallback {
        void onResponse(int code, byte[] data);
    }

    protected final HuaweiP2PManager manager;

    protected HuaweiBaseP2PService(HuaweiP2PManager manager) {
        this.manager = manager;
    }

    public void register() {
        manager.registerService(this);
    }

    public abstract String getModule();

    public abstract String getPackage();

    public abstract String getFingerprint();

    public abstract void registered();

    public abstract void unregister();

    public abstract void handleData(byte[] data);

    public String getLocalFingerprint() {
        return "UniteDeviceManagement";
    }

    public String getPingPackage() {
        return "com.huawei.health";
    }

    private final Map<Short, HuaweiP2PCallback> waitPackets = new ConcurrentHashMap<>();

    private short getNextSequence() {
        return manager.getNextSequence();
    }

    private void sendP2PCommand(byte cmdId,
                               String srcPackage,
                               String dstPackage,
                               String srcFingerprint,
                               String dstFingerprint,
                               byte[] sendData,
                               HuaweiP2PCallback callback) {
        try {
            short seq = this.getNextSequence();
            SendP2PCommand cmd = new SendP2PCommand(this.manager.getSupportProvider(), cmdId, seq, srcPackage, dstPackage, srcFingerprint, dstFingerprint, sendData, 0);
            if (callback != null) {
                this.waitPackets.put(seq, callback);
            }
            cmd.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send p2p cmdId: {}", cmdId,  e);
        }
    }

    public void sendPing(HuaweiP2PCallback callback) {
        sendP2PCommand((byte) 1, this.getPingPackage(), this.getPackage(), null, null, null, callback);
    }

    public void sendCommand(byte[] sendData, HuaweiP2PCallback callback) {
        sendP2PCommand((byte) 2, this.getModule(), this.getPackage(), this.getLocalFingerprint(), this.getFingerprint(), sendData, callback);
    }
    public void sendGetVersion(HuaweiP2PCallback callback) {
        if(manager.getSupportProvider().getDeviceState().supportsP2PGetAppVersion()) {
            sendP2PCommand((byte) 4, this.getModule(), this.getPackage(), null, null, null, callback);
        } else {
            LOG.error("P2P Get App Version is not supported");
        }
    }

    protected int onPingPacket(P2P.P2PCommand.Response packet) {
        return 0xcf;
    }

    protected int onDataPacket(P2P.P2PCommand.Response packet) {
        return 0xcf;
    }

    public void handlePacket(P2P.P2PCommand.Response packet) {
        LOG.info("HuaweiBaseP2PService handlePacket: {} Code: {}", packet.cmdId, packet.respCode);
        if (waitPackets.containsKey(packet.sequenceId)) {
            LOG.info("HuaweiBaseP2PService handlePacket find handler");
            HuaweiP2PCallback handle = waitPackets.remove(packet.sequenceId);
            if(handle != null) {
                handle.onResponse(packet.respCode, packet.respData);
            } else {
                LOG.error("HuaweiBaseP2PService handler is null");
            }
        } else {
            if (packet.cmdId == 1) { //Ping
                int ret = onPingPacket(packet);
                manager.sendAck(packet.sequenceId, packet.dstPackage, packet.srcPackage, ret);
            } else if (packet.cmdId == 2) {
                int ret = onDataPacket(packet);
                manager.sendAck(packet.sequenceId, packet.dstPackage, packet.srcPackage, ret);
                handleData(packet.respData);
            }
        }
    }

    public void handleFile(String filename, byte[] data) {

    }

}
