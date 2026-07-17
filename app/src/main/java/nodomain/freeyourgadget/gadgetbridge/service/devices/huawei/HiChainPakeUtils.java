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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;

/**
 * Implements the PSK-SPEKE (Pre-Shared Key Simple Password Authenticated Key Exchange)
 * PAKE algorithm as used by the Honor/Huawei HiChain device authentication protocol.
 *
 * Ported from the open-source OpenHarmony HiChain implementation
 * (base/security/device_auth): the SPEKE PAKE key agreement, the PAKE bind task, and the
 * authenticated key-exchange step.
 *
 * Supports two modes:
 * - EC-SPEKE (modLen=0): Uses X25519 for key exchange (version supports feature 2)
 * - DL-SPEKE (modLen=256/384): Uses modular exponentiation with Oakley Group 14/15
 *
 * Full BindRequest protocol flow (6 messages):
 *   Phase 1 - PAKE Authentication:
 *     1. PAKE Start Request  (client->server): support256mod, serviceType
 *     2. PAKE Response       (server->client): challenge, salt, epk
 *     3. Client Confirm      (client->server): challenge, epk, kcfData (proof)
 *     4. Server Confirm      (server->client): kcfData (proof)
 *   Phase 2 - Key Exchange (using PAKE session key):
 *     5. Exchange Request    (client->server): exAuthInfo (AES-GCM encrypted)
 *     6. Exchange Response   (server->client): exAuthInfo (AES-GCM encrypted)
 */
public class HiChainPakeUtils {

    private static final String TAG = "HiChainPakeUtils";

    // HKDF info strings (OpenHarmony device_auth HiChain constants)
    private static final byte[] BASE_INFO = "hichain_speke_base_info".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SESSION_KEY_INFO = "hichain_speke_sessionkey_info".getBytes(StandardCharsets.UTF_8);
    private static final byte[] RETURN_KEY_INFO = "hichain_return_key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] TMP_AUTH_KEY_INFO = "hichain_tmp_auth_enc_key".getBytes(StandardCharsets.UTF_8);
    // STS (reconnect) HKDF info string (OpenHarmony device_auth STS auth-info constant)
    private static final byte[] STS_AUTH_INFO = "hichain_auth_info".getBytes(StandardCharsets.UTF_8);

    // Exchange AAD strings (OpenHarmony device_auth key-exchange constants)
    public static final byte[] EXCHANGE_REQUEST_AAD = "hichain_exchange_request".getBytes(StandardCharsets.UTF_8);
    public static final byte[] EXCHANGE_RESPONSE_AAD = "hichain_exchange_response".getBytes(StandardCharsets.UTF_8);

    // Oakley Group 14 (2048-bit MODP) prime
    private static final String MOD_POW_256 =
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
          + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
          + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
          + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
          + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
          + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
          + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
          + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
          + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
          + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
          + "15728E5A8AACAA68FFFFFFFFFFFFFFFF";

    // Oakley Group 15 (3072-bit MODP) prime
    private static final String MOD_POW_384 =
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
          + "29024E088A67CC74020BBEA63B139B22514A08798E3404DD"
          + "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
          + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
          + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D"
          + "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
          + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
          + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B"
          + "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
          + "DE2BCBF6955817183995497CEA956AE515D2261898FA0510"
          + "15728E5A8AAAC42DAD33170D04507A33A85521ABDF1CBA64"
          + "ECFB850458DBEF0A8AEA71575D060C7DB3970F85A6E1E4C7"
          + "ABF5AE8CDB0933D71E8C94E04A25619DCEE3D2261AD2EE6B"
          + "F12FFA06D98A0864D87602733EC86A64521F2B18177B200C"
          + "BBE117577A615D6C770988C0BAD946E208E24FA074E5AB31"
          + "43DB5BFCE0FD108E4B82D120A93AD2CAFFFFFFFFFFFFFFFF";

    // Curve25519 prime for EC-SPEKE
    private static final BigInteger CURVE25519_P = new BigInteger(
            "7FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFED", 16);
    private static final BigInteger CURVE25519_A24 = BigInteger.valueOf(121665); // (486662-2)/4

    private HiChainPakeUtils() {
    }

    // ========================================================================
    // Algorithm selection
    // ========================================================================

