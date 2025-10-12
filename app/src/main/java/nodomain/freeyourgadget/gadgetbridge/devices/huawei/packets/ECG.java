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

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;

public class ECG {
    public static final byte id = 0x23;

    public static class SetECGOpen {
        public static final byte id = 0x10;

        public static class Request extends HuaweiPacket {
            public Request(ParamsProvider paramsProvider, byte state) {
                super(paramsProvider);

                this.serviceId = ECG.id;
                this.commandId = id;

                this.tlv = new HuaweiTLV().put(0x1, state);

                this.isEncrypted = true;
                this.complete = true;
            }
        }
    }
}
