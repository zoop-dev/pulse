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

import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetAdaptiveVolumeRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetAdaptiveVolumeRequest.class);

    public GetAdaptiveVolumeRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.AdaptiveVolume.id;
    }

    @Override
    protected List<byte[]> createRequest() throws Request.RequestCreationException {
        try {
            return new Earphones.AdaptiveVolume.GetRequest(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new Request.RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws Request.ResponseParseException {
        LOG.debug("handle GetAdaptiveVolume");

        if (!(receivedPacket instanceof Earphones.AdaptiveVolume.Response))
            throw new Request.ResponseTypeMismatchException(receivedPacket, Earphones.AdaptiveVolume.Response.class);

        Earphones.AdaptiveVolume.Response packet = (Earphones.AdaptiveVolume.Response) receivedPacket;

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
        String mode = SetAdaptiveVolumeRequest.Mode.fromResponse(packet).toPreference();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ADAPTIVE_VOLUME, mode);
        editor.putString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ADAPTIVE_VOLUME_APPLIED, mode);
        editor.apply();
    }
}
