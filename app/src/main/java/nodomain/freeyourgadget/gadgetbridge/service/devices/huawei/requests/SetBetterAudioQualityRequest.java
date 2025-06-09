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

import android.content.SharedPreferences;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Earphones;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SetBetterAudioQualityRequest extends Request {

    public enum AudioQualityMode {
        OFF((byte) 0x00),
        ON((byte) 0x01);

        private final byte code;

        AudioQualityMode(final byte code) {
            this.code = code;
        }

        public byte getCode() {
            return this.code;
        }


        public static SetBetterAudioQualityRequest.AudioQualityMode fromBoolean(boolean state) {
            return state ? ON : OFF;
        }

        public static boolean toBoolean(SetBetterAudioQualityRequest.AudioQualityMode mode) {
            return mode == ON;
        }

        public static SetBetterAudioQualityRequest.AudioQualityMode fromPreferences(final SharedPreferences prefs) {
            return SetBetterAudioQualityRequest.AudioQualityMode.fromBoolean(prefs.getBoolean(DeviceSettingsPreferenceConst.PREF_HUAWEI_FREEBUDS_BETTER_AUDIO_QUALITY, toBoolean(OFF)));
        }
    }

    public SetBetterAudioQualityRequest(HuaweiSupportProvider supportProvider) {
        super(supportProvider);
        this.serviceId = Earphones.id;
        this.commandId = Earphones.SetBetterAudioQuality.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            SetBetterAudioQualityRequest.AudioQualityMode audioQualityMode = SetBetterAudioQualityRequest.AudioQualityMode.fromPreferences(GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress()));
            return new Earphones.SetBetterAudioQuality.Request(this.paramsProvider, audioQualityMode).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }
}
