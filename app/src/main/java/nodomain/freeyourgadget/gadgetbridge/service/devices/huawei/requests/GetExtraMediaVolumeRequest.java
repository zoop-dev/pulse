/*  Copyright (C) 2026

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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetExtraMediaVolumeRequest extends Request {
    public GetExtraMediaVolumeRequest(HuaweiSupportProvider supportProvider) {
        super(supportProvider);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.GetExtraMediaVolume.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new Earphones.GetExtraMediaVolume.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof Earphones.GetExtraMediaVolume.Response)) {
            throw new ResponseTypeMismatchException(receivedPacket, Earphones.GetExtraMediaVolume.Response.class);
        }

        boolean enabled = ((Earphones.GetExtraMediaVolume.Response) receivedPacket).enabled;
        GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).edit()
                .putBoolean(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_EXTRA_MEDIA_VOLUME, enabled)
                .apply();
    }
}
