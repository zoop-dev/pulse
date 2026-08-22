/*  Copyright (C) 2026 Baptiste Debut

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.protobuf.ByteString;

import org.junit.Test;

import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;

public class XiaomiAuthServiceTest {
    private static final byte[] NONCE = new byte[]{
            0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77,
            (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb,
            (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff,
    };

    /// Wear OS watches need the phone's identity in the nonce, or they refuse the handshake.
    @Test
    public void testNonceCommandWithDeviceId() {
        final XiaomiProto.PhoneNonce phoneNonce = XiaomiAuthService
                .buildNonceCommand(NONCE, "0123456789abcdef0123456789abcdef")
                .getAuth()
                .getPhoneNonce();

        assertArrayEquals(NONCE, phoneNonce.getNonce().toByteArray());
        assertTrue(phoneNonce.hasDeviceId());
        assertEquals("0123456789abcdef0123456789abcdef", phoneNonce.getDeviceId());
    }

    /// Every other Xiaomi device has no device id to send, and must keep producing exactly the
    /// bytes it did before the field existed - proto2 only serializes what was explicitly set.
    @Test
    public void testNonceCommandWithoutDeviceIdIsUnchangedOnTheWire() {
        final byte[] expected = XiaomiProto.Command.newBuilder()
                .setType(XiaomiAuthService.COMMAND_TYPE)
                .setSubtype(XiaomiAuthService.CMD_NONCE)
                .setAuth(XiaomiProto.Auth.newBuilder().setPhoneNonce(
                        XiaomiProto.PhoneNonce.newBuilder()
                                .setNonce(ByteString.copyFrom(NONCE))
                                .build()
                ))
                .build()
                .toByteArray();

        for (final String noDeviceId : new String[]{null, ""}) {
            final XiaomiProto.Command cmd = XiaomiAuthService.buildNonceCommand(NONCE, noDeviceId);
            assertFalse(cmd.getAuth().getPhoneNonce().hasDeviceId());
            assertArrayEquals(expected, cmd.toByteArray());
        }
    }
}
