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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiOTAManager;

public class OTA {
    public static final byte id = 0x09;

    public static class StartQuery {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, String firmwareVersion, short fileId, byte operation, boolean add) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x01, firmwareVersion)
                        .put(0x02, fileId)
                        .put(0x03, operation);
                if(add)
                    this.tlv.put(0x05, (byte)5);
            }
        }

        public static class Response extends HuaweiPacket {
            public byte batteryThreshold;
            public int respCode;

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x4)) {
                    batteryThreshold = this.tlv.getByte(0x4);
                }
                if(this.tlv.contains(0x7f)) {
                    respCode = this.tlv.getInteger(0x7f);
                }
            }

        }
    }

    public static class DataParams {
        public static final byte id = 0x02;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV();
            }
        }

        public static class Response extends HuaweiPacket {
            public HuaweiOTAManager.UploadInfo info = new HuaweiOTAManager.UploadInfo();

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x1))
                    info.waitTimeout = this.tlv.getAsInteger(0x1);
                if(this.tlv.contains(0x2))
                    info.restartTimeout = this.tlv.getAsInteger(0x2);

                info.maxUnitSize = this.tlv.getAsInteger(0x3);

                if(this.tlv.contains(0x4))
                    info.interval = this.tlv.getAsLong(0x4);
                if(this.tlv.contains(0x5))
                    info.ack = (this.tlv.getAsLong(0x5) == 1);
                if(this.tlv.contains(0x6))
                    info.offset = (this.tlv.getAsLong(0x6) == 1);
            }
        }
    }

    public static class DataChunkRequest {
        public static final byte id = 0x03;

        public static class Response extends HuaweiPacket {
            public Integer errorCode = null;
            public long fileOffset;
            public long chunkLength;
            public byte[] bitmap = null;
            public long wifiSend = 0;

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x7f)) {
                    errorCode = this.tlv.getInteger(0x7f);
                    return;
                }

                fileOffset = this.tlv.getAsLong(0x1);
                chunkLength = this.tlv.getAsLong(0x2);
                if(this.tlv.contains(0x3))
                    bitmap = this.tlv.getBytes(0x3);
                if(this.tlv.contains(0x4))
                    wifiSend = this.tlv.getAsLong(0x4);
            }
        }
    }

    public static class NextChunkSend extends HuaweiPacket {
        public static final byte id = 0x04;

        public NextChunkSend(ParamsProvider paramsProvider) {
            super(paramsProvider);
            this.serviceId = OTA.id;
            this.commandId = id;
            this.complete = true;
        }
    }

    public static class SizeReport {
        public static final byte id = 0x05;

        public static class Response extends HuaweiPacket {

            public long size = 0;
            public long current = 0;

            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x1))
                    size = this.tlv.getAsLong(0x1);
                if(this.tlv.contains(0x2))
                    current = this.tlv.getAsLong(0x2);
            }
        }
    }

    public static class UpdateResult {
        public static final byte id = 0x06;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV();
            }
        }

        public static class Response extends HuaweiPacket {
            public int resultCode = 0; // 0 fail, 1 -success
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x01)) {
                    resultCode = this.tlv.getAsInteger(0x01);
                }
            }
        }
    }

    public static class DeviceError {
        public static final byte id = 0x07;

        public static class Response extends HuaweiPacket {
            public int errorCode;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x7f)) {
                    errorCode = this.tlv.getInteger(0x7f);
                }
            }
        }
    }

    public static class SetStatus {
        public static final byte id = 0x09;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, Byte useWifi) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV().put(0x01, (byte)0x01);
                if(useWifi != null) {
                    this.tlv.put(0x02, useWifi);
                }
            }
        }
    }


    public static class SetAutoUpdate {
        public static final byte id = 0x0c;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, boolean state) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV().put(0x01, state);
            }
        }

        public static class Response extends HuaweiPacket {
            public int respCode;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x7f)) {
                    respCode = this.tlv.getInteger(0x7f);
                }
            }

        }
    }

    public static class NotifyNewVersion {
        public static final byte id = 0x0e;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, String newSoftwareVersion, long fileSize, byte unkn1, byte unkn2) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x01, newSoftwareVersion)
                        .put(0x02, fileSize)
                        .put(0x03, System.currentTimeMillis() / 1000)
                        .put(0x04, unkn1) // NOTE: 1, 2 or 3. If not 1 do not add 0x01 and 0x02 tlv
                        .put(0x05, unkn2); // NOTE: 2 or 0. I don't understand meaning of this byte
            }
        }

        public static class Response extends HuaweiPacket {
            public int respCode;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x7f)) {
                    respCode = this.tlv.getInteger(0x7f);
                }
            }

        }
    }

    public static class DeviceRequest {
        public static final byte id = 0x0f;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, int code) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x7f, code);
            }
        }

        public static class Response extends HuaweiPacket {
            public byte unkn1;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x01)) {
                    unkn1 = this.tlv.getByte(0x01);
                }
            }
        }
    }

    public static class Progress {
        public static final byte id = 0x12;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, byte percent, byte state, byte mode) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x01, percent)
                        .put(0x02, state)
                        .put(0x03, mode);
            }
        }
    }

    public static class GetMode {
        public static final byte id = 0x13;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted
            }

            @Override
            public List<byte[]> serialize() throws CryptoException {
                return super.serializeOTAGetMode();
            }
        }

        public static class Response extends HuaweiPacket {
            public byte mode = (byte) 255;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x01)) {
                    mode = this.tlv.getByte(0x01);
                }
            }

        }
    }

    public static class SetChangeLog {
        public static final byte id = 0x14;

        public static class Request extends HuaweiPacket {

            // TODO: discover how to proper encode changelog
            //    result = 0 - no changelog data
            public Request(ParamsProvider paramsProvider, String version, byte result) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x01, version)
                        .put(0x02, result);
            }
        }

        public static class Response extends HuaweiPacket {
            public int respCode;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x7f)) {
                    respCode = this.tlv.getInteger(0x7f);
                }
            }

        }
    }

    public static class GetChangeLog {
        public static final byte id = 0x15;

        public static class Request extends HuaweiPacket {

            public Request(ParamsProvider paramsProvider, String version, String language) {
                super(paramsProvider);
                this.serviceId = OTA.id;
                this.commandId = id;
                this.isEncrypted = false; // NOTE: not sure but looks like unencrypted

                this.tlv = new HuaweiTLV()
                        .put(0x01, version)
                        .put(0x02, language);
            }
        }

        public static class Response extends HuaweiPacket {
            byte result;
            String uri;
            public Response (ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws HuaweiPacket.ParseException {
                if(this.tlv.contains(0x03))
                    result = this.tlv.getByte(0x03);
                if(this.tlv.contains(0x04))
                    uri = this.tlv.getString(0x04);
            }
        }
    }
}
