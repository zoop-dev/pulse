package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import java.nio.ByteBuffer;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Stress {
    public static final byte id = 0x20;

    public static class AutomaticStress {
        public static final byte id = 0x09;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte status, byte score, List<Float> feature, int time) {
                super(paramsProvider);

                this.serviceId = Stress.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV()
                        .put(0x01, status);
                if(feature != null && feature.size() == 12) {
                    this.tlv.put(0x02, score);
                    ByteBuffer feat = ByteBuffer.allocate(12 * 4);
                    for (Float f: feature) {
                        feat.putFloat(f);
                        //feat.putInt(Float.floatToIntBits(f));
                    }
                    this.tlv.put(0x03, feat.array());
                    this.tlv.put(0x04, time);
                }


                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }
}
