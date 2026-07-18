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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests.Request;

/**
 * Manages all response data.
 */
public class ResponseManager {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseManager.class);

    private final List<Request> handlers = Collections.synchronizedList(new ArrayList<>());
    // Reassembly state is per transport channel: the main socket and each aux (dual channel)
    // socket run on their own read thread and deliver independent byte streams. Sharing a single
    // partial-packet buffer between them lets one stream's fragment corrupt the other's in-progress
    // (possibly sliced) packet, which throws it away silently. Keying by channel keeps each stream's
    // reassembly isolated. Access is guarded by handleData being synchronized.
    private final Map<Integer, HuaweiPacket> receivedPackets = new HashMap<>();
    private final AsynchronousResponse asynchronousResponse;
    private final HuaweiSupportProvider support;

    public ResponseManager(HuaweiSupportProvider support) {
        this.asynchronousResponse = new AsynchronousResponse(support);
        this.support = support;
    }

    /**
     * Add a request to the response handler list
     * @param handler The request to handle responses
     */
    public void addHandler(Request handler) {
        synchronized (handlers) {
            handlers.add(handler);
        }
    }

    /**
     * Remove a request from the response handler list
     * @param handler The request to remove
     */
    public void removeHandler(Request handler) {
        synchronized (handlers) {
            handlers.remove(handler);
        }
    }

    /**
     * Remove all requests with specified class from the response handler list
     * @param handlerClass The class of which the requests are removed
     */
    public void removeHandler(Class<?> handlerClass) {
        synchronized (handlers) {
            handlers.removeIf(request -> request.getClass() == handlerClass);
        }
    }

    /**
     * Parses the data into a Huawei Packet.
     * If the packet is complete, it will be handled by the first request that accepts it,
     * or as an asynchronous request otherwise.
     *
     * @param data The received data
     */
    public void handleData(byte[] data) {
        handleData(data, 0);
    }

    /**
     * Parses the data into a Huawei Packet.
     * If the packet is complete, it will be handled by the first request that accepts it,
     * or as an asynchronous request otherwise.
     *
     * @param data    The received data
     * @param channel The transport channel the data arrived on (0 = primary socket / BLE,
     *                &gt;0 = negotiated dual channel aux socket). Reassembly state is kept per
     *                channel so the two independent socket streams never corrupt each other.
     */
    public synchronized void handleData(byte[] data, int channel) {
        //NOTE: This is a quick fix issue with concatenated packets.
        //TODO: Extract transport related code from packet.
        int left = 0;
        do {
            if(left > 0)
                data = Arrays.copyOfRange(data, data.length - left, data.length);

            HuaweiPacket receivedPacket = receivedPackets.get(channel);
            try {
                if (receivedPacket == null)
                    receivedPacket = new HuaweiPacket(support.getParamsProvider()).parse(data);
                else
                    receivedPacket = receivedPacket.parse(data);

                receivedPackets.put(channel, receivedPacket);
                left = receivedPacket.getLeft();
            } catch (HuaweiPacket.ParseException e) {
                LOG.error("Packet parse exception", e);

                // Clean up so the next message may be parsed correctly
                receivedPackets.remove(channel);
                return;
            }

            if (receivedPacket.complete) {
                Request handler = null;
                synchronized (handlers) {
                    for (Request req : handlers) {
                        if (req.handleResponse(receivedPacket)) {
                            handler = req;
                            break;
                        }
                    }
                }

                if (handler == null) {
                    LOG.debug("Service: {}, command: {}, asynchronous response.", Integer.toHexString(receivedPacket.serviceId & 0xff), Integer.toHexString(receivedPacket.commandId & 0xff));

                    // Asynchronous response
                    asynchronousResponse.handleResponse(receivedPacket);
                } else {
                    LOG.debug("Service: {}, command: {}, handled by: {}", Integer.toHexString(receivedPacket.serviceId & 0xff), Integer.toHexString(receivedPacket.commandId & 0xff), handler.getClass());

                    if (handler.autoRemoveFromResponseHandler()) {
                        synchronized (handlers) {
                            handlers.remove(handler);
                        }
                    }

                    handler.handleResponse();
                }
                receivedPackets.remove(channel);
            }
        } while (left > 0);
    }
}
