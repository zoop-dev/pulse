/*  Copyright (C) 2025 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services;

import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdateDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsDeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsDeviceSources;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

public class ZeppOsDeviceInfoService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsDeviceInfoService.class);

    private static final short ENDPOINT = 0x0043;

    private static final byte CMD_REQUEST = 0x01;
    private static final byte CMD_REPLY = 0x02;

    public static String PREF_KEY_DEVICE_PNP_ID = "zepp_os_pnp_id";

    public ZeppOsDeviceInfoService(final ZeppOsSupport support) {
        super(support, false);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        if (payload[0] != CMD_REPLY) {
            LOG.warn("Unexpected device info payload byte {}", String.format("0x%02x", payload[0]));
            return;
        }

        final List<GBDeviceEvent> events = decodeDeviceInfo(payload);
        for (GBDeviceEvent event : events) {
            evaluateGBDeviceEvent(event);
        }
    }

    public static List<GBDeviceEvent> decodeDeviceInfo(final byte[] payload) {
        final ByteBuffer buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);

        final GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();

        final List<GBDeviceEvent> events = new ArrayList<>(2);
        events.add(versionInfo);

        if (buf.get() != CMD_REPLY) {
            throw new IllegalArgumentException("not a device info reply payload");
        }

        final byte one = buf.get();
        if (one != 1) {
            LOG.warn("Unexpected device info payload 2nd byte {}", String.format("0x%02x", one));
            return events;
        }

        // Active 2:    0x00000000000000ff
        // Helio Strap: 0x000000000000007f
        // Probably some flags?
        // Assume the first 4 bits indicate the first 4 fields, since the last bit in the helio did
        // not seem to change the order, but there is less data

        final long flags = buf.getLong();

        if ((flags & 1) != 0) {
            final int unk3count = buf.get() & 0xff;
            buf.get(new byte[unk3count]);
        }

        if ((flags & 2) != 0) {
            final String serialNumber = StringUtils.untilNullTerminator(buf);
        }

        if ((flags & 4) != 0) {
            versionInfo.hwVersion = StringUtils.untilNullTerminator(buf);
        }

        if ((flags & 8) != 0) {
            versionInfo.fwVersion = StringUtils.untilNullTerminator(buf);
        }

        if ((flags & 16) != 0 && buf.remaining() >= 7) {
            final byte[] pnpId = new byte[7];
            buf.get(pnpId);

            events.add(new GBDeviceEventUpdatePreferences(
                    PREF_KEY_DEVICE_PNP_ID,
                    GB.hexdump(pnpId)
            ));

            final ZeppOsDeviceInfo deviceInfo = getDeviceInfo(pnpId);

            LOG.debug("Got PNP ID={} -> {}", GB.hexdump(pnpId), deviceInfo);

            if (deviceInfo != null) {
                events.add(new GBDeviceEventUpdateDeviceInfo("PRODUCT_ID: ", String.valueOf(deviceInfo.getProductId())));
                events.add(new GBDeviceEventUpdateDeviceInfo("PRODUCT_VERSION: ", String.valueOf(deviceInfo.getProductVersion())));
                events.add(new GBDeviceEventUpdateDeviceInfo("DEVICE_SOURCE: ", String.valueOf(deviceInfo.getDeviceSource())));
            }
        }

        return events;
    }

    @Nullable
    public static ZeppOsDeviceInfo getDeviceInfo(final GBDevice gbDevice) {
        final DevicePrefs devicePrefs = GBApplication.getDevicePrefs(gbDevice);
        final String pnpHex = devicePrefs.getString(PREF_KEY_DEVICE_PNP_ID, null);
        if (pnpHex == null || pnpHex.length() != 14) {
            LOG.error("Unknown or invalid PNP ID for {}: {}", gbDevice, pnpHex);
            return null;
        }

        final byte[] pnpId = GB.hexStringToByteArray(pnpHex);

        return getDeviceInfo(pnpId);
    }

    @Nullable
    public static ZeppOsDeviceInfo getDeviceInfo(final byte[] pnpId) {
        final int productId = BLETypeConversions.toUint16(pnpId, 3);
        final int productVersion = BLETypeConversions.toUint16(pnpId, 5);

        LOG.debug(
                "Resolving source from pnpId={} for productId={}, productVersion={}",
                GB.hexdump(pnpId),
                productId,
                productVersion
        );

        return ZeppOsDeviceSources.INSTANCE.resolve(productId, productVersion);
    }

    public void requestDeviceInfo(final ZeppOsTransactionBuilder builder) {
        write(builder, CMD_REQUEST);
    }
}
