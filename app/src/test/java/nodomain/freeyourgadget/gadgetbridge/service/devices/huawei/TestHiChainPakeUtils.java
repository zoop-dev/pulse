/*  Copyright (C) 2026 Vitaliy Tomin

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.junit.Assert;
import org.junit.Test;

public class TestHiChainPakeUtils {

    private static byte[] hex(String s) {
        byte[] o = new byte[s.length() / 2];
        for (int i = 0; i < o.length; i++) {
            o[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return o;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) {
            sb.append(String.format("%02x", x));
        }
        return sb.toString();
    }

    /**
     * Elligator2 hash-to-point, validated byte-for-byte against the OpenHarmony device_auth
     * reference implementation (crypto_hash_to_point.c OpensslHashToPoint). Inputs and outputs
     * are the exact bytes produced by that C code.
     */
    @Test
    public void testHashToCurve25519() {
        String[][] vectors = {
            {"0000000000000000000000000000000000000000000000000000000000000001",
             "8488ac5e043ea853cd14e1a0d9b13abf534298f96509d669b0ea87cbf09bb673"},
            {"0102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f20",
             "4e4c39ac31d3b3b16617427847178670e6afce5430eaa6aa41166cd7b1552525"},
            {"deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef00",
             "281f4575bc3210b5e6031561b1dfdacfadd91220fad52dab911c99e7e8e92d56"},
            {"fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0",
             "39cefaa8000c28b1501cee3f00eb5b997e897979a0c854ff5924f32de8103a7e"},
            {"abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
             "d3e239d7180ca04349edbd3751eca41fd3c2856bc64319cd5d9649764651a87d"},
        };
        for (String[] v : vectors) {
            Assert.assertEquals(v[1], hex(HiChainPakeUtils.hashToCurve25519(hex(v[0]))));
        }
    }

    /** RFC 7748 section 5.2 X25519 test vector 1 (little-endian). */
    @Test
    public void testX25519Rfc7748() {
        byte[] scalar = hex("a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4");
        byte[] uIn = hex("e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c");
        Assert.assertEquals(
                "c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552",
                hex(HiChainPakeUtils.x25519(scalar, uIn)));
    }

    /** First-pair PIN: UPPERCASE hex of the first 6 bytes of the shared secret. */
    @Test
    public void testDerivePin() {
        byte[] shared = hex("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789");
        Assert.assertEquals("ABCDEF012345", HiChainPakeUtils.derivePin(shared));
    }

    /**
     * Ed25519 (RFC 8032) identity signing for the PAKE exchange step, validated against
     * pyca/cryptography (deterministic PureEdDSA).
     */
    @Test
    public void testEd25519() {
        byte[] seed = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        byte[] msg = hex("deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef");
        String expPub = "03a107bff3ce10be1d70dd18e74bc09967e4d6309ba50d5f1ddc8664125531b8";
        String expSig = "3771fe931eacab89e3f74565a75c44ee25722c0c0f5359b01c48f35d419c6940"
                + "f4966019fad4f4db18ca85e6724062735722465dea3450d758a916c8f1c2130f";

        byte[] pub = HiChainPakeUtils.ed25519PublicKey(seed);
        Assert.assertEquals(expPub, hex(pub));

        byte[] sig = HiChainPakeUtils.ed25519Sign(seed, msg);
        Assert.assertEquals(expSig, hex(sig));

        Assert.assertTrue(HiChainPakeUtils.ed25519Verify(msg, sig, pub));

        byte[] tampered = sig.clone();
        tampered[0] ^= 0x01;
        Assert.assertFalse(HiChainPakeUtils.ed25519Verify(msg, tampered, pub));
    }

    /**
     * Ed25519 -> X25519 conversion for the reconnect PSK. Two independent checks, both
     * dependency-free:
     *  1. Self-consistency: X25519(ed25519PrivateToX25519(seed), 9) must equal
     *     ed25519PublicToX25519(ed25519PublicKey(seed)). Since x25519() and ed25519PublicKey() are
     *     validated above, this pins the Montgomery u = (1+y)/(1-y) map to the standard basepoint
     *     (u=9) correspondence - i.e. exactly the libsodium/RFC 7748 conversion the watch uses.
     *  2. Two-party agreement: both identities derive the same static PSK from own-priv * peer-pub,
     *     which is the property the STS handshake relies on.
     */
    @Test
    public void testEd25519ToX25519Psk() {
        byte[] seedA = hex("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f");
        byte[] seedB = hex("a1b2c3d4e5f60718293a4b5c6d7e8f900f1e2d3c4b5a69788796a5b4c3d2e1f0");

        // 1. self-consistency for both identities
        byte[] basepoint = new byte[32];
        basepoint[0] = 9;
        for (byte[] seed : new byte[][]{seedA, seedB}) {
            byte[] fromPriv = HiChainPakeUtils.x25519(
                    HiChainPakeUtils.ed25519PrivateToX25519(seed), basepoint);
            byte[] fromPub = HiChainPakeUtils.ed25519PublicToX25519(
                    HiChainPakeUtils.ed25519PublicKey(seed));
            Assert.assertEquals(hex(fromPriv), hex(fromPub));
        }

        // 2. two-party PSK agreement (own priv * peer pub == peer priv * own pub)
        byte[] pubA = HiChainPakeUtils.ed25519PublicKey(seedA);
        byte[] pubB = HiChainPakeUtils.ed25519PublicKey(seedB);
        byte[] pskA = HiChainPakeUtils.computeStsPsk(seedA, pubB);
        byte[] pskB = HiChainPakeUtils.computeStsPsk(seedB, pubA);
        Assert.assertEquals(hex(pskA), hex(pskB));

        // PSK must be non-trivial
        byte[] zero = new byte[32];
        Assert.assertNotEquals(hex(zero), hex(pskA));
    }
}
