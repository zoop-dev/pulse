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

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetAdaptiveVolumeRequest extends Request {
    public enum Mode {
        OFF((byte) -1),
        LOW((byte) 0x00),
        DEFAULT((byte) 0x01),
        HIGH((byte) 0x02);

        private final byte sensitivity;

        Mode(byte sensitivity) {
            this.sensitivity = sensitivity;
        }

        public static Mode fromPreference(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                return OFF;
            }
        }

        public static Mode fromResponse(Earphones.AdaptiveVolume.Response response) {
            if (!response.enabled) {
                return OFF;
            }
            switch (response.sensitivity) {
                case 0x00:
                    return LOW;
                case 0x02:
                    return HIGH;
                default:
                    return DEFAULT;
            }
        }

        public String toPreference() {
            return name().toLowerCase();
        }
    }

    public SetAdaptiveVolumeRequest(HuaweiSupportProvider supportProvider) {
        super(supportProvider);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.AdaptiveVolume.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
            Mode mode = Mode.fromPreference(prefs.getString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ADAPTIVE_VOLUME, "off"));
            Mode appliedMode = Mode.fromPreference(prefs.getString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ADAPTIVE_VOLUME_APPLIED, "off"));
            List<byte[]> requests = new ArrayList<>();

            if (mode == Mode.OFF) {
                requests.addAll(new Earphones.AdaptiveVolume.SetRequest(this.paramsProvider, false).serialize());
            } else {
                if (appliedMode == Mode.OFF) {
                    requests.addAll(new Earphones.AdaptiveVolume.SetRequest(this.paramsProvider, true).serialize());
                }
                requests.addAll(new Earphones.AdaptiveVolume.SetSensitivityRequest(this.paramsProvider, mode.sensitivity).serialize());
            }

            prefs.edit().putString(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_ADAPTIVE_VOLUME_APPLIED, mode.toPreference()).apply();
            return requests;
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
