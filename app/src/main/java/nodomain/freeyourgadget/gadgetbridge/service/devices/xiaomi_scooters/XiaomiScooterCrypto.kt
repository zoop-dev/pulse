package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils
import org.bouncycastle.shaded.crypto.engines.AESEngine
import org.bouncycastle.shaded.crypto.modes.CCMBlockCipher
import org.bouncycastle.shaded.crypto.modes.CCMModeCipher
import org.bouncycastle.shaded.crypto.params.AEADParameters
import org.bouncycastle.shaded.crypto.params.KeyParameter
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import java.util.zip.CRC32
import javax.crypto.KeyAgreement

/**
 * Implements the cryptographic transport used by the Xiaomi Scooter 5 Max: ephemeral ECDH
 * on secp256r1, HKDF-SHA256 session-key derivation seeded with the device's LTMK (long-term
 * auth key?), and AES-128-CCM for the application channel.
 */
object XiaomiScooterCrypto {
    private const val HKDF_SALT = "smartcfg-login-salt"
    private const val HKDF_INFO = "smartcfg-login-info"

    /** Static nonce used to authenticate the login token. */
    private val LOGIN_CONFIRMATION_NONCE =
        byteArrayOf(0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b)

    /** The four session keys/IVs derived at login, one pair per direction. */
    @Suppress("ArrayInDataClass")
    data class SessionKeys(
        val devKey: ByteArray,
        val appKey: ByteArray,
        val devIv: ByteArray,
        val appIv: ByteArray,
    )

    /** A ciphertext ready to be sent, with the (16-bit, wire-truncated) counter used to build it. */
    @Suppress("ArrayInDataClass")
    data class EncryptedFrame(
        val wireCounter: Int,
        val ciphertext: ByteArray,
    )

    /** Holds the derived session keys and the per-direction nonce counters for one connection. */
    class Session(private val keys: SessionKeys) {
        private var encryptCounter = 0

        // The wire only carries the low 16 bits of the (32-bit) nonce counter; track wraparound
        // ourselves so long-lived sessions (>65535 messages in one direction) still work (not confirmed).
        // BLE notifications can arrive slightly out of order (e.g. an ack overtaking an already-queued
        // telemetry frame), so a plain "counter decreased" check misfires on that ordinary jitter and
        // permanently desyncs the nonce. Only treat a decrease as a genuine rollover when it's a jump
        // of more than half the 16-bit range, which ordinary reordering never produces.
        private var maxDecryptWireCounter = -1
        private var decryptCounterHigh = 0

        fun encryptForDevice(plaintext: ByteArray): EncryptedFrame {
            val counter = encryptCounter
            encryptCounter++
            val nonce = buildNonce(keys.appIv, counter)
            val ciphertext = encryptCcm(keys.appKey, nonce, plaintext)
            return EncryptedFrame(wireCounter = counter and 0xffff, ciphertext = ciphertext)
        }

        fun decryptFromDevice(wireCounter: Int, ciphertext: ByteArray): ByteArray {
            if (maxDecryptWireCounter >= 0 && wireCounter < maxDecryptWireCounter) {
                if (maxDecryptWireCounter - wireCounter > 0x8000) {
                    decryptCounterHigh++
                    maxDecryptWireCounter = wireCounter
                }
            } else {
                maxDecryptWireCounter = wireCounter
            }
            val counter = (decryptCounterHigh shl 16) or (wireCounter and 0xffff)
            val nonce = buildNonce(keys.devIv, counter)
            return decryptCcm(keys.devKey, nonce, ciphertext)
        }
    }

    // =======================================================================
    // ECDH (secp256r1)
    // =======================================================================

