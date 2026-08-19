/*  Copyright (C) 2026 rejunte

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiManualSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.XiaomiActivityFileId;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ManualSamplesParserTest {
    private static final String SPO2_97_PACKET = "EFB9856AF4011800FFEFB9856A0261511E4648";
    private static final String STRESS_40_PACKET = "65C0856AF4011800FF65C0856A03288CA20578";
    private static final String STRESS_27_PACKET = "D1C3856AF4011800FFD1C3856A031BFBF91375";

    @Test
    public void decodeVersion1Spo2() {
        final List<XiaomiManualSample> samples = decode(SPO2_97_PACKET);

        assertNotNull(samples);
        assertEquals(1, samples.size());
        assertEquals(XiaomiManualSampleProvider.TYPE_SPO2, samples.get(0).getType().intValue());
        assertEquals(97, samples.get(0).getValue().intValue());
        assertEquals(1787148783000L, samples.get(0).getTimestamp());
    }

    @Test
    public void decodeVersion1Stress() {
        final List<XiaomiManualSample> stress40 = decode(STRESS_40_PACKET);
        final List<XiaomiManualSample> stress27 = decode(STRESS_27_PACKET);

        assertNotNull(stress40);
        assertEquals(1, stress40.size());
        assertEquals(XiaomiManualSampleProvider.TYPE_STRESS, stress40.get(0).getType().intValue());
        assertEquals(40, stress40.get(0).getValue().intValue());

        assertNotNull(stress27);
        assertEquals(1, stress27.size());
        assertEquals(XiaomiManualSampleProvider.TYPE_STRESS, stress27.get(0).getType().intValue());
        assertEquals(27, stress27.get(0).getValue().intValue());
    }

    @Test
    public void decodeVersion1RejectsUnknownHeader() {
        final byte[] packet = GB.hexStringToByteArray(SPO2_97_PACKET);
        packet[8] = 0;

        assertNull(decode(packet));
    }

    @Test
    public void decodeVersion1RejectsUnknownType() {
        final byte[] packet = GB.hexStringToByteArray(SPO2_97_PACKET);
        packet[13] = 0x04;

        assertNull(decode(packet));
    }

    @Test
    public void decodeVersion1RejectsOutOfRangeValue() {
        final byte[] packet = GB.hexStringToByteArray(SPO2_97_PACKET);
        packet[14] = 101;

        assertNull(decode(packet));
    }

    private static List<XiaomiManualSample> decode(final String hex) {
        return decode(GB.hexStringToByteArray(hex));
    }

    private static List<XiaomiManualSample> decode(final byte[] packet) {
        final XiaomiActivityFileId fileId = XiaomiActivityFileId.from(Arrays.copyOfRange(packet, 0, 7));
        return ManualSamplesParser.decodeVersion1(fileId, packet);
    }
}