    /**
     * Determines PAKE algorithm parameters.
     *
     * HiChain PAKE algorithm selection (OpenHarmony device_auth):
     * - version supports feature 2 -> EC-SPEKE (X25519, modLen=0)
     * - peer public param < 384 bytes -> DL-SPEKE 2048-bit (modLen=256, privLen=28)
     * - otherwise -> DL-SPEKE 3072-bit (modLen=384, privLen=32)
     */
    public static PakeParams determineAlgorithm(boolean versionSupportsFeature2, byte[] peerPublicParam) {
        PakeParams params = new PakeParams();
        if (versionSupportsFeature2) {
            params.modLen = 0;
            params.privateParamLen = 32;
        } else if (peerPublicParam != null && peerPublicParam.length < 384) {
            params.modLen = 256;
            params.privateParamLen = 28;
        } else {
            params.modLen = 384;
            params.privateParamLen = 32;
        }
        return params;
    }

    public static BigInteger getMod(int modLen) {
        if (modLen == 0) {
            return BigInteger.ZERO;
        }
        if (modLen * 2 < 768) {
            return new BigInteger(MOD_POW_256, 16);
        }
        return new BigInteger(MOD_POW_384, 16);
    }

    // ========================================================================
    // Core PAKE cryptographic operations
    // ========================================================================

    /**
     * Computes shared base g from password-derived key.
     * DL-SPEKE: g = passwordKey^2 mod p
     * EC-SPEKE: g = hashToCurve(passwordKey) via keyManager.hash2Point()
     */
    public static byte[] computeSharedBase(byte[] passwordKey, BigInteger mod) {
        if (mod.equals(BigInteger.ZERO)) {
            return hashToCurve25519(passwordKey);
        }
        BigInteger pw = new BigInteger(1, passwordKey);
        return fixedLength(pw.modPow(BigInteger.TWO, mod).toByteArray(),
                mod.bitLength() / 8 + (mod.bitLength() % 8 == 0 ? 0 : 1));
    }

    /**
     * Computes ephemeral public parameter A = g^a mod p (DL) or X25519(a, g) (EC).
     */
    public static byte[] computePublicParameter(byte[] base, byte[] privKey, BigInteger mod, int modLen) {
        if (mod.equals(BigInteger.ZERO)) {
            return x25519(privKey, base);
        }
        BigInteger b = new BigInteger(1, base);
        BigInteger a = new BigInteger(1, privKey);
        return fixedLength(b.modPow(a, mod).toByteArray(), modLen);
    }

    /**
     * Computes shared secret K = B^a mod p (DL) or X25519(a, B) (EC).
     */
    public static byte[] computeSharedSecret(byte[] peerPublicParam, byte[] selfPrivateParam,
                                              BigInteger mod, int modLen) {
        if (mod.equals(BigInteger.ZERO)) {
            return x25519(selfPrivateParam, peerPublicParam);
        }
        BigInteger B = new BigInteger(1, peerPublicParam);
        BigInteger a = new BigInteger(1, selfPrivateParam);
        return fixedLength(B.modPow(a, mod).toByteArray(), modLen);
    }

    // ========================================================================
    // Session key and proof derivation
    // ========================================================================

