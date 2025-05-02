/*  Copyright (C) 2024 Me7c7, Vitalii Tomin

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

public class App {
    public static final byte id = 0x2a;

    public static class AppDeviceParams {
        public byte type = 0;
        public int osApiLevel = 0;
        public String osVersion = "";
        public String screenShape = "";
        public int width = 0;
        public int height = 0;
        public int buildLevel = 0;
        public String buildType = "";
    }

    public static class InstalledAppInfo {
        public String packageName;
        public String version;
        public int size;
        public String appName;
        public int appTime;
        public int versionCode;
        public byte packageType;

        public InstalledAppInfo(HuaweiTLV tlv) throws HuaweiPacket.MissingTagException {
            this.packageName = tlv.getString(0x03);
            this.version = tlv.getString(0x04);
            this.size = tlv.getInteger(0x05); // Most devices returns 0
            this.appName =  tlv.getString(0x06);
            this.appTime = tlv.getInteger(0x07); // Most devices returns 0
            this.versionCode = tlv.getInteger(0x09);
            this.packageType = tlv.getByte(0x0d);
        }
    }

    public static class AppDelete {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider,
                           String packageName) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x01, (byte)1)
                        .put(0x02, packageName);
            }
        }

        public static class Response extends HuaweiPacket {
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }
        }
    }

    public static class AppNames {
        public static final byte id = 0x03;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;
                this.tlv = new HuaweiTLV()
                        .put(0x81);
            }
        }

        public static class Response extends HuaweiPacket {

            public List<App.InstalledAppInfo> appInfoList;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                appInfoList = new ArrayList<>();
                if(this.tlv.contains(0x81)) {
                    for (HuaweiTLV subTlv : this.tlv.getObject(0x81).getObjects(0x82)) {
                        appInfoList.add(new InstalledAppInfo(subTlv));
                    }
                }
            }
        }
    }

    public static class AppInfoParams {
        public static final byte id = 0x06;
        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = App.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x81);

                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {
            public AppDeviceParams params = new AppDeviceParams();
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                if(this.tlv.contains(0x81)) {
                    HuaweiTLV subTlv = this.tlv.getObject(0x81).getObject(0x82);
                    this.params.type = subTlv.getByte(0x03); // 38 - liteWearable
                    this.params.osApiLevel = subTlv.getInteger(0x04);
                    this.params.osVersion = subTlv.getString(0x05);
                    if (subTlv.contains(0x06))
                        this.params.screenShape = subTlv.getString(0x06);
                    if (subTlv.contains(0x07))
                        this.params.width = subTlv.getShort(0x07);
                    if (subTlv.contains(0x08))
                        this.params.height = subTlv.getShort(0x08);
                    if (subTlv.contains(0x09))
                        this.params.buildLevel = subTlv.getInteger(0x09);
                    if(subTlv.contains(0x0a))
                        this.params.buildType = subTlv.getString(0x0a);
                }
            }
        }
    }

}
