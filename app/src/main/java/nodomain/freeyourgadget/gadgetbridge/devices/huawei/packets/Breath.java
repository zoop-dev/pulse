package nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class Breath {
    public static final byte id = 0x2d;

    public static class SleepBreath {
        public static final byte id = 0x01;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte type) {
                super(paramsProvider);

                this.serviceId = Breath.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x1, type);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }
}
