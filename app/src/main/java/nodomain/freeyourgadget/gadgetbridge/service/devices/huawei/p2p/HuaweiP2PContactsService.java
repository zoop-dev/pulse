/*  Copyright (C) 2025 Me7c7

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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.entities.Contact;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiP2PManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiUploadManager;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.SendFileUploadInfo;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class HuaweiP2PContactsService extends HuaweiBaseP2PService {
    private final Logger LOG = LoggerFactory.getLogger(HuaweiP2PContactsService.class);
    private final AtomicBoolean isRegistered = new AtomicBoolean(false);

    public static final String MODULE = "hw.unitedevice.contactsapp";

    public HuaweiP2PContactsService(HuaweiP2PManager manager) {
        super(manager);
        LOG.info("HuaweiP2PContactsService");
    }

    public static HuaweiP2PContactsService getRegisteredInstance(HuaweiP2PManager manager) {
        return (HuaweiP2PContactsService) manager.getRegisteredService(MODULE);
    }

    @Override
    public String getModule() {
        return MODULE;
    }

    @Override
    public String getPackage() {
        return "in.huawei.contacts";
    }

    @Override
    public String getFingerprint() {
        return "SystemApp";
    }

    @Override
    public void registered() {
        isRegistered.set(true);
    }

    @Override
    public void unregister() {
        isRegistered.set(false);

    }

    private int getUtf8ByteLength(char c) {
        if (c <= 127) return 1;  // ASCII (1 byte)
        if (c <= 2047) return 2; // Latin-1, extended ASCII (2 bytes)
        return 3;                // unicode characters (3 bytes)
    }

    private String truncateByLen(String str, int maxBytes) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        int byteCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char currentChar = str.charAt(i);
            byteCount += getUtf8ByteLength(currentChar);
            if (byteCount > maxBytes) {
                break;
            }
            sb.append(currentChar);
        }
        return sb.toString();
    }

    public static <T> List<List<T>> partitionList(List<T> list, int chunkSize) {
        if (list == null) {
            return new ArrayList<>();
        }
        ArrayList<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < list.size(); start += chunkSize) {
            chunks.add(list.subList(start, Math.min(start + chunkSize, list.size())));
        }
        return chunks;
    }

    private List<String> prepareDataWithHeader(int operation, String majorVersion, int minorVersion, TreeMap<String, List<JsonObject>> map) {
        List<String> ret = new ArrayList<>();
        for (Map.Entry<String, List<JsonObject>> next : map.entrySet()) {
            for (List<JsonObject> list : partitionList(next.getValue(), 20)) {
                JsonArray contactsList = new JsonArray();
                for (JsonObject obj : list) {
                    contactsList.add(obj);
                }
                JsonObject syncData = new JsonObject();
                syncData.addProperty("operation", operation);
                syncData.addProperty("minor", minorVersion + 1);
                syncData.addProperty("major", truncateByLen(majorVersion, 32));
                syncData.add("contactsList", contactsList); // max 20
                syncData.addProperty("sortLetter", next.getKey());

                String json = new Gson().toJson(syncData);
                LOG.info(json);
                ret.add(json);
            }
        }
        return ret;
    }

    // NOTE: currently watch is properly works only with ASCII symbols.
    public static String getLetter(final String str) {
        final String tmp = str.substring(0, 1).toUpperCase(Locale.ENGLISH);
        return tmp.matches("[A-Z]") ? tmp : "#";
    }

    public static String removeNonAscii(String str) {
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    private JsonObject prepareContact(final Contact con, final String sortLetter) {
        JsonArray phoneList = new JsonArray();

        JsonObject phone = new JsonObject();
        phone.addProperty("label", "Mobile"); // TODO:
        phone.addProperty("number", con.getNumber());
        phoneList.add(phone);

        if (phoneList.size() > 7) {
            for (int i = 6; i < phoneList.size(); i++) {
                phoneList.remove(i);
            }
        }

        JsonObject contact = new JsonObject();
        contact.addProperty("uuid", truncateByLen(con.getContactId(), 32));
        contact.addProperty("sortLetter", sortLetter);
        contact.addProperty("name", truncateByLen(con.getName(), 28));
        contact.add("phoneList", phoneList);
        contact.addProperty("sortKey", truncateByLen(removeNonAscii(con.getName()), 56));

        return contact;
    }

    private List<String> prepareContactsByLetters(final List<? extends Contact> contacts, int operation, final String majorVersion, int minorVersion) {
        TreeMap<String, List<JsonObject>> map = new TreeMap<>();
        for (Contact c : contacts) {
            String sortLetter = getLetter(c.getName());
            if (!map.containsKey(sortLetter)) {
                map.put(sortLetter, new ArrayList<>());
            }
            map.get(sortLetter).add(prepareContact(c, sortLetter));
        }
        return prepareDataWithHeader(operation, majorVersion, minorVersion, map);
    }

    private byte[] getContactsFileContent(String majorVersion, int minorVersion) {

        final List<Contact> contacts = DBHelper.getContacts(manager.getSupportProvider().getDevice());
        List<String> res = prepareContactsByLetters(contacts, 1, majorVersion, minorVersion);

        int len = 1; // flag len, 1 byte
        if(res.isEmpty()) {
             len += 1;
        } else {
            for (String s : res) {
                len += 4 + s.getBytes(StandardCharsets.UTF_8).length; // length 4 bytes + data length
            }
        }

        ByteBuffer sendData = ByteBuffer.allocate(len);
        sendData.put((byte) 0); // sync flag ??
        if(res.isEmpty()) {
            sendData.put((byte) 0); // no data
        } else {
            for (String s : res) {
                byte[] dataBytes = s.getBytes(StandardCharsets.UTF_8);
                sendData.putInt(dataBytes.length);
                sendData.put(dataBytes);
            }
        }
        return sendData.array();
    }

    private void sendContactsFile(String majorVersion, int minorVersion) {
        LOG.info("Send contacts file upload info");

        if (majorVersion == null || majorVersion.isEmpty()) {
            majorVersion = new String(this.manager.getSupportProvider().getAndroidId(), StandardCharsets.UTF_8).toLowerCase();
        }

        byte[] data = getContactsFileContent(majorVersion, minorVersion);

        LOG.info(GB.hexdump(data));

        HuaweiUploadManager.FileUploadInfo fileInfo = new HuaweiUploadManager.FileUploadInfo();

        fileInfo.setFileType((byte) 7);
        fileInfo.setFileName("contacts.json");
        fileInfo.setBytes(data);
        fileInfo.setSrcPackage(this.getModule());
        fileInfo.setDstPackage(this.getPackage());
        fileInfo.setSrcFingerprint(this.getLocalFingerprint());
        fileInfo.setDstFingerprint(this.getFingerprint());

        fileInfo.setFileUploadCallback(new HuaweiUploadManager.FileUploadCallback() {
            @Override
            public void onUploadStart() {
                // TODO: set device as busy in this case. But maybe exists another way to do this. Currently user see text on device card.
                // Also text should be changed
                manager.getSupportProvider().getDevice().setBusyTask(R.string.updating_firmware, manager.getSupportProvider().getContext());
                manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
            }

            @Override
            public void onUploadProgress(int progress) {
            }

            @Override
            public void onUploadComplete() {
                if (manager.getSupportProvider().getDevice().isBusy()) {
                    manager.getSupportProvider().getDevice().unsetBusyTask();
                    manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
                }
            }

            @Override
            public void onError(int code) {
                if (manager.getSupportProvider().getDevice().isBusy()) {
                    manager.getSupportProvider().getDevice().unsetBusyTask();
                    manager.getSupportProvider().getDevice().sendDeviceUpdateIntent(manager.getSupportProvider().getContext());
                }
            }
        });
        HuaweiUploadManager huaweiUploadManager = this.manager.getSupportProvider().getUploadManager();

        huaweiUploadManager.setFileUploadInfo(fileInfo);

        try {
            SendFileUploadInfo sendFileUploadInfo = new SendFileUploadInfo(this.manager.getSupportProvider(), huaweiUploadManager);
            sendFileUploadInfo.doPerform();
        } catch (IOException e) {
            LOG.error("Failed to send file upload info", e);
        }
    }

    public void startSync() {
        HuaweiTLV tlv = new HuaweiTLV();
        tlv.put(0x01, (byte) 0x01);
        sendCommand(tlv.serialize(), null);
    }

    @Override
    public void handleData(byte[] data) {
        LOG.info("HuaweiP2PContactsService handleData");
        try {
            HuaweiTLV tlv = new HuaweiTLV();
            tlv.parse(data);
            LOG.error(tlv.toString());
            int operateMode = tlv.getAsInteger(0x01, -1);
            if (operateMode == 1) {
                String majorVersion = null;
                int minorVersion = 0;
                int count = -1;
                if (tlv.contains(0x2))
                    majorVersion = tlv.getString(0x2).trim();
                if (tlv.contains(0x3))
                    minorVersion = tlv.getAsInteger(0x3, 0);
                if (tlv.contains(0x4))
                    count = tlv.getAsInteger(0x4, -1);

                LOG.info("HuaweiP2PContactsService operateMode: 1, majorVersion: {}, minorVersion: {}, count: {}", majorVersion, minorVersion, count);
                sendContactsFile(majorVersion, minorVersion);
            } else if (operateMode == 2) {
                String majorVersion = null;
                int minorVersion = 0;
                int count = -1;
                if (tlv.contains(0x2))
                    majorVersion = tlv.getString(0x2).trim();
                if (tlv.contains(0x3))
                    minorVersion = tlv.getAsInteger(0x3, 0);
                if (tlv.contains(0x4))
                    count = tlv.getAsInteger(0x4, -1);

                LOG.info("HuaweiP2PContactsService operateMode: 2, majorVersion: {}, minorVersion: {}, count: {}", majorVersion, minorVersion, count);
                //status: 0 - ok, 1 - no contacts, 2 - sync in progress, 3 - sync turned off, 4 - no permissions, 5 - not supported
                byte status = 3;

                final List<Contact> contacts = DBHelper.getContacts(manager.getSupportProvider().getDevice());
                if (!contacts.isEmpty()) {
                    status = 0;
                }

                HuaweiTLV tlv2 = new HuaweiTLV();
                tlv2.put(0x01, (byte) 0x02);
                tlv2.put(0x08, status);
                sendCommand(tlv2.serialize(), null);

                if(status == 0) {
                    sendContactsFile(majorVersion, minorVersion);
                }

            } else if (operateMode == 3) {
                int status = -1;
                if (tlv.contains(0x8))
                    status = tlv.getAsInteger(0x8);
                if (status != 0) {
                    LOG.error("send contacts fail status: {}", status);
                }
            } else {
                LOG.error("unknown operateMode : {}", operateMode);
            }
        } catch (HuaweiPacket.MissingTagException e) {
            LOG.error("Failed to handle p2p contacts", e);
        }
    }
}
