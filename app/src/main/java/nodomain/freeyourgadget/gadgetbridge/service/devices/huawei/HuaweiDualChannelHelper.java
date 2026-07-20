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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;

/**
 * Holds the "dual channel" routing tables negotiated with the watch through the 0x3C command
 * ({@link DeviceConfig.DualChannel}) and decides which RFCOMM socket a given packet is sent on.
 * <p>
 * This is a direct port of the vendor {@code DeviceDualChannelHelper}: the primary socket is
 * channel 0, the extra socket is the negotiated channel number. A packet goes on the extra
 * channel when its service is flagged wholesale, or its (service, command) pair is listed, or —
 * for the P2P services 0x34/0x37 — its destination package name is listed.
 * <p>
 * This class carries no transport; it only answers "primary or extra". The socket itself is owned
 * by the transport layer, so this logic is independent of how the second socket is implemented.
 */
public class HuaweiDualChannelHelper {
    private static final Logger LOG = LoggerFactory.getLogger(HuaweiDualChannelHelper.class);

    /** Services the vendor always keeps on the primary socket, whatever the routing tables say. */
    private static final Set<Integer> ALWAYS_MAIN_CHANNEL = new HashSet<>();
    static {
        for (byte s : DeviceConfig.DualChannel.alwaysMainChannelServices)
            ALWAYS_MAIN_CHANNEL.add(s & 0xFF);
    }

    private boolean active = false;
    private int channel = 0;
    private final Set<Integer> dualServices = new HashSet<>();
    private final List<DeviceConfig.DualChannel.ChannelEntry> entries = new ArrayList<>();

    /**
     * Watch 4 firmware quirk: while dual channel is active it never answers the 0x2C
     * FileDownloadInit on the main socket, but answers immediately on aux. When the download
     * manager detects that (init timeout on main), it sets this flag so all further 0x2C traffic
     * of this connection rides the aux socket. Reset on (re)negotiation.
     */
    private boolean fileDownloadViaAux = false;

    /** Stores the routing tables from a parsed 0x3C response. Call on (re)connection. */
    public synchronized void update(DeviceConfig.DualChannel.Response response) {
        reset();
        this.channel = response.channel;
        for (byte s : response.dualServices)
            this.dualServices.add(s & 0xFF);
        this.entries.addAll(response.entries);
        this.active = this.channel > 0;
        LOG.debug("Dual channel updated: active={}, channel={}, dualServices={}, entries={}",
                active, channel, dualServices.size(), entries.size());
    }

    public synchronized void reset() {
        active = false;
        channel = 0;
        dualServices.clear();
        entries.clear();
        fileDownloadViaAux = false;
    }

    /** See {@link #fileDownloadViaAux}. */
    public synchronized void setFileDownloadViaAux(boolean fileDownloadViaAux) {
        this.fileDownloadViaAux = fileDownloadViaAux;
    }

    public synchronized boolean isFileDownloadViaAux() {
        return fileDownloadViaAux;
    }

    /** The negotiated RFCOMM channel of the extra socket, or 0 when dual channel is inactive. */
    public synchronized int getChannel() {
        return channel;
    }

    /** True once a channel has been negotiated and the extra socket may be used. */
    public synchronized boolean isActive() {
        return active && channel > 0;
    }

    /**
     * Whether the packet for {@code (serviceId, commandId)} should be sent over the extra socket.
     * Returns false (i.e. use the primary socket) whenever dual channel is inactive or the packet
     * is not listed, so callers can route unconditionally and non-dual devices are unaffected.
     */
    public synchronized boolean useExtraChannel(int serviceId, int commandId) {
        if (!isActive())
            return false;
        // Firmware fallback (see fileDownloadViaAux): file download moved to aux for this
        // connection after the watch ignored it on main. Checked before ALWAYS_MAIN_CHANNEL,
        // which would otherwise pin 0x2C to the main socket.
        if (fileDownloadViaAux && (serviceId & 0xFF) == 0x2C)
            return true;
        if (ALWAYS_MAIN_CHANNEL.contains(serviceId & 0xFF))
            return false;
        if (dualServices.contains(serviceId & 0xFF))
            return true;
        for (DeviceConfig.DualChannel.ChannelEntry entry : entries) {
            if ((entry.service & 0xFF) != (serviceId & 0xFF))
                continue;
            for (byte c : entry.commands) {
                if ((c & 0xFF) == (commandId & 0xFF))
                    return true;
            }
        }
        return false;
    }

    /**
     * P2P routing for services 0x34 and 0x37: the extra channel is chosen by the destination
     * package name (as advertised in the 0x3C response), not by command id.
     */
    public synchronized boolean useExtraChannelForPackage(int serviceId, String packageName) {
        if (!isActive() || packageName == null)
            return false;
        for (DeviceConfig.DualChannel.ChannelEntry entry : entries) {
            if ((entry.service & 0xFF) == (serviceId & 0xFF) && entry.packages.contains(packageName))
                return true;
        }
        return false;
    }
}
