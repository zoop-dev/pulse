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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.OTA;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendOTAProgress extends Request {

    private final byte progress;
    private final byte state;
    private final byte mode;

    public SendOTAProgress(HuaweiSupportProvider support,
                          byte progress,
                           byte state,
                           byte mode) {
        super(support);
        this.serviceId = OTA.id;
        this.commandId = OTA.Progress.id;

        this.progress = progress;
        this.state = state;
        this.mode = mode;
        this.addToResponse = false;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new OTA.Progress.Request(paramsProvider, this.progress, this.state, this.mode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
