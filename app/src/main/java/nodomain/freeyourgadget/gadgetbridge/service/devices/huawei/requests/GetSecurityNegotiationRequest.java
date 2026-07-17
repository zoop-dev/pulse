/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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

import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSecurityNegotiationRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetSecurityNegotiationRequest.class);
    public int authType = 0x00;
    // Watch-dictated auth path (tag 0x02 of the 0x33 response): 1 = fresh bind, 2 = STS reconnect,
    // -1 = not reported. Drives whether initializeDeviceHiChainMode runs bind or STS.
    public int honorPairType = -1;
    public byte[] responseNonce;

    public GetSecurityNegotiationRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.SecurityNegotiation.id;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            if (supportProvider.getCoordinator().supportsHiChainPake()) {
                // Honor PAKE devices (Honor Watch 5) use the MBB auth-type request: authType,
                // pairType and our authId in tags 1/2/3. Once a bind has registered our identity
                // with the watch (we have a stored peer authId) we request RECONNECT (pairType 2),
                // which the watch grants and answers with an STS PSK-SPEKE handshake (msg 17/18 <->
                // 32785/32786; see GetHiChainPakeRequest). Before that first bind, or if the stored
                // identity is stale, we request FIRST_PAIR (pairType 1). If the watch declines STS
                // it simply answers pairType 1 and we fall back to a fresh (seamless) bind.
                int honorPairType = (supportProvider.getPakePeerAuthId() != null) ? 2 : 1;
                return new DeviceConfig.SecurityNegotiation.Request(
                        paramsProvider,
                        paramsProvider.getAuthMode(),
                        honorPairType,
                        supportProvider.getAndroidId()
                    ).serialize();
            }
            return new DeviceConfig.SecurityNegotiation.Request(
                    paramsProvider,
                    paramsProvider.getAuthMode(),
                    supportProvider.getAndroidId(),
                    Build.MODEL
                ).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Security and Negotiation");

        if (!(receivedPacket instanceof DeviceConfig.SecurityNegotiation.Response)) {
            // TODO: exception
            return;
        }

        this.authType = ((DeviceConfig.SecurityNegotiation.Response) receivedPacket).authType;
        this.honorPairType = ((DeviceConfig.SecurityNegotiation.Response) receivedPacket).honorPairType;
        this.responseNonce = ((DeviceConfig.SecurityNegotiation.Response) receivedPacket).responseNonce;
    }
}
