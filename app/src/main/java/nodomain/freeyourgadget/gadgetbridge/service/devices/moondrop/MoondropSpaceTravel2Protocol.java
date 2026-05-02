/*  Copyright (C) 2026 Jan Petrlík

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moondrop;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_MOONDROP_ANC_MODE;

public class MoondropSpaceTravel2Protocol extends MoondropSpaceTravelProtocol {
    private static final byte AUDIO_CURATION_FEATURE = (byte)0x08;
    private static final byte AUDIO_CURATION_PDU_GET_MODE = (byte)0x03;
    private static final byte AUDIO_CURATION_PDU_SET_MODE = (byte)0x04;

    protected MoondropSpaceTravel2Protocol(GBDevice device) {
        super(device);
    }

    @Override
    protected GBDeviceEvent decodePacket(short featureId, short pduId, byte[] payload) {
        if (featureId == AUDIO_CURATION_FEATURE && pduId == AUDIO_CURATION_PDU_GET_MODE)
            return handlePacketAudioCurationMode(payload);
        return super.decodePacket(featureId, pduId, payload);
    }

    @Override
    public byte[] encodeSendConfiguration(String config) {
        if (PREF_MOONDROP_ANC_MODE.equals(config))
            return encodeSetAudioCurationMode();
        return super.encodeSendConfiguration(config);
    }

    public byte[] encodeGetAudioCurationMode() {
        return new GaiaPacket(AUDIO_CURATION_FEATURE, AUDIO_CURATION_PDU_GET_MODE).encode();
    }

    private byte[] encodeSetAudioCurationMode() {
        Prefs prefs = getDevicePrefs();
        byte mode = Byte.parseByte(prefs.getString(PREF_MOONDROP_ANC_MODE, "1"));

        byte[] payload = new byte[] { mode };

        return new GaiaPacket(AUDIO_CURATION_FEATURE, AUDIO_CURATION_PDU_SET_MODE, payload).encode();
    }

    private GBDeviceEvent handlePacketAudioCurationMode(byte[] payload) {
        if (payload.length < 1)
            return null;

        // GET_MODE returns a 0-based slot index, SET_MODE and the UI dropdown use a
        // bitmask (Normal=1, ANC=2, Transparency=4). Convert before storing the
        // preference.
        int slot = payload[0] & 0xff;
        if (slot > 2)
            return null;
        int mode = 1 << slot;

        return new GBDeviceEventUpdatePreferences(PREF_MOONDROP_ANC_MODE, String.valueOf(mode));
    }
}
