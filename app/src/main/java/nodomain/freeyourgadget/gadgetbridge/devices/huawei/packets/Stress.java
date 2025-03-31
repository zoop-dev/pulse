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
