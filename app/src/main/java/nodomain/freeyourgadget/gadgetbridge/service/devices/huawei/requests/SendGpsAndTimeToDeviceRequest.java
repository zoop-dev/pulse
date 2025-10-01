/*  Copyright (C) 2024 Vitalii Tomin, Martin.JM

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

import android.location.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.GpsAndTime;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.p2p.HuaweiP2PContactsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.webview.CurrentPosition;

public class SendGpsAndTimeToDeviceRequest extends Request {
    private final int timestamp;
    private final double lat;
    private final double lon;

    public SendGpsAndTimeToDeviceRequest(HuaweiSupportProvider support, int timestamp, double lat, double lon) {
        super(support);
        this.serviceId = GpsAndTime.id;
        this.commandId = GpsAndTime.CurrentGPSRequest.id;
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new GpsAndTime.CurrentGPSRequest(
                    this.paramsProvider,
                    timestamp,
                    lat,
                    lon
            ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
