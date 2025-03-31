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

public class GetMusicList extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetMusicList.class);

    private final int startFrame;
    private final int endFrame;

    public GetMusicList(HuaweiSupportProvider support, int startFrame, int endFrame) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicList.id;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicList.Request(paramsProvider, (short) this.startFrame, (short) this.endFrame).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.MusicList processResponse");
        if (!(receivedPacket instanceof MusicControl.MusicList.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicList.Response.class);

        MusicControl.MusicList.Response resp = (MusicControl.MusicList.Response) (receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicListResponse(resp.startFrame, resp.endIndex, resp.musicList);

    }
}
