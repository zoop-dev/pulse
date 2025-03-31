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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.datasync.HuaweiDataSyncCommon;

public class DataSync {
    public static final byte id = 0x37;

    public static int getError(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
        if (tlv.contains(0x7f)) {
            return tlv.getInteger(0x7f);
        }
        return HuaweiDataSyncCommon.REPLY_OK;
    }

    public static class ConfigCommand {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider,
                           String srcPackage,
                           String dstPackage,
                           HuaweiDataSyncCommon.ConfigCommandData sendData) {
                super(paramsProvider);
                this.serviceId = DataSync.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, srcPackage)
                        .put(0x02, dstPackage);
                // TODO: potentially can be not 83 tlv.
                if(!sendData.getConfigDataList().isEmpty()) {
                    HuaweiTLV tlvList = new HuaweiTLV();
                    for(HuaweiDataSyncCommon.ConfigData dt: sendData.getConfigDataList()) {
                        HuaweiTLV item = new HuaweiTLV();
                        if(dt.configId != -1) {
                            item.put(0x05, dt.configId);
                        }
                        if(dt.configAction != -1) {
                            item.put(0x06, dt.configAction);
                        }
                        if(dt.configData != null) {
                            item.put(0x07, dt.configData);
                        }
                        if(dt.configUnknown != -1) {
                            item.put(0x08, dt.configUnknown);
                        }
                        tlvList.put(0x84, item);
                    }
                    this.tlv.put(0x83, tlvList);
                }
            }
        }

        public static class Response extends HuaweiPacket {
            public String srcPackage;
            public String dstPackage;
            public HuaweiDataSyncCommon.ConfigCommandData data = new HuaweiDataSyncCommon.ConfigCommandData();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                srcPackage = this.tlv.getString(0x01);
                dstPackage = this.tlv.getString(0x02);
                int returnCode = getError(this.tlv);
                if(returnCode != HuaweiDataSyncCommon.REPLY_OK) {
                    data.setCode(returnCode);
                    return;
                }
                // TODO: potentially can be not 83 tlv.
                if(this.tlv.contains(0x83)) {
                    List<HuaweiDataSyncCommon.ConfigData> respData = new ArrayList<>();
                    List<HuaweiTLV> subContainers = this.tlv.getObject(0x83).getObjects(0x84);
                    for(HuaweiTLV subTlv: subContainers) {
                        HuaweiDataSyncCommon.ConfigData dt = new HuaweiDataSyncCommon.ConfigData();
                        if(subTlv.contains(0x05)) {
                            dt.configId = subTlv.getInteger(0x05);
                        }
                        if(subTlv.contains(0x06)) {
                            dt.configAction = subTlv.getByte(0x06);
                        }
                        if(subTlv.contains(0x07)) {
                            dt.configData = subTlv.getBytes(0x07);
                        }
                        if(subTlv.contains(0x08)) {
                            dt.configUnknown = subTlv.getLong(0x08);
                        }
                        respData.add(dt);
                    }
                    data.setConfigDataList(respData);
                }
            }

        }
    }

    public static class EventCommand {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider,
                           String srcPackage,
                           String dstPackage,
                           HuaweiDataSyncCommon.EventCommandData sendData) {
                super(paramsProvider);
                this.serviceId = DataSync.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, srcPackage)
                        .put(0x02, dstPackage)
                        .put(0x03, sendData.getEventId())
                        .put(0x04, sendData.getEventLevel())
                        .put(0x05, sendData.getData());
            }
        }

        public static class Response extends HuaweiPacket {
            public String srcPackage;
            public String dstPackage;
            public HuaweiDataSyncCommon.EventCommandData data = new HuaweiDataSyncCommon.EventCommandData();

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                srcPackage = this.tlv.getString(0x01);
                dstPackage = this.tlv.getString(0x02);
                int returnCode = getError(this.tlv);
                if(returnCode != HuaweiDataSyncCommon.REPLY_OK) {
                    data.setCode(returnCode);
                    return;
                }
                data.setEventId(this.tlv.getAsInteger(0x03));
                data.setEventLevel(this.tlv.getAsInteger(0x04));
                data.setData(this.tlv.getBytes(0x05));
            }
        }
    }

    public static class DataCommand {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider,
                           String srcPackage,
                           String dstPackage,
                           HuaweiDataSyncCommon.DataCommandData sendData) {
                super(paramsProvider);
                this.serviceId = DataSync.id;
                this.commandId = id;

                // TODO: discover other data
                this.tlv = new HuaweiTLV()
                        .put(0x01, srcPackage)
                        .put(0x02, dstPackage);
            }
        }

        public static class Response extends HuaweiPacket {
            public String srcPackage;
            public String dstPackage;
            public HuaweiDataSyncCommon.DataCommandData data = new HuaweiDataSyncCommon.DataCommandData();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                srcPackage = this.tlv.getString(0x01);
                dstPackage = this.tlv.getString(0x02);
                int returnCode = getError(this.tlv);
                if(returnCode != HuaweiDataSyncCommon.REPLY_OK) {
                    data.setCode(returnCode);
                    return;
                }
                // TODO: discover and decode other data
            }
        }
    }

    public static class DictDataCommand {
        public static final byte id = 0x04;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider,
                           String srcPackage,
                           String dstPackage,
                           HuaweiDataSyncCommon.DictDataCommandData sendData) {
                super(paramsProvider);
                this.serviceId = DataSync.id;
                this.commandId = id;

                // TODO: discover other data
                this.tlv = new HuaweiTLV()
                        .put(0x01, srcPackage)
                        .put(0x02, dstPackage);
            }
        }

        public static class Response extends HuaweiPacket {
            public String srcPackage;
            public String dstPackage;
            public HuaweiDataSyncCommon.DictDataCommandData data = new HuaweiDataSyncCommon.DictDataCommandData();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                srcPackage = this.tlv.getString(0x01);
                dstPackage = this.tlv.getString(0x02);
                int returnCode = getError(this.tlv);
                if(returnCode != HuaweiDataSyncCommon.REPLY_OK) {
                    data.setCode(returnCode);
                    return;
                }
                // TODO: discover other data
            }
        }
    }
}