    /**
     * Derives session key and KCF key from shared secret.
     *
     * HiChain PAKE session-key derivation (OpenHarmony device_auth):
     *   tmpKey = HKDF-SHA256(sharedSecret, salt, "hichain_speke_sessionkey_info", 48)
     *   sessionKey = tmpKey[0:16]
     *   kcfKey = tmpKey[16:32] (DL-SPEKE) or tmpKey[16:48] (EC-SPEKE)
     */
    public static SessionKeyResult deriveSessionKeys(byte[] sharedSecret, byte[] salt, boolean isEcPake)
            throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] tmpKey = CryptoUtils.hkdfSha256(sharedSecret, salt, SESSION_KEY_INFO, 48);

        SessionKeyResult result = new SessionKeyResult();
        result.sessionKey = Arrays.copyOfRange(tmpKey, 0, 16);
        if (isEcPake) {
            result.kcfKey = Arrays.copyOfRange(tmpKey, 16, 48);
        } else {
            result.kcfKey = Arrays.copyOfRange(tmpKey, 16, 32);
        }
        return result;
    }

    /**
     * Generates proof: HMAC-SHA256(kcfKey, selfChallenge || peerChallenge).
     * HiChain PAKE proof generation (OpenHarmony device_auth).
     */
    public static byte[] generateProof(byte[] kcfKey, byte[] selfChallenge, byte[] peerChallenge)
            throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] concatenated = new byte[selfChallenge.length + peerChallenge.length];
        System.arraycopy(selfChallenge, 0, concatenated, 0, selfChallenge.length);
        System.arraycopy(peerChallenge, 0, concatenated, selfChallenge.length, peerChallenge.length);
        return hmacSha256(kcfKey, concatenated);
    }

    /**
     * Verifies proof: HMAC(kcfKey, peerChallenge || selfChallenge).
     * HiChain PAKE proof verification (OpenHarmony device_auth).
     */
    public static boolean verifyProof(byte[] kcfKey, byte[] firstChallenge, byte[] secondChallenge,
                                       byte[] receivedProof)
            throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] expected = generateProof(kcfKey, firstChallenge, secondChallenge);
        return constantTimeEquals(expected, receivedProof);
    }

    /**
     * Derives return key: HKDF-SHA256(sessionKey, salt, "hichain_return_key", len).
     * HiChain PAKE output-key derivation (OpenHarmony device_auth).
     */
    public static byte[] deriveReturnKey(byte[] sessionKey, byte[] salt, int desiredLen)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return CryptoUtils.hkdfSha256(sessionKey, salt, RETURN_KEY_INFO, desiredLen);
    }

    // ========================================================================
    // PSK / Temporary Auth Key derivation
    // ========================================================================

    /**
     * Derives the temporary auth key (PSK) from X25519 shared secret + serverNonce.
     * HiChain reconnect temporary auth key (OpenHarmony device_auth key manager):
     *   PSK = HKDF-SHA256(sharedSecret, serverNonce, "hichain_tmp_auth_enc_key", 32)
     *
     * This PSK is used as the password for the PAKE/SPEKE authentication.
     */
    public static byte[] derivePskFromX25519SharedSecret(byte[] sharedSecret, byte[] serverNonce)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return CryptoUtils.hkdfSha256(sharedSecret, serverNonce, TMP_AUTH_KEY_INFO, 32);
    }

    // ========================================================================
    // STS / reconnect PSK-SPEKE key derivation
    //
    // The Honor reconnect (operationCode 2) is PSK-SPEKE: the same EC-SPEKE PAKE as the bind,
    // run with a PIN derived from a pre-shared key. The PSK is the STATIC X25519 ECDH between the
    // two Ed25519 identity keys exchanged at bind (KeyManagerImpl.computePsk -> keyAgreement on
    // the Ed25519 asset), and the PIN is HKDF(PSK, nonce, "hichain_tmp_auth_enc_key", 32)
    // hex-encoded (KeyManagerImpl.getTmpAuthKey). The nonce is the server-supplied 32-byte value
    // in the AUTH_START_RESPONSE (msg 32785). Follows the OpenHarmony device_auth key manager.
    // ========================================================================

    private static final char[] LOWER_HEX = "0123456789abcdef".toCharArray();

    /**
     * X25519 private scalar corresponding to an Ed25519 identity {@code seed}:
     * clamp(SHA-512(seed)[0:32]), little-endian (libsodium ed25519_sk_to_curve25519).
     */
    public static byte[] ed25519PrivateToX25519(byte[] seed) {
        byte[] h = sha512(seed);
        byte[] a = Arrays.copyOfRange(h, 0, 32);
        a[0] &= (byte) 0xF8;
        a[31] &= (byte) 0x7F;
        a[31] |= (byte) 0x40;
        return a;
    }

    /**
     * X25519 (Montgomery u) public value from an Ed25519 public key via the birational map
     * {@code u = (1 + y) / (1 - y) mod p} (RFC 7748 §4.1). Input/output little-endian.
     */
    public static byte[] ed25519PublicToX25519(byte[] ed25519Pub) {
        byte[] c = Arrays.copyOf(ed25519Pub, 32);
        c[31] &= (byte) 0x7F; // drop the Edwards sign bit
        BigInteger y = new BigInteger(1, reverse(c));
        BigInteger onePlusY = BigInteger.ONE.add(y).mod(CURVE25519_P);
        BigInteger oneMinusY = BigInteger.ONE.subtract(y).mod(CURVE25519_P);
        BigInteger u = onePlusY.multiply(oneMinusY.modInverse(CURVE25519_P)).mod(CURVE25519_P);
        return reverse(fixedLength(u.toByteArray(), 32));
    }

    /**
     * Static reconnect PSK: X25519(ed25519PrivateToX25519(ownSeed), ed25519PublicToX25519(peerPub)).
     * Both peers derive the same value (own identity priv * peer identity pub).
     */
    public static byte[] computeStsPsk(byte[] ownSeed, byte[] peerEd25519Pub) {
        return x25519(ed25519PrivateToX25519(ownSeed), ed25519PublicToX25519(peerEd25519Pub));
    }

    /**
     * Reconnect PIN string = lowercase hex of HKDF(PSK, nonce, "hichain_tmp_auth_enc_key", 32),
     * fed as the SPEKE password (KeyManagerImpl.getTmpAuthKey + AuthRequest.changeClientCurrentTask).
     * Note the LOWERCASE hex (CommonUtils.toHexString), unlike the uppercase first-pair PIN.
     */
    public static String deriveStsPin(byte[] psk, byte[] nonce)
            throws InvalidKeyException, NoSuchAlgorithmException {
        return toLowerHex(derivePskFromX25519SharedSecret(psk, nonce));
    }

    static String toLowerHex(byte[] b) {
        char[] out = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            out[i * 2] = LOWER_HEX[v >>> 4];
            out[i * 2 + 1] = LOWER_HEX[v & 0x0F];
        }
        return new String(out);
    }

    /**
     * Derives password key: HKDF-SHA256(pinBytes, salt, "hichain_speke_base_info", 32).
     * HiChain PAKE parameter generation (OpenHarmony device_auth).
     */

    /**
     * Derives password key: HKDF-SHA256(pinBytes, salt, "hichain_speke_base_info", 32).
     * HiChain PAKE parameter generation (OpenHarmony device_auth).
     */
    public static byte[] derivePasswordKey(String pinHex, byte[] salt)
            throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] pinBytes = pinHex.getBytes(StandardCharsets.ISO_8859_1);
        return CryptoUtils.hkdfSha256(pinBytes, salt, BASE_INFO, 32);
    }

    // ========================================================================
    // Exchange encryption (AES-GCM)
    // ========================================================================

    /**
     * Encrypts data with AES-128-GCM.
     * From BlockCipherUtils.encryptAesGcm(): output = 12-byte IV || ciphertext+tag.
     */
    public static byte[] encryptAesGcm(byte[] plaintext, byte[] key, byte[] aad) throws Exception {
        byte[] iv = randomBytes(12);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
    }

    /**
     * Decrypts AES-128-GCM data.
     * From BlockCipherUtils.decryptAesGcm(): input = 12-byte IV || ciphertext+tag.
     */
    public static byte[] decryptAesGcm(byte[] encrypted, byte[] key, byte[] aad) throws Exception {
        if (encrypted == null || encrypted.length <= 12) {
            return new byte[0];
        }
        byte[] iv = Arrays.copyOfRange(encrypted, 0, 12);
        byte[] ciphertext = Arrays.copyOfRange(encrypted, 12, encrypted.length);
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        if (aad != null) {
            cipher.updateAAD(aad);
        }
        return cipher.doFinal(ciphertext);
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    public static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new SecureRandom().nextBytes(result);
        return result;
    }

    static byte[] fixedLength(byte[] input, int length) {
        if (length <= 0) {
            return input;
        }
        byte[] result = new byte[length];
        if (input.length > length) {
            System.arraycopy(input, input.length - length, result, 0, length);
        } else {
            System.arraycopy(input, 0, result, length - input.length, input.length);
        }
        return result;
    }

    static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    static byte[] hmacSha256(byte[] key, byte[] data)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    // ========================================================================
    // X25519 implementation
    // ========================================================================

    /**
     * X25519 scalar multiplication (RFC 7748), little-endian in and out.
     *
     * {@code scalar} and {@code uCoord} are little-endian byte arrays and the returned
     * u-coordinate is little-endian, matching the on-wire representation and
     * BouncyCastle's {@code X25519Agreement} (which the Honor app uses). This is what makes
     * {@link #derivePin(byte[])} take the correct 6 bytes and the EC-SPEKE epks interoperate.
     *
     * Validate against RFC 7748 §5.2 test vectors before trusting PAKE output, e.g.
     *   scalar = a546e36bf0527c9d3b16154b82465edd62144c0ac1fc5a18506a2244ba449ac4
     *   u      = e6db6867583030db3594c1a424b15f7c726624ec26b3353b10a903a6d0ab1c4c
     *   result = c3da55379de9c6908e94ea4df28d084f32eccf03491c71f754b4075577a28552
     * (all little-endian, as written in the RFC).
     */
    public static byte[] x25519(byte[] scalar, byte[] uCoord) {
        byte[] k = Arrays.copyOf(scalar, 32);
        k[0] &= (byte) 0xF8;
        k[31] &= (byte) 0x7F;
        k[31] |= (byte) 0x40;

        // decodeUCoordinate: little-endian, clear the MSB of the most-significant byte
        byte[] uLe = Arrays.copyOf(uCoord, 32);
        uLe[31] &= (byte) 0x7F;
        BigInteger u = new BigInteger(1, reverse(uLe));
        BigInteger x1 = u;
        BigInteger x2 = BigInteger.ONE;
        BigInteger z2 = BigInteger.ZERO;
        BigInteger x3 = u;
        BigInteger z3 = BigInteger.ONE;
        int swap = 0;

        for (int t = 254; t >= 0; t--) {
            int kt = (k[t >> 3] >> (t & 7)) & 1; // little-endian scalar
            swap ^= kt;

            // Conditional swap
            if (swap != 0) {
                BigInteger tmp;
                tmp = x2; x2 = x3; x3 = tmp;
                tmp = z2; z2 = z3; z3 = tmp;
            }
            swap = kt;

            BigInteger A = x2.add(z2).mod(CURVE25519_P);
            BigInteger AA = A.multiply(A).mod(CURVE25519_P);
            BigInteger B = x2.subtract(z2).mod(CURVE25519_P);
            BigInteger BB = B.multiply(B).mod(CURVE25519_P);
            BigInteger E = AA.subtract(BB).mod(CURVE25519_P);
            BigInteger C = x3.add(z3).mod(CURVE25519_P);
            BigInteger D = x3.subtract(z3).mod(CURVE25519_P);
            BigInteger DA = D.multiply(A).mod(CURVE25519_P);
            BigInteger CB = C.multiply(B).mod(CURVE25519_P);
            BigInteger a1 = DA.add(CB).mod(CURVE25519_P);
            BigInteger a2 = DA.subtract(CB).mod(CURVE25519_P);
            x3 = a1.multiply(a1).mod(CURVE25519_P);
            z3 = x1.multiply(a2.multiply(a2).mod(CURVE25519_P)).mod(CURVE25519_P);
            x2 = AA.multiply(BB).mod(CURVE25519_P);
            z2 = E.multiply(AA.add(CURVE25519_A24.multiply(E).mod(CURVE25519_P)).mod(CURVE25519_P)).mod(CURVE25519_P);
        }

        // Final conditional swap
        if (swap != 0) {
            BigInteger tmp;
            tmp = x2; x2 = x3; x3 = tmp;
            tmp = z2; z2 = z3; z3 = tmp;
        }

        BigInteger z2inv = z2.modPow(CURVE25519_P.subtract(BigInteger.TWO), CURVE25519_P);
        BigInteger result = x2.multiply(z2inv).mod(CURVE25519_P);

        // encodeUCoordinate: little-endian 32 bytes
        return reverse(fixedLength(result.toByteArray(), 32));
    }

    static byte[] reverse(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[in.length - 1 - i];
        }
        return out;
    }

    /**
     * First-pair PAKE PIN: UPPERCASE hex of the first 6 bytes of the (little-endian) X25519
     * shared secret, as observed in the pairing capture (the watch upper-cases the hex).
     * The result is used as a *string* whose bytes are HKDF'd (see {@link #derivePasswordKey}),
     * so the case is significant.
     */
    public static String derivePin(byte[] x25519SharedSecret) {
        byte[] six = Arrays.copyOf(x25519SharedSecret, 6);
        StringBuilder sb = new StringBuilder(12);
        for (byte b : six) {
            String h = Integer.toHexString(b & 0xFF);
            if (h.length() < 2) {
                sb.append('0');
            }
            sb.append(h);
        }
        return sb.toString().toUpperCase(java.util.Locale.ROOT);
    }

    private static final BigInteger CURVE25519_A = BigInteger.valueOf(486662);

    /**
     * Elligator2 map of a 32-byte secret to a Curve25519 u-coordinate (little-endian),
     * the EC-SPEKE base point {@code g}. Faithful port of OpenHarmony device_auth
     * {@code OpensslHashToPoint} / {@code CurveHashToPoint}
     * (deps_adapter/key_management_adapter/impl/src/standard/crypto_hash_to_point.c),
     * validated byte-for-byte against that C code (see PAKE_PROTOCOL.md C8).
     *
     * With p = 2^255-19, A = 486662, u = 2:
     *   r = little-endian(secret) with the top 2 bits of the MSB cleared (&0x3f)
     *   b = -A / (1 + u*r^2) mod p
     *   a = b^3 + A*b^2 + b mod p                  (Montgomery RHS at b)
     *   x = b            if a is a quadratic residue (or a == 0)
     *     = -b - A mod p otherwise
     * The output replicates the reference exactly, including its non-left-padded byte
     * placement for x &lt; 2^248 (BN_bn2bin into a zeroed buffer, then endian-swap).
     */
    static byte[] hashToCurve25519(byte[] secret) {
        byte[] h = Arrays.copyOf(secret, 32);
        h[31] &= 0x3f;
        BigInteger r = new BigInteger(1, reverse(h));

        BigInteger p = CURVE25519_P;
        BigInteger A = CURVE25519_A;
        BigInteger u = BigInteger.TWO;

        BigInteger denom = BigInteger.ONE.add(u.multiply(r.multiply(r).mod(p))).mod(p);
        BigInteger b = A.negate().mod(p).multiply(denom.modInverse(p)).mod(p);

        BigInteger b2 = b.multiply(b).mod(p);
        BigInteger b3 = b2.multiply(b).mod(p);
        BigInteger a = b3.add(A.multiply(b2)).add(b).mod(p);

        // Legendre symbol a^((p-1)/2): 1 = QR, p-1 = non-residue, 0 = a==0
        BigInteger legendre = a.modPow(p.subtract(BigInteger.ONE).shiftRight(1), p);
        BigInteger x = (a.signum() == 0 || legendre.equals(BigInteger.ONE))
                ? b
                : b.negate().subtract(A).mod(p);

        byte[] be = toUnsignedBytes(x);
        byte[] buf = new byte[32];
        System.arraycopy(be, 0, buf, 0, Math.min(be.length, 32));
        return reverse(buf);
    }

    /** Minimal big-endian bytes of a non-negative value (OpenSSL BN_bn2bin semantics). */
    static byte[] toUnsignedBytes(BigInteger v) {
        byte[] a = v.toByteArray();
        if (a.length > 1 && a[0] == 0) {
            return Arrays.copyOfRange(a, 1, a.length);
        }
        return a;
    }

    // ========================================================================
    // Ed25519 (RFC 8032) - identity signing for the PAKE exchange step
    //
    // Pure-Java implementation (no java.security "Ed25519", which is API 33+, and not in the
    // shaded BouncyCastle). Validated byte-for-byte against pyca/cryptography, see
    // TestHiChainPakeUtils.testEd25519. The Honor secure element pre-hashes the message with
    // SHA-256 before signing, so callers sign sha256(challenge || info) - matching the
    // authenticated key-exchange step of OpenHarmony device_auth.
    // ========================================================================

    private static final BigInteger ED_L = new BigInteger(
            "1000000000000000000000000000000014def9dea2f79cd65812631a5cf5d3ed", 16);
    private static final BigInteger ED_D = new BigInteger(
            "37095705934669439343138083508754565189542113879843219016388785533085940283555");
    private static final BigInteger[] ED_B = {
            new BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202"),
            new BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960"),
            BigInteger.ONE,
            new BigInteger("15112221349535400772501151409588531511454012693041857206046113283949847762202")
                    .multiply(new BigInteger("46316835694926478169428394003475163141307993866256225615783033603165251855960"))
                    .mod(CURVE25519_P)
    };

    /** Ed25519 public key (32-byte compressed point) from a 32-byte seed. */
    public static byte[] ed25519PublicKey(byte[] seed) {
        return edCompress(edScalarMul(edSecretScalar(seed), ED_B));
    }

    /**
     * Ed25519 signature (64 bytes) of {@code msg} under the identity {@code seed} (RFC 8032).
     * {@code msg} is signed directly (PureEdDSA) - pass the SHA-256 digest yourself if the
     * peer expects a pre-hashed message.
     */
    public static byte[] ed25519Sign(byte[] seed, byte[] msg) {
        byte[] h = sha512(seed);
        byte[] aBytes = Arrays.copyOfRange(h, 0, 32);
        aBytes[0] &= (byte) 0xF8;
        aBytes[31] &= (byte) 0x7F;
        aBytes[31] |= (byte) 0x40;
        BigInteger a = new BigInteger(1, reverse(aBytes));
        byte[] prefix = Arrays.copyOfRange(h, 32, 64);

        byte[] aPub = edCompress(edScalarMul(a, ED_B));
        BigInteger r = new BigInteger(1, reverse(sha512(concat(prefix, msg)))).mod(ED_L);
        byte[] rEnc = edCompress(edScalarMul(r, ED_B));
        BigInteger k = new BigInteger(1, reverse(sha512(concat(rEnc, aPub, msg)))).mod(ED_L);
        BigInteger s = r.add(k.multiply(a)).mod(ED_L);
        return concat(rEnc, edEncodeLe(s));
    }

    /** Ed25519 signature verification (RFC 8032). {@code msg} is verified as-is (PureEdDSA). */
    public static boolean ed25519Verify(byte[] msg, byte[] sig, byte[] pubKey) {
        if (sig == null || sig.length != 64 || pubKey == null || pubKey.length != 32) {
            return false;
        }
        BigInteger[] a = edDecompress(pubKey);
        if (a == null) {
            return false;
        }
        byte[] rEnc = Arrays.copyOfRange(sig, 0, 32);
        BigInteger s = new BigInteger(1, reverse(Arrays.copyOfRange(sig, 32, 64)));
        if (s.compareTo(ED_L) >= 0) {
            return false;
        }
        BigInteger[] r = edDecompress(rEnc);
        if (r == null) {
            return false;
        }
        BigInteger k = new BigInteger(1, reverse(sha512(concat(rEnc, pubKey, msg)))).mod(ED_L);
        BigInteger[] sB = edScalarMul(s, ED_B);
        BigInteger[] rPlusKa = edAdd(r, edScalarMul(k, a));
        return edPointEqual(sB, rPlusKa);
    }

    private static final BigInteger ED_SQRTM1 =
            BigInteger.TWO.modPow(CURVE25519_P.subtract(BigInteger.ONE).shiftRight(2), CURVE25519_P);

    private static BigInteger[] edDecompress(byte[] comp) {
        byte[] c = comp.clone();
        int sign = (c[31] >> 7) & 1;
        c[31] &= (byte) 0x7F;
        BigInteger y = new BigInteger(1, reverse(c));
        if (y.compareTo(CURVE25519_P) >= 0) {
            return null;
        }
        BigInteger y2 = y.multiply(y).mod(CURVE25519_P);
        BigInteger num = y2.subtract(BigInteger.ONE).mod(CURVE25519_P);
        BigInteger den = ED_D.multiply(y2).add(BigInteger.ONE).mod(CURVE25519_P);
        BigInteger x2 = num.multiply(edInv(den)).mod(CURVE25519_P);
        if (x2.signum() == 0) {
            if (sign == 1) {
                return null;
            }
            return new BigInteger[]{BigInteger.ZERO, y, BigInteger.ONE, BigInteger.ZERO};
        }
        BigInteger x = x2.modPow(CURVE25519_P.add(BigInteger.valueOf(3)).shiftRight(3), CURVE25519_P);
        if (x.multiply(x).subtract(x2).mod(CURVE25519_P).signum() != 0) {
            x = x.multiply(ED_SQRTM1).mod(CURVE25519_P);
        }
        if (x.multiply(x).subtract(x2).mod(CURVE25519_P).signum() != 0) {
            return null;
        }
        if ((x.testBit(0) ? 1 : 0) != sign) {
            x = CURVE25519_P.subtract(x);
        }
        return new BigInteger[]{x, y, BigInteger.ONE, x.multiply(y).mod(CURVE25519_P)};
    }

    private static boolean edPointEqual(BigInteger[] p, BigInteger[] q) {
        if (p[0].multiply(q[2]).subtract(q[0].multiply(p[2])).mod(CURVE25519_P).signum() != 0) {
            return false;
        }
        return p[1].multiply(q[2]).subtract(q[1].multiply(p[2])).mod(CURVE25519_P).signum() == 0;
    }

    public static byte[] sha256(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] sha512(byte[] data) {
        try {
            return java.security.MessageDigest.getInstance("SHA-512").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static BigInteger edSecretScalar(byte[] seed) {
        byte[] h = sha512(seed);
        byte[] a = Arrays.copyOfRange(h, 0, 32);
        a[0] &= (byte) 0xF8;
        a[31] &= (byte) 0x7F;
        a[31] |= (byte) 0x40;
        return new BigInteger(1, reverse(a));
    }

    private static BigInteger edInv(BigInteger x) {
        return x.modPow(CURVE25519_P.subtract(BigInteger.TWO), CURVE25519_P);
    }

    // Extended (X, Y, Z, T) coordinates on the Edwards curve.
    private static BigInteger[] edAdd(BigInteger[] p, BigInteger[] q) {
        BigInteger a = p[1].subtract(p[0]).multiply(q[1].subtract(q[0])).mod(CURVE25519_P);
        BigInteger b = p[1].add(p[0]).multiply(q[1].add(q[0])).mod(CURVE25519_P);
        BigInteger c = BigInteger.TWO.multiply(p[3]).multiply(q[3]).multiply(ED_D).mod(CURVE25519_P);
        BigInteger d = BigInteger.TWO.multiply(p[2]).multiply(q[2]).mod(CURVE25519_P);
        BigInteger e = b.subtract(a), f = d.subtract(c), g = d.add(c), hh = b.add(a);
        return new BigInteger[]{
                e.multiply(f).mod(CURVE25519_P), g.multiply(hh).mod(CURVE25519_P),
                f.multiply(g).mod(CURVE25519_P), e.multiply(hh).mod(CURVE25519_P)};
    }

    private static BigInteger[] edScalarMul(BigInteger s, BigInteger[] point) {
        BigInteger[] q = {BigInteger.ZERO, BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO};
        BigInteger[] base = point;
        BigInteger scalar = s;
        while (scalar.signum() > 0) {
            if (scalar.testBit(0)) {
                q = edAdd(q, base);
            }
            base = edAdd(base, base);
            scalar = scalar.shiftRight(1);
        }
        return q;
    }

    private static byte[] edCompress(BigInteger[] point) {
        BigInteger zInv = edInv(point[2]);
        BigInteger x = point[0].multiply(zInv).mod(CURVE25519_P);
        BigInteger y = point[1].multiply(zInv).mod(CURVE25519_P);
        byte[] out = edEncodeLe(y);
        if (x.testBit(0)) {
            out[31] |= (byte) 0x80;
        }
        return out;
    }

    /** 32-byte little-endian encoding of a field/scalar value. */
    private static byte[] edEncodeLe(BigInteger v) {
        byte[] be = v.toByteArray();
        byte[] b = new byte[32];
        int copy = Math.min(be.length, 32);
        System.arraycopy(be, be.length - copy, b, 32 - copy, copy);
        return reverse(b);
    }

    public static byte[] concat(byte[]... arrays) {
        int n = 0;
        for (byte[] x : arrays) {
            n += x.length;
        }
        byte[] out = new byte[n];
        int pos = 0;
        for (byte[] x : arrays) {
            System.arraycopy(x, 0, out, pos, x.length);
            pos += x.length;
        }
        return out;
    }

    // ========================================================================
    // Data classes
    // ========================================================================

    public static class PakeParams {
        public int modLen = 384;
        public int privateParamLen = 32;

        public boolean isEcPake() {
            return modLen == 0;
        }

        public BigInteger getMod() {
            return HiChainPakeUtils.getMod(modLen);
        }
    }

    public static class SessionKeyResult {
        public byte[] sessionKey;
        public byte[] kcfKey;
    }

    public static class PakeState {
        public PakeParams params;
        public BigInteger mod;

        public byte[] salt;
        public byte[] selfChallenge;
        public byte[] selfPrivateParam;
        public byte[] selfPublicParam;

        public byte[] peerChallenge;
        public byte[] peerPublicParam;
        public byte[] peerAuthId;

        public byte[] passwordKey;
        public byte[] sharedBase;
        public byte[] sharedSecret;
        public byte[] sessionKey;
        public byte[] kcfKey;

        public String pinHex;
    }
}
