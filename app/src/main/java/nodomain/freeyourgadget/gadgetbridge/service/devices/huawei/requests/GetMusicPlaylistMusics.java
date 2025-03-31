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

public class GetMusicPlaylistMusics extends Request {
    private final Logger LOG = LoggerFactory.getLogger(GetMusicPlaylistMusics.class);

    private final int playlist;
    private final int index;

    public GetMusicPlaylistMusics(HuaweiSupportProvider support, int playlist, int index) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicPlaylistMusics.id;
        this.playlist = playlist;
        this.index = index;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicPlaylistMusics.Request(paramsProvider, (short) playlist, (short) index).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.info("MusicControl.GetMusicPlaylistMusics processResponse");
        if (!(receivedPacket instanceof MusicControl.MusicPlaylistMusics.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicPlaylistMusics.Response.class);

        MusicControl.MusicPlaylistMusics.Response resp = (MusicControl.MusicPlaylistMusics.Response) (receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicPlaylistMusics(resp.id, resp.index, resp.musicIds);
    }
}
