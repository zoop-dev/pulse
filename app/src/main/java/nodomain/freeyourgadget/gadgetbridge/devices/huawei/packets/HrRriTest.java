package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class HrRriTest {
    public static final byte id = 0x19;

    public static class OpenOrClose {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte type) {
                super(paramsProvider);

                this.serviceId = HrRriTest.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x1, type);

                this.isEncrypted = true;
                this.complete = true;
            }
        }

        public static class Response extends HuaweiPacket {

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {

            }
        }
    }

    public static class RriData {
        public static final byte id = 0x05;

        public static class Response extends HuaweiPacket {
            public static class rriSqiData {
                public short rri;
                public byte sqi;

                @Override
                public String toString() {
                    return "SubContainer{" +
                            "rri=" + rri +
                            ", sqi=" + sqi +
                            '}';
                }
            }

            public List<rriSqiData> containers;

            public Response(ParamsProvider paramsProvider) {
                super(paramsProvider);
            }

            @Override
            public void parseTlv() throws ParseException {
                HuaweiTLV container = this.tlv.getObject(0x82);
                List<HuaweiTLV> subContainers = container.getObjects(0x83);
                this.containers = new ArrayList<>();
                for (HuaweiTLV subContainerTlv : subContainers) {
                    rriSqiData rriSqi = new rriSqiData();
                    rriSqi.rri = subContainerTlv.getShort(0x04);
                    rriSqi.sqi = subContainerTlv.getByte(0x05);
                    this.containers.add(rriSqi);
                }
            }
        }
    }

}
