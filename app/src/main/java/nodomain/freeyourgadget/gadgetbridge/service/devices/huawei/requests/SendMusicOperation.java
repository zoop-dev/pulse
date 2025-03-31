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

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.MusicControl;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendMusicOperation extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(SendMusicOperation.class);

    private final int operation;
    private final int playlistIndex;
    private final String playlistName;
    private final ArrayList<Integer> musicIds;


    public SendMusicOperation(HuaweiSupportProvider support, int operation, int playlistIndex, String playlistName, ArrayList<Integer> musicIds) {
        super(support);
        this.serviceId = MusicControl.id;
        this.commandId = MusicControl.MusicOperation.id;
        this.operation = operation;
        this.playlistIndex = playlistIndex;
        this.playlistName = playlistName;
        this.musicIds = musicIds;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new MusicControl.MusicOperation.Request(paramsProvider, operation, playlistIndex, playlistName, musicIds).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseTypeMismatchException {
        LOG.debug("handle Music Operation");
        if (!(receivedPacket instanceof MusicControl.MusicOperation.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, MusicControl.MusicOperation.Response.class);

        MusicControl.MusicOperation.Response resp = (MusicControl.MusicOperation.Response) (receivedPacket);
        supportProvider.getHuaweiMusicManager().onMusicOperationResponse(resp.resultCode, resp.operation, resp.playlistIndex, resp.playlistName, resp.musicIds);
    }

}
