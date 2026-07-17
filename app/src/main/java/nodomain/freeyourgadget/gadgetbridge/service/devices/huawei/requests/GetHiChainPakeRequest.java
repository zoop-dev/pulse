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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiTLV;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig.HiChain;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HiChainPakeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

/**
 * HiChain authentication for the Honor Watch 5 (service 0x01 / command 0x28), covering both:
 *   - first-pair BIND (operationCode 1) via the PSK-SPEKE PAKE protocol, and
 *   - reconnect AUTHENTICATE (operationCode 2) via the STS protocol, which re-authenticates
 *     against the Ed25519 identity keys exchanged at bind (see PAKE_PROTOCOL.md C10).
 * The HiChain PAKE/STS mechanics follow the open-source OpenHarmony HiChain implementation
 * (base/security/device_auth). The Honor-specific MBB wire framing of the 0x28 packet was
 * derived from a pairing capture. See PAKE_PROTOCOL.md (sections C1–C8) for the full derivation.
 *
 * Wire framing (packHiChainData): TLV tag 0x01 = dataType byte, tag 0x02 = payload.
 *   dataType 1 = X25519 ephemeral pubkey exchange, 2 = PAKE JSON passthrough.
 *
 * Flow (step -> message):
 *   0  X25519 exchange:   out {1:01, 2:phonePub};  in {1:01, 2:watchPub}
 *      => PIN = UPPERCASE_hex(X25519(phonePriv, watchPub)[0:6])
 *   1  PAKE start (msg 1): {support256mod, serviceType:"WEAR_MBB", operationCode, version}
 *      in msg 32769: {challenge, salt, epk}
 *   2  client confirm (msg 2): {challenge, epk, kcfData}
 *      in msg 32770: {kcfData}
 *   3  exchange (msg 3): {exAuthInfo}  -- AES-GCM(sessionKey, info || Ed25519 sig)
 *      in msg 32771: {exAuthInfo}  -- peer identity, verified
 *
 * Cryptographically the password fed to SPEKE is the ASCII of the UPPERCASE PIN string,
 * and the SPEKE base is hash2Point(HKDF(pin, serverSalt, "hichain_speke_base_info")). The
 * hash2Point (Elligator2 on Curve25519) is implemented in HiChainPakeUtils - see C8.
 */
