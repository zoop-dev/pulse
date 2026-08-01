package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi_scooters

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

/**
 * Verifies [XiaomiScooterCrypto]: the ECDH/HKDF/AES-CCM transport. These tests verify the machinery
 * is internally consistent: ECDH round-trips symmetrically, the raw public-key encoding round-trips,
 * and AES-CCM round-trips and rejects tampering.
 */
class XiaomiScooterCryptoTest {
    @Test
    fun testEcdh_isSymmetric() {
        val appKeyPair = XiaomiScooterCrypto.generateEphemeralKeyPair()
        val deviceKeyPair = XiaomiScooterCrypto.generateEphemeralKeyPair()

        val appPub = XiaomiScooterCrypto.publicKeyToRaw(appKeyPair.public)
        val devicePub = XiaomiScooterCrypto.publicKeyToRaw(deviceKeyPair.public)

        val sharedFromApp = XiaomiScooterCrypto.sharedSecret(appKeyPair.private, devicePub)
        val sharedFromDevice = XiaomiScooterCrypto.sharedSecret(deviceKeyPair.private, appPub)

        assertArrayEquals(sharedFromApp, sharedFromDevice)
        assertEquals(32, sharedFromApp.size)
    }

    @Test
    fun testPublicKeyRawEncoding_is64BytesAndRoundTrips() {
        val keyPair = XiaomiScooterCrypto.generateEphemeralKeyPair()
        val raw = XiaomiScooterCrypto.publicKeyToRaw(keyPair.public)
        assertEquals(64, raw.size)

        // Re-importing the raw key and using it for ECDH against a third party must give the
        // same result as using the original key object directly.
        val other = XiaomiScooterCrypto.generateEphemeralKeyPair()
        val direct = XiaomiScooterCrypto.sharedSecret(other.private, raw)
        val viaRoundTrip = XiaomiScooterCrypto.sharedSecret(
            other.private,
            XiaomiScooterCrypto.publicKeyToRaw(XiaomiScooterCrypto.rawToPublicKey(raw))
        )
        assertArrayEquals(direct, viaRoundTrip)
    }

    @Test
    fun testDeriveSessionKeys_isDeterministicAndProducesDistinctMaterial() {
        val sharedSecret = ByteArray(32) { it.toByte() }
        val authKey = ByteArray(32) { (it * 7).toByte() }

        val first = XiaomiScooterCrypto.deriveSessionKeys(sharedSecret, authKey)
        val second = XiaomiScooterCrypto.deriveSessionKeys(sharedSecret, authKey)

        assertArrayEquals(first.devKey, second.devKey)
        assertArrayEquals(first.appKey, second.appKey)
        assertArrayEquals(first.devIv, second.devIv)
        assertArrayEquals(first.appIv, second.appIv)

        assertEquals(16, first.devKey.size)
        assertEquals(16, first.appKey.size)
        assertEquals(4, first.devIv.size)
        assertEquals(4, first.appIv.size)
        assertNotEquals(first.devKey.toList(), first.appKey.toList())

        // Changing the auth key must change the derived session keys.
        val differentAuthKey = ByteArray(32) { (it * 13).toByte() }
        val third = XiaomiScooterCrypto.deriveSessionKeys(sharedSecret, differentAuthKey)
        assertNotEquals(first.appKey.toList(), third.appKey.toList())
    }

    @Test
    fun testLoginConfirmationToken_isDeterministicAndCrcDependent() {
        val appKey = ByteArray(16) { it.toByte() }
        val devicePubKey = ByteArray(64) { (it * 3).toByte() }

        val token = XiaomiScooterCrypto.buildLoginConfirmationToken(appKey, devicePubKey)
        assertEquals(8, token.size) // 4-byte ciphertext + 4-byte tag

        val tokenAgain = XiaomiScooterCrypto.buildLoginConfirmationToken(appKey, devicePubKey)
        assertArrayEquals(token, tokenAgain)

        val differentDevicePubKey = ByteArray(64) { (it * 5).toByte() }
        val tokenForDifferentDevice = XiaomiScooterCrypto.buildLoginConfirmationToken(appKey, differentDevicePubKey)
        assertNotEquals(token.toList(), tokenForDifferentDevice.toList())
    }

    @Test
    fun testCcm_roundTrips() {
        val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val plaintext = "Xiaomi Scooter 5 Max".toByteArray()

        val ciphertext = XiaomiScooterCrypto.encryptCcm(key, nonce, plaintext)
        assertEquals(plaintext.size + 4, ciphertext.size) // + 4-byte tag

        val decrypted = XiaomiScooterCrypto.decryptCcm(key, nonce, ciphertext)
        assertArrayEquals(plaintext, decrypted)
    }

    @Test
    fun testCcm_rejectsTamperedCiphertext() {
        val key = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val ciphertext = XiaomiScooterCrypto.encryptCcm(key, nonce, "motor_lock=1".toByteArray())
        ciphertext[0] = (ciphertext[0].toInt() xor 0x01).toByte()

        assertThrows(Exception::class.java) {
            XiaomiScooterCrypto.decryptCcm(key, nonce, ciphertext)
        }
    }

    @Test
    fun testSession_encryptDecryptRoundTripsAcrossMultipleMessages() {
        val keys = XiaomiScooterCrypto.SessionKeys(
            devKey = ByteArray(16) { it.toByte() },
            appKey = ByteArray(16) { (it + 1).toByte() },
            devIv = ByteArray(4) { it.toByte() },
            appIv = ByteArray(4) { (it + 1).toByte() },
        )
        // The app encrypts with appKey/appIv and decrypts with devKey/devIv, so a session standing
        // in for the device has to mirror the two directions.
        val mirroredKeys = XiaomiScooterCrypto.SessionKeys(
            devKey = keys.appKey,
            appKey = keys.devKey,
            devIv = keys.appIv,
            appIv = keys.devIv,
        )
        val appSession = XiaomiScooterCrypto.Session(keys)
        val deviceSession = XiaomiScooterCrypto.Session(mirroredKeys)

        for (i in 0 until 5) {
            val toDevice = "app message $i".toByteArray()
            val appFrame = appSession.encryptForDevice(toDevice)
            assertEquals(i, appFrame.wireCounter)
            assertArrayEquals(toDevice, deviceSession.decryptFromDevice(appFrame.wireCounter, appFrame.ciphertext))

            val toApp = "device message $i".toByteArray()
            val deviceFrame = deviceSession.encryptForDevice(toApp)
            assertEquals(i, deviceFrame.wireCounter)
            assertArrayEquals(toApp, appSession.decryptFromDevice(deviceFrame.wireCounter, deviceFrame.ciphertext))
        }
    }

    @Test
    fun testBuildNonce_layout() {
        val nonce = XiaomiScooterCrypto.buildNonce(byteArrayOf(0x01, 0x02, 0x03, 0x04), counter = 5)
        assertEquals(12, nonce.size)
        assertArrayEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0, 0, 0, 0, 5, 0, 0, 0), nonce)
        assertTrue(nonce.size == 12)
    }
}