    fun generateEphemeralKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        @Suppress("SpellCheckingInspection")
        keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
        return keyPairGenerator.generateKeyPair()
    }

    /** Raw 64-byte X‖Y public key encoding (SEC1 uncompressed point without the leading 0x04). */
    fun publicKeyToRaw(publicKey: PublicKey): ByteArray {
        val ecPublicKey = publicKey as ECPublicKey
        val x = toFixedLength(ecPublicKey.w.affineX, 32)
        val y = toFixedLength(ecPublicKey.w.affineY, 32)
        return x + y
    }

    fun rawToPublicKey(raw: ByteArray): PublicKey {
        require(raw.size == 64) { "Expected a 64-byte raw public key, got ${raw.size}" }
        val x = BigInteger(1, raw.copyOfRange(0, 32))
        val y = BigInteger(1, raw.copyOfRange(32, 64))
        val keyFactory = KeyFactory.getInstance("EC")
        return keyFactory.generatePublic(ECPublicKeySpec(ECPoint(x, y), ecParameterSpec()))
    }

    /** 32-byte X-coordinate of the ECDH shared point. */
    fun sharedSecret(privateKey: PrivateKey, remotePublicKeyRaw: ByteArray): ByteArray {
        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(rawToPublicKey(remotePublicKeyRaw), true)
        return keyAgreement.generateSecret()
    }

    private fun ecParameterSpec(): ECParameterSpec {
        val algorithmParameters = AlgorithmParameters.getInstance("EC")
        @Suppress("SpellCheckingInspection")
        algorithmParameters.init(ECGenParameterSpec("secp256r1"))
        return algorithmParameters.getParameterSpec(ECParameterSpec::class.java)
    }

    private fun toFixedLength(value: BigInteger, @Suppress("SameParameterValue") length: Int): ByteArray {
        val raw = value.toByteArray()
        val result = ByteArray(length)
        val srcOffset = maxOf(0, raw.size - length)
        val copyLen = minOf(raw.size, length)
        System.arraycopy(raw, srcOffset, result, length - copyLen, copyLen)
        return result
    }

    // =======================================================================
    // HKDF session-key derivation
    // =======================================================================

    /**
     * `PRK = HMAC-SHA256(salt, ecdhSharedSecret ‖ authKey)`, then two HKDF-expand rounds using
     * `info = "smartcfg-login-info"`: `T1` yields `devKey ‖ appKey`, `T2`'s first 8 bytes yield
     * `devIv ‖ appIv` (its remaining 24 bytes are unused).
     */
    fun deriveSessionKeys(ecdhSharedSecret: ByteArray, authKey: ByteArray): SessionKeys {
        require(ecdhSharedSecret.size == 32) { "ECDH shared secret must be 32 bytes" }
        require(authKey.size == 32) { "Auth key (LTMK) must be 32 bytes" }
        val ikm = ecdhSharedSecret + authKey
        val okm = CryptoUtils.hkdfSha256(
            ikm,
            HKDF_SALT.toByteArray(Charsets.US_ASCII),
            HKDF_INFO.toByteArray(Charsets.US_ASCII),
            64,
        )
        return SessionKeys(
            devKey = okm.copyOfRange(0, 16),
            appKey = okm.copyOfRange(16, 32),
            devIv = okm.copyOfRange(32, 36),
            appIv = okm.copyOfRange(36, 40),
        )
    }

    // =======================================================================
    // Login confirmation token
    // =======================================================================

    /**
     * `AES-128-CCM(appKey, staticNonce, plaintext=CRC32_LE(devicePublicKeyRaw), tagLen=4)`, an
     * 8-byte token proving the app derived the correct session key and saw the correct device
     * public key.
     */
    fun buildLoginConfirmationToken(appKey: ByteArray, devicePublicKeyRaw: ByteArray): ByteArray {
        val crc = CRC32()
        crc.update(devicePublicKeyRaw)
        val plaintext = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(crc.value.toInt()).array()
        return encryptCcm(appKey, LOGIN_CONFIRMATION_NONCE, plaintext, tagLengthBits = 32)
    }

    // =======================================================================
    // AES-128-CCM
    // =======================================================================

    fun buildNonce(ivPrefix: ByteArray, counter: Int): ByteArray {
        require(ivPrefix.size == 4) { "IV prefix must be 4 bytes" }
        return ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
            .put(ivPrefix)
            .putInt(0)
            .putInt(counter)
            .array()
    }

    fun encryptCcm(key: ByteArray, nonce: ByteArray, plaintext: ByteArray, tagLengthBits: Int = 32): ByteArray {
        val cipher = createCipher(forEncrypt = true, key = key, nonce = nonce, macSizeBits = tagLengthBits)
        val out = ByteArray(cipher.getOutputSize(plaintext.size))
        val len = cipher.processBytes(plaintext, 0, plaintext.size, out, 0)
        cipher.doFinal(out, len)
        return out
    }

    fun decryptCcm(key: ByteArray, nonce: ByteArray, ciphertext: ByteArray, tagLengthBits: Int = 32): ByteArray {
        val cipher = createCipher(forEncrypt = false, key = key, nonce = nonce, macSizeBits = tagLengthBits)
        val out = ByteArray(cipher.getOutputSize(ciphertext.size))
        val len = cipher.processBytes(ciphertext, 0, ciphertext.size, out, 0)
        val finalLen = cipher.doFinal(out, len)
        return out.copyOf(len + finalLen)
    }

    private fun createCipher(forEncrypt: Boolean, key: ByteArray, nonce: ByteArray, macSizeBits: Int): CCMModeCipher {
        val keyParameter = KeyParameter(key)
        val engine = AESEngine.newInstance()
        engine.init(forEncrypt, keyParameter)
        val blockCipher = CCMBlockCipher.newInstance(engine)
        blockCipher.init(forEncrypt, AEADParameters(keyParameter, macSizeBits, nonce, null))
        return blockCipher
    }
}
