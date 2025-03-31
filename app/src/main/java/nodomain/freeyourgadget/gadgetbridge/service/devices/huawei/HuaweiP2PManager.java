/*  Copyright (C) 2024-2025 Me7c7

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import android.net.Uri;
import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.P2P;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiBaseP2PService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PWakeAppScreenshot;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendP2PCommand;

public class HuaweiP2PManager {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PManager.class);

    public interface HuaweiWakeApp {
        boolean onWakeApp(HuaweiP2PManager manager, Uri uri);
    }

    private final HuaweiSupportProvider support;

    private final List<HuaweiBaseP2PService> registeredServices;

    private final Map<String, HuaweiWakeApp> supportedWakeApp;

    private Short sequence = 1;

    public synchronized Short getNextSequence() {
            return sequence++;
    }

    public HuaweiP2PManager(HuaweiSupportProvider support) {
        this.support = support;
        this.registeredServices = new ArrayList<>();
        this.supportedWakeApp = new HashMap<>();
        this.supportedWakeApp.put("/router/device/screenshot",new HuaweiP2PWakeAppScreenshot());
    }

    public HuaweiSupportProvider getSupportProvider() {
        return support;
    }

    public void registerService(HuaweiBaseP2PService service) {
        for (HuaweiBaseP2PService svr : registeredServices) {
            if (svr.getModule().equals(service.getModule())) {
                LOG.error("P2P Service already registered, unregister: {}", service.getModule());
                svr.unregister();
                registeredServices.remove(svr);
            }
        }
        registeredServices.add(service);
        service.registered();
    }

    public HuaweiBaseP2PService getRegisteredService(String module) {
        for (HuaweiBaseP2PService svr : registeredServices) {
            if (svr.getModule().equals(module)) {
                return svr;
            }
        }
        return null;
    }

    public void unregisterAllService() {
        for (HuaweiBaseP2PService svr : registeredServices) {
            svr.unregister();
        }
        registeredServices.clear();
    }

    public void sendAck(short sequence, String srcPackage, String dstPackage, int code) {
        try {
            SendP2PCommand test = new SendP2PCommand(this.getSupportProvider(), (byte) 3, sequence, srcPackage, dstPackage, null, null, null, code);
            test.doPerform();
        } catch (IOException e) {
            LOG.error("P2P Service error send ACK", e);
        }
    }

    public int handleLink(String link) {
        if (TextUtils.isEmpty(link) || (!link.startsWith("huaweischeme://healthapp/router/") && !link.startsWith("huaweischeme://healthapp/home/"))) {
            return 0xd2;
        }
        Uri uri = Uri.parse(link);
        LOG.info("Path: {}", uri.getPath());
        HuaweiWakeApp svr = supportedWakeApp.get(uri.getPath());
        if(svr != null && svr.onWakeApp(this, uri)) {
            return 0xd1; //success
        }
        return 0xd2;
    }

    public int handleWakeApp(P2P.P2PCommand.Response packet) {
        if (packet.respData == null || packet.respData.length == 0) {
            return 0xcc;
        }

        HuaweiTLV tlv = new HuaweiTLV();
        tlv.parse(packet.respData);
        String link = "";
        if(tlv.contains(0x04)) {
            try {
                link = tlv.getString(0x04);
            } catch (HuaweiPacket.MissingTagException e) {
                LOG.error("P2P Service error get link", e);
            }
        }

        if (!TextUtils.isEmpty(link)) {
            return handleLink(link);
        }
        // TODO: support other TLV.
        return 0xcd;
    }


    public void handleFile(String srcPackage, String dstPackage, String srcFingerprint, String dstFingerprint, String filename, byte[] data) {
        // NOTE: Maybe packet should be found by dstPacket or as a pair package + fingerprint
        for (HuaweiBaseP2PService service : registeredServices) {
            if (service.getPackage().equals(srcPackage)) {
                service.handleFile(filename, data);
            }
        }
    }

    public void handlePacket(P2P.P2PCommand.Response packet) {
        LOG.info("P2P Service message: Src: {} Dst: {} Seq: {}", packet.srcPackage, packet.dstPackage, packet.sequenceId);
        if(packet.cmdId == 1) {
            String[] split = packet.dstPackage.split("\\.");
            if (split.length > 2 && split[2].equals("wakeapp")) {
                int code = handleWakeApp(packet);
                sendAck(packet.sequenceId, packet.dstPackage, packet.srcPackage, code);
                return;
            }
        }
        for (HuaweiBaseP2PService service : registeredServices) {
            if (service.getPackage().equals(packet.srcPackage)) {
                service.handlePacket(packet);
            }
        }
    }
}