public class GetHiChainPakeRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetHiChainPakeRequest.class);

    // HiChain message codes (OpenHarmony device_auth)
    private static final int MSG_PAKE_START = 1;
    private static final int MSG_PAKE_CLIENT_CONFIRM = 2;
    private static final int MSG_EXCHANGE_REQUEST = 3;
    private static final int MSG_STS_AUTH_START = 17; // reconnect AUTH_START_REQUEST
    private static final int MSG_STS_AUTH_ACK = 18;   // reconnect AUTH_ACK_REQUEST

    // Outgoing step marker for the STS AUTH_ACK_REQUEST (msg 18), distinct from the bind steps.
    private static final byte STEP_SEND_STS_ACK = 0x12;

    // MBB HiChain data types (tag 0x01 of the 0x28 packet)
    private static final byte DATA_DH = 0x01;      // X25519 pubkey exchange
    private static final byte DATA_HICHAIN = 0x02; // PAKE JSON passthrough

    private byte operationCode = 0x01; // BIND
    private byte step;

    // X25519 device key exchange state (little-endian, RFC 7748)
    private byte[] devicePrivateKey = null;
    private byte[] devicePublicKey = null;
    // Not used for the bind password (kept for future reconnect/exchange work).
    public byte[] serverNonce = null;

    private HiChainPakeUtils.PakeState pakeState = null;
    private byte[] sessionKey = null;
    private byte[] pakeChallenge = null; // 32-byte combined challenge, for the exchange signature

    // STS reconnect state (operationCode 2 = PSK-SPEKE). msg 17 carries a throwaway ephemeral;
    // the real SPEKE runs in pakeState once the PIN is derived from the 32785 nonce.
    private byte[] stsSelfPrivate = null;   // ephemeral X25519 for the (cosmetic) msg 17 epk
    private byte[] stsSelfPublic = null;
    private byte[] stsSelfChallenge = null; // random challenge in msg 17
    private byte[] stsProof = null;         // our PAKE proof (kcfData) for AUTH_ACK_REQUEST (msg 18)

    private final int firstAuthenticateTimeout = 30 * 1000;
    private final int authenticateTimeout = 5000;

    /**
     * @param stsReconnect the watch offered pairType 2 (RECONNECT) in the security negotiation, so
     *   run the STS reconnect (operationCode 2). Otherwise run a full PSK-SPEKE bind (operationCode
     *   1) - this covers both first-pair and the reconnect case where the watch still asks for
     *   FIRST_PAIR because our bind has not registered us in its trust store. The bind PIN is
     *   DH-derived, so re-binding needs no user interaction. The 0x33 pairType dispatch
     *   (FIRST_PAIR vs RECONNECT) is observed from the pairing capture.
     */
    public GetHiChainPakeRequest(HuaweiSupportProvider support, boolean stsReconnect) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = HiChain.id;
        this.operationCode = stsReconnect ? (byte) 0x02 : (byte) 0x01;
        // The watch may interleave an async PermissionCheck round-trip before replying, so keep the
        // generous timeout for both paths.
        setupTimeoutUntilNext(firstAuthenticateTimeout);
        this.step = 0x00;
        this.pakeState = new HiChainPakeUtils.PakeState();
    }

    public GetHiChainPakeRequest(Request prevReq) {
        super(prevReq.supportProvider);
        this.serviceId = DeviceConfig.id;
        this.commandId = HiChain.id;
        GetHiChainPakeRequest hcReq = (GetHiChainPakeRequest) prevReq;
        this.operationCode = hcReq.operationCode;
        this.step = hcReq.step;
        this.devicePrivateKey = hcReq.devicePrivateKey;
        this.devicePublicKey = hcReq.devicePublicKey;
        this.serverNonce = hcReq.serverNonce;
        this.pakeState = hcReq.pakeState;
        this.sessionKey = hcReq.sessionKey;
        this.pakeChallenge = hcReq.pakeChallenge;
        this.stsSelfPrivate = hcReq.stsSelfPrivate;
        this.stsSelfPublic = hcReq.stsSelfPublic;
        this.stsSelfChallenge = hcReq.stsSelfChallenge;
        this.stsProof = hcReq.stsProof;
    }

    /**
     * Builds a HiChain PAKE passthrough packet: {@code {message, payload:{...}}} serialized
     * into TLV tag 0x02, with tag 0x01 = dataType 2. {@code version} and {@code operationCode}
     * are only present on the first message (RequestBase.sentPassthroughData).
     */
    private HuaweiPacket buildPakePacket(int messageId, JSONObject payload, boolean firstMessage)
            throws HuaweiPacket.SerializeException {
        return buildPakePacket(messageId, payload, firstMessage, false);
    }

    /**
     * @param authForm when true, adds {@code authForm:0} to the outer envelope. The device auth
     *                 engine (RequestBase.sentPassthroughData) tags every STS AuthRequest message
     *                 this way; PAKE bind messages omit it.
     */
    private HuaweiPacket buildPakePacket(int messageId, JSONObject payload, boolean firstMessage,
                                         boolean authForm) throws HuaweiPacket.SerializeException {
        try {
            if (firstMessage) {
                JSONObject version = new JSONObject();
                version.put("minVersion", "1.0.0");
                version.put("currentVersion", "2.0.15");
                payload.put("version", version);
                payload.put("operationCode", operationCode);
            }

            JSONObject value = new JSONObject();
            if (authForm) {
                value.put("authForm", 0);
            }
            value.put("message", messageId);
            value.put("payload", payload);

            final String json = value.toString();
            return new HuaweiPacket(paramsProvider) {{
                this.serviceId = DeviceConfig.id;
                this.commandId = HiChain.id;
                this.isSliced = true;
                this.isEncrypted = false;
                this.complete = true;
                this.tlv = new HuaweiTLV()
                        .put(0x01, DATA_HICHAIN)
                        .put(0x02, json);
            }};
        } catch (JSONException e) {
            throw new HuaweiPacket.SerializeException("PAKE JSON exception", e);
        }
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        LOG.debug("PAKE BindRequest operationCode: {} step: {}", operationCode, step);
        try {
            if (step == 0x00 && operationCode == 0x02) {
                // Reconnect: skip the X25519 PIN pre-exchange entirely and open the STS
                // handshake with AUTH_START_REQUEST (msg 17). On pairType==RECONNECT the
                // watch expects STS auth directly, with no DH pubkey exchange first.
                return createStsStartRequest();

            } else if (step == STEP_SEND_STS_ACK) {
                // Reconnect PSK-SPEKE client confirm (msg 18): our ephemeral epk, challenge and PAKE
                // proof, all computed when the AUTH_START_RESPONSE (msg 32785) was processed.
                JSONObject payload = new JSONObject();
                payload.put("challenge", StringUtils.bytesToHex(pakeState.selfChallenge));
                payload.put("epk", StringUtils.bytesToHex(pakeState.selfPublicParam));
                payload.put("kcfData", StringUtils.bytesToHex(stsProof));
                return buildPakePacket(MSG_STS_AUTH_ACK, payload, false, true).serialize();

            } else if (step == 0x00) {
                // Phase: X25519 ephemeral key exchange (dataType 1)
                devicePrivateKey = HiChainPakeUtils.randomBytes(32);
                devicePublicKey = HiChainPakeUtils.x25519(devicePrivateKey, new byte[]{9});
                LOG.debug("Device X25519 public key: {}", GB.hexdump(devicePublicKey));

                HuaweiPacket packet = new HuaweiPacket(paramsProvider) {{
                    this.serviceId = DeviceConfig.id;
                    this.commandId = HiChain.id;
                    this.isSliced = true;
                    this.isEncrypted = false;
                    this.complete = true;
                    this.tlv = new HuaweiTLV()
                            .put(0x01, DATA_DH)
                            .put(0x02, devicePublicKey);
                }};
                return packet.serialize();

            } else if (step == 0x01) {
                // Phase: PAKE start (msg 1)
                JSONObject payload = new JSONObject();
                payload.put("support256mod", true);
                payload.put("serviceType", "WEAR_MBB");
                return buildPakePacket(MSG_PAKE_START, payload, true).serialize();

            } else if (step == 0x02) {
                // Phase: client confirm (msg 2). Compute the shared secret, session/kcf keys and
                // proof now, as the HiChain PAKE client-confirm step does.
                pakeState.sharedSecret = HiChainPakeUtils.computeSharedSecret(
                        pakeState.peerPublicParam, pakeState.selfPrivateParam,
                        pakeState.mod, pakeState.params.modLen);
                LOG.debug("PAKE shared secret: {}", GB.hexdump(pakeState.sharedSecret));

                HiChainPakeUtils.SessionKeyResult keys = HiChainPakeUtils.deriveSessionKeys(
                        pakeState.sharedSecret, pakeState.salt, pakeState.params.isEcPake());
                pakeState.sessionKey = keys.sessionKey;
                pakeState.kcfKey = keys.kcfKey;
                sessionKey = keys.sessionKey;

                byte[] proof = HiChainPakeUtils.generateProof(
                        keys.kcfKey, pakeState.selfChallenge, pakeState.peerChallenge);

                JSONObject payload = new JSONObject();
                payload.put("challenge", StringUtils.bytesToHex(pakeState.selfChallenge));
                payload.put("epk", StringUtils.bytesToHex(pakeState.selfPublicParam));
                payload.put("kcfData", StringUtils.bytesToHex(proof));
                return buildPakePacket(MSG_PAKE_CLIENT_CONFIRM, payload, false).serialize();

            } else if (step == 0x03) {
                // Phase: key exchange (msg 3). exAuthInfo = AES-GCM(sessionKey, info || sig),
                // where info = {"authId","authPk"} JSON and sig = Ed25519 over
                // sha256(pakeChallenge || info). Follows the HiChain key-exchange request step.
                byte[] selfAuthId = supportProvider.getAndroidId();
                byte[] seed = supportProvider.getPakeIdentitySeed();
                byte[] publicKey = HiChainPakeUtils.ed25519PublicKey(seed);

                JSONObject info = new JSONObject();
                info.put("authId", StringUtils.bytesToHex(selfAuthId));
                info.put("authPk", StringUtils.bytesToHex(publicKey));
                byte[] infoBytes = info.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

                byte[] digest = HiChainPakeUtils.sha256(
                        HiChainPakeUtils.concat(pakeChallenge, infoBytes));
                byte[] signature = HiChainPakeUtils.ed25519Sign(seed, digest);

                byte[] plaintext = HiChainPakeUtils.concat(infoBytes, signature);
                byte[] exAuthInfo = HiChainPakeUtils.encryptAesGcm(
                        plaintext, sessionKey, HiChainPakeUtils.EXCHANGE_REQUEST_AAD);

                JSONObject payload = new JSONObject();
                payload.put("exAuthInfo", StringUtils.bytesToHex(exAuthInfo));
                return buildPakePacket(MSG_EXCHANGE_REQUEST, payload, false).serialize();
            }
        } catch (Exception e) {
            throw new RequestCreationException("HiChain PAKE exception", e);
        }
        return null;
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        if (!(receivedPacket instanceof HiChain.Response))
            throw new ResponseTypeMismatchException(receivedPacket, HiChain.Response.class);

        HiChain.Response response = (HiChain.Response) receivedPacket;
        if (response.errorCode != 0) {
            throw new ResponseParseException("HiChain PAKE peer error 0x" + Integer.toHexString(response.errorCode));
        }
        step = response.step;
        LOG.debug("PAKE response step: {}", step);

        try {
            if (step == 0x00) {
                // X25519 exchange response -> derive the pairing PIN
                if (response.x25519KeyExchangeData == null) {
                    throw new ResponseParseException("Expected X25519 key exchange response");
                }
                byte[] peerPub = response.x25519KeyExchangeData.publicKey;
                byte[] shared = HiChainPakeUtils.x25519(devicePrivateKey, peerPub);
                pakeState.pinHex = HiChainPakeUtils.derivePin(shared);
                LOG.debug("Derived PAKE PIN ({} chars)", pakeState.pinHex.length());

                advance(0x01);

            } else if (step == 0x01) {
                // PAKE response (msg 32769): challenge, salt, epk
                if (response.pakeResponseData == null) {
                    throw new ResponseParseException("Expected PAKE response (challenge/salt/epk)");
                }
                pakeState.peerChallenge = response.pakeResponseData.challenge;
                pakeState.salt = response.pakeResponseData.salt;
                pakeState.peerPublicParam = response.pakeResponseData.epk;

                // Honor Watch 5 uses EC-SPEKE (X25519). Peer epk is 32 bytes.
                pakeState.params = HiChainPakeUtils.determineAlgorithm(true, pakeState.peerPublicParam);
                pakeState.mod = pakeState.params.getMod();

                if (pakeState.pinHex == null) {
                    throw new ResponseParseException("No PIN available (X25519 step did not run)");
                }
                pakeState.passwordKey = HiChainPakeUtils.derivePasswordKey(pakeState.pinHex, pakeState.salt);
                pakeState.sharedBase = HiChainPakeUtils.computeSharedBase(pakeState.passwordKey, pakeState.mod);

                pakeState.selfChallenge = HiChainPakeUtils.randomBytes(16);
                pakeState.selfPrivateParam = HiChainPakeUtils.randomBytes(pakeState.params.privateParamLen);
                pakeState.selfPublicParam = HiChainPakeUtils.computePublicParameter(
                        pakeState.sharedBase, pakeState.selfPrivateParam,
                        pakeState.mod, pakeState.params.modLen);
                LOG.debug("Our epk: {} ({} bytes)", GB.hexdump(pakeState.selfPublicParam),
                        pakeState.selfPublicParam.length);

                advance(0x02);

            } else if (step == 0x02) {
                // Server confirm (msg 32770): verify the server proof
                if (response.pakeConfirmData == null) {
                    throw new ResponseParseException("Expected PAKE server confirm (kcfData)");
                }
                boolean valid = HiChainPakeUtils.verifyProof(
                        pakeState.kcfKey, pakeState.peerChallenge, pakeState.selfChallenge,
                        response.pakeConfirmData.kcfData);
                if (!valid) {
                    throw new ResponseParseException("PAKE server proof verification FAILED "
                            + "(PIN/Elligator2/SPEKE mismatch)");
                }
                LOG.info("PAKE server proof VERIFIED - mutual auth OK (exchange step still a stub)");

                byte[] combined = new byte[32];
                System.arraycopy(pakeState.selfChallenge, 0, combined, 0, 16);
                System.arraycopy(pakeState.peerChallenge, 0, combined, 16, 16);
                pakeChallenge = combined;

                advance(0x03);

            } else if (step == 0x03) {
                // Exchange response (msg 32771): decrypt the peer's info || Ed25519 signature and
                // verify it, as the HiChain key-exchange verify/save step does.
                if (response.pakeExchangeData == null || response.pakeExchangeData.exAuthInfo == null) {
                    throw new ResponseParseException("Expected PAKE exchange response (exAuthInfo)");
                }
                byte[] plaintext = HiChainPakeUtils.decryptAesGcm(
                        response.pakeExchangeData.exAuthInfo, sessionKey,
                        HiChainPakeUtils.EXCHANGE_RESPONSE_AAD);
                if (plaintext.length <= 64) {
                    throw new ResponseParseException("Bad PAKE exchange response payload");
                }
                int infoLen = plaintext.length - 64;
                byte[] peerInfo = java.util.Arrays.copyOfRange(plaintext, 0, infoLen);
                byte[] peerSig = java.util.Arrays.copyOfRange(plaintext, infoLen, plaintext.length);

                JSONObject peerJson = new JSONObject(
                        new String(peerInfo, java.nio.charset.StandardCharsets.UTF_8));
                byte[] peerAuthId = StringUtils.hexToBytes(peerJson.getString("authId"));
                byte[] peerPubKey = StringUtils.hexToBytes(peerJson.getString("authPk"));

                // Peer signed sha256( (peerChallenge || selfChallenge) || peerInfo )
                byte[] reordered = new byte[32];
                System.arraycopy(pakeState.peerChallenge, 0, reordered, 0, 16);
                System.arraycopy(pakeState.selfChallenge, 0, reordered, 16, 16);
                byte[] peerDigest = HiChainPakeUtils.sha256(
                        HiChainPakeUtils.concat(reordered, peerInfo));
                boolean sigOk = HiChainPakeUtils.ed25519Verify(peerDigest, peerSig, peerPubKey);
                if (!sigOk) {
                    throw new ResponseParseException("PAKE exchange: peer identity signature "
                            + "verification FAILED (Ed25519 mismatch)");
                }
                LOG.info("PAKE exchange complete - peer identity verified, BIND OK");
                pakeState.peerAuthId = peerAuthId;
                // Persist the watch identity so the STS reconnect (operationCode 2) can re-auth
                // against it without a fresh PIN.
                supportProvider.savePakePeerIdentity(peerAuthId, peerPubKey);
                // No advance(): the request chain finalizes here and init proceeds (configureReq).

            } else if (step == HiChain.Response.STEP_STS_START) {
                // Reconnect AUTH_START_RESPONSE (msg 32785): derive the PSK-SPEKE PIN from the
                // server nonce + stored identities, then run the PAKE client-confirm.
                processStsStartResponse(response);
                advance(STEP_SEND_STS_ACK);

            } else if (step == HiChain.Response.STEP_STS_ACK) {
                // Reconnect AUTH_ACK_RESPONSE (msg 32786): PAKE server confirm. Verify the watch's
                // proof over (peerChallenge || selfChallenge) with our derived kcfKey.
                if (response.stsAckData == null) {
                    throw new ResponseParseException("Expected STS ack response (kcfData)");
                }
                boolean valid = HiChainPakeUtils.verifyProof(
                        pakeState.kcfKey, pakeState.peerChallenge, pakeState.selfChallenge,
                        response.stsAckData.kcfData);
                if (!valid) {
                    throw new ResponseParseException(
                            "STS reconnect server proof verification FAILED (PSK/PIN mismatch)");
                }
                LOG.info("STS reconnect complete - mutual auth OK, RECONNECT OK");
                // No advance(): the chain finalizes here and init proceeds (configureReq).
            }
        } catch (ResponseParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseParseException("HiChain PAKE response error: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the STS AUTH_START_REQUEST (msg 17) that opens the reconnect handshake. Generates a
     * fresh ephemeral X25519 key pair and challenge; declares our own authId in the (misleadingly
     * named) {@code peerAuthId} field, as the OpenHarmony device_auth STS auth-start does.
     */
    private List<byte[]> createStsStartRequest() throws Exception {
        stsSelfPrivate = HiChainPakeUtils.randomBytes(32);
        stsSelfPublic = HiChainPakeUtils.x25519(stsSelfPrivate, new byte[]{9});
        stsSelfChallenge = HiChainPakeUtils.randomBytes(16);
        byte[] selfAuthId = supportProvider.getAndroidId();
        LOG.debug("STS reconnect: sending AUTH_START_REQUEST, epk={}", GB.hexdump(stsSelfPublic));

        JSONObject payload = new JSONObject();
        payload.put("challenge", StringUtils.bytesToHex(stsSelfChallenge));
        payload.put("epk", StringUtils.bytesToHex(stsSelfPublic));
        payload.put("serviceType", "WEAR_MBB");
        payload.put("peerAuthId", StringUtils.bytesToHex(selfAuthId));
        payload.put("keyLength", 32);
        payload.put("keyInfoSize", 0);
        return buildPakePacket(MSG_STS_AUTH_START, payload, true, true).serialize();
    }

    /**
     * Handles the reconnect AUTH_START_RESPONSE (msg 32785), which is PSK-SPEKE. The payload is a
     * PAKE response (challenge/salt/epk) plus a server {@code nonce}. The SPEKE password (PIN) is
     * {@code lowercase_hex(HKDF(PSK, nonce, "hichain_tmp_auth_enc_key", 32))}, where PSK is the
     * static X25519 ECDH between our Ed25519 identity and the watch's (both exchanged at bind). We
     * then run the exact same PAKE client-confirm as the bind step, producing our epk, challenge
     * and proof (msg 18). Requires the peer identity persisted at bind; without it, re-pair.
     * Follows the HiChain KeyManager (getTmpAuthKey/computePsk) + PakeTask key agreement.
     */
    private void processStsStartResponse(HiChain.Response response) throws Exception {
        if (response.stsStartData == null) {
            throw new ResponseParseException("Expected STS start response (challenge/salt/epk/nonce)");
        }
        byte[] seed = supportProvider.getPakeIdentitySeed();
        byte[] peerAuthPk = supportProvider.getPakePeerAuthPk();
        if (peerAuthPk == null) {
            throw new ResponseParseException(
                    "No stored peer identity for STS reconnect - the watch must be re-paired");
        }

        // PSK-SPEKE PIN from the static identity ECDH and the server nonce.
        byte[] psk = HiChainPakeUtils.computeStsPsk(seed, peerAuthPk);
        String pinHex = HiChainPakeUtils.deriveStsPin(psk, response.stsStartData.nonce);
        LOG.debug("STS reconnect: derived PSK-SPEKE PIN ({} chars)", pinHex.length());

        // From here the math is identical to the bind PAKE client-confirm (step 0x02).
        pakeState.peerChallenge = response.stsStartData.challenge;
        pakeState.salt = response.stsStartData.salt;
        pakeState.peerPublicParam = response.stsStartData.epk;
        pakeState.params = HiChainPakeUtils.determineAlgorithm(true, pakeState.peerPublicParam);
        pakeState.mod = pakeState.params.getMod();
        pakeState.pinHex = pinHex;
        pakeState.passwordKey = HiChainPakeUtils.derivePasswordKey(pinHex, pakeState.salt);
        pakeState.sharedBase = HiChainPakeUtils.computeSharedBase(pakeState.passwordKey, pakeState.mod);

        pakeState.selfChallenge = HiChainPakeUtils.randomBytes(16);
        pakeState.selfPrivateParam = HiChainPakeUtils.randomBytes(pakeState.params.privateParamLen);
        pakeState.selfPublicParam = HiChainPakeUtils.computePublicParameter(
                pakeState.sharedBase, pakeState.selfPrivateParam,
                pakeState.mod, pakeState.params.modLen);

        pakeState.sharedSecret = HiChainPakeUtils.computeSharedSecret(
                pakeState.peerPublicParam, pakeState.selfPrivateParam,
                pakeState.mod, pakeState.params.modLen);
        HiChainPakeUtils.SessionKeyResult keys = HiChainPakeUtils.deriveSessionKeys(
                pakeState.sharedSecret, pakeState.salt, pakeState.params.isEcPake());
        pakeState.sessionKey = keys.sessionKey;
        pakeState.kcfKey = keys.kcfKey;
        sessionKey = keys.sessionKey;

        stsProof = HiChainPakeUtils.generateProof(
                keys.kcfKey, pakeState.selfChallenge, pakeState.peerChallenge);
        LOG.debug("STS reconnect: PAKE params computed, sending AUTH_ACK_REQUEST (msg 18)");
    }

    private void advance(int nextStep) {
        this.step = (byte) nextStep;
        GetHiChainPakeRequest nextRequest = new GetHiChainPakeRequest(this);
        nextRequest.setFinalizeReq(this.finalizeReq);
        this.nextRequest(nextRequest);
    }
}
