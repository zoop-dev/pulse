/*  Copyright (C) 2024 Me7c7, José Rebelo

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

import android.text.TextUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;

public class HuaweiP2PCannedRepliesService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PCannedRepliesService.class);
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    public static final String MODULE = "hw.unitedevice.smsquick";


    public static final int CMD_CANNED_REPLY_QUERY = 7001;
    public static final int CMD_CANNED_REPLY_UPDATE = 7002;
    public static final int CMD_CANNED_REPLY_CONNECT = 7003;

    public HuaweiP2PCannedRepliesService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PCannedRepliesService");
    }

    public static HuaweiP2PCannedRepliesService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PCannedRepliesService) manager.getRegisteredService(HuaweiP2PCannedRepliesService.MODULE);
    }

    @Override
    public String getModule() {
        return HuaweiP2PCannedRepliesService.MODULE;
    }

    @Override
    public String getPackage() {
        return "com.huawei.watch.home";
    }

    @Override
    public String getFingerprint() {
        return "603AC6A57E2023E00C9C93BB539CA653DF3003EBA4E92EA1904BA4AAA5D938F0";
    }

    @Override
    public void registered() {
        isRegistered.set(true);
        // NOTE: sendConnect can clean saved canned messages. Additional research required
        //sendConnect();
        sendQuery();
    }

    @Override
    public void unregister() {
        isRegistered.set(false);

    }

    public void sendConnect() {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x1, CMD_CANNED_REPLY_CONNECT);
        sendCommand(tlv.serialize(), null);
    }

    public void sendQuery() {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x1, CMD_CANNED_REPLY_QUERY);
        sendCommand(tlv.serialize(), null);
    }

    public void sendReplies(String[] replies) {
        HuaweiTLV tlv = new HuaweiTLV();
        for (String reply : replies) {
            tlv.put(0x83, new HuaweiTLV().put(0x04, reply));
        }
        HuaweiTLV res = new HuaweiTLV();
        res.put(0x1, CMD_CANNED_REPLY_UPDATE).put(0x82, tlv);

        sendCommand(res.serialize(), null);
    }

    private void parseDeviceReplies(HuaweiTLV tlv) {
        List<HuaweiTLV> replies = tlv.getObjects(0x83);
        if (replies.isEmpty())
            return;
        final GBDeviceEventUpdatePreferences gbDeviceEventUpdatePreferences = new GBDeviceEventUpdatePreferences();

        for (int i = 1; i <= manager.getSupportProvider().getHuaweiCoordinator().getCannedRepliesSlotCount(manager.getSupportProvider().getDevice()); i++) {
            String message = null;
            if (replies.size() >= i) {
                if (replies.get(i - 1).contains(0x04)) {
                    try {
                        String reply = replies.get(i - 1).getString(0x04);
                        if (!TextUtils.isEmpty(reply)) {
                            message = reply;
                        }
                    } catch (HuaweiPacket.MissingTagException e) {
                        LOG.info("No tag");
                    }
                }
            }
            gbDeviceEventUpdatePreferences.withPreference("canned_reply_" + i, message);
        }

        manager.getSupportProvider().evaluateGBDeviceEvent(gbDeviceEventUpdatePreferences);
    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PCannedRepliesService handleData");
        try {
            HuaweiTLV tlv = new HuaweiTLV();
            tlv.parse(data);
            LOG.error(tlv.toString());
            if (tlv.contains(0x01)) {
                int code = tlv.getInteger(0x01);
                if (code == CMD_CANNED_REPLY_CONNECT) {
                    // send default replies, replies cannot be empty
                    String[] replies = {"OK", "Yes", "No"};
                    sendReplies(replies);
                }
                if (code == CMD_CANNED_REPLY_QUERY) {
                    if (tlv.contains(0x82)) {
                        parseDeviceReplies(tlv.getObject(0x82));
                    }
                }
            }
        } catch (HuaweiPacket.MissingTagException e) {
            LOG.error("Failed to handle p2p canned replies", e);
        }
    }
}