/*  Copyright (C) 2024 Me7c7

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetExtendedMusicInfoParams extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetExtendedMusicInfoParams.class);
    public GetExtendedMusicInfoParams(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.ExtendedMusicInfoParams.id;

    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsMusicUploading();
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.ExtendedMusicInfoParams.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.ExtendedMusicInfoParams processResponse");
        if (!(receivedPacket instanceof MusicControl.ExtendedMusicInfoParams.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.ExtendedMusicInfoParams.Response.class);

        MusicControl.ExtendedMusicInfoParams.Response resp = (MusicControl.ExtendedMusicInfoParams.Response)(receivedPacket);
        supportProvider.getHuaweiCoordinator().setExtendedMusicInfoParams(resp.params);
    }
}
