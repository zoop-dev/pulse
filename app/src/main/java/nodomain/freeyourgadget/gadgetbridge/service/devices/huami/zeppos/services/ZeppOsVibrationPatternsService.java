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

import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX;
import static nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst.PREF_HUAMI_VIBRATION_TRY_PREFIX;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.AbstractZeppOsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ZeppOsVibrationPatternsService extends AbstractZeppOsService {
    private static final Logger LOG = LoggerFactory.getLogger(ZeppOsVibrationPatternsService.class);

    private static final short ENDPOINT = 0x0018;

    private static final byte VIBRATION_PATTERN_SET = 0x03;
    private static final byte VIBRATION_PATTERN_ACK = 0x04;

    public ZeppOsVibrationPatternsService(final ZeppOsSupport support) {
        super(support, true);
    }

    @Override
    public short getEndpoint() {
        return ENDPOINT;
    }

    @Override
    public void handlePayload(final byte[] payload) {
        //noinspection SwitchStatementWithTooFewBranches
        switch (payload[0]) {
            case VIBRATION_PATTERN_ACK:
                LOG.info("Vibration Patterns ACK, status = {}", payload[1]);
                return;
            default:
                LOG.warn("Unexpected Vibration Patterns payload byte {}", String.format("0x%02x", payload[0]));
        }
    }

    @Override
    public void initialize(final ZeppOsTransactionBuilder builder) {
        for (final HuamiVibrationPatternNotificationType type : getCoordinator().getVibrationPatternNotificationTypes(getSupport().getDevice())) {
            // FIXME: Can we read these from the band?
            final String typeKey = type.name().toLowerCase(Locale.ROOT);
            setVibrationPattern(builder, HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX + typeKey);
        }
    }

    @Override
    public boolean onSendConfiguration(final String config, final Prefs prefs) {
        if (config.startsWith(HuamiConst.PREF_HUAMI_VIBRATION_PROFILE_PREFIX) ||
                config.startsWith(HuamiConst.PREF_HUAMI_VIBRATION_COUNT_PREFIX) ||
                config.startsWith(HuamiConst.PREF_HUAMI_VIBRATION_TRY_PREFIX)) {
            withTransactionBuilder(
                    "send " + config,
                    builder -> setVibrationPattern(builder, config)
            );
            return true;
        }

        return false;
    }


    protected void setVibrationPattern(final ZeppOsTransactionBuilder builder, final String preferenceKey) {
        // The preference key has one of the 3 prefixes
        final String notificationTypeName = preferenceKey.replace(PREF_HUAMI_VIBRATION_COUNT_PREFIX, "")
                .replace(PREF_HUAMI_VIBRATION_PROFILE_PREFIX, "")
                .replace(PREF_HUAMI_VIBRATION_TRY_PREFIX, "")
                .toUpperCase(Locale.ROOT);
        final HuamiVibrationPatternNotificationType notificationType = HuamiVibrationPatternNotificationType.valueOf(notificationTypeName);
        final boolean isTry = preferenceKey.startsWith(PREF_HUAMI_VIBRATION_TRY_PREFIX);

        final VibrationProfile vibrationProfile = HuamiCoordinator.getVibrationProfile(
                getSupport().getDevice().getAddress(),
                notificationType,
                true
        );

        setVibrationPattern(builder, notificationType, isTry, vibrationProfile);
    }

    private void setVibrationPattern(final ZeppOsTransactionBuilder builder,
                                     final HuamiVibrationPatternNotificationType notificationType,
                                     final boolean test,
                                     final VibrationProfile profile) {
        final int MAX_TOTAL_LENGTH_MS = 10_000; // 10 seconds, about as long as Mi Fit allows

        // The on-off sequence, until the max total length is reached
        final List<Short> onOff = HuamiUtils.truncateVibrationsOnOff(profile, MAX_TOTAL_LENGTH_MS);

        final ByteBuffer buf = ByteBuffer.allocate(5 + 2 * onOff.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.put(VIBRATION_PATTERN_SET);
        buf.put(notificationType.getCode());
        buf.put((byte) (profile != null ? 1 : 0)); // 1 for custom, 0 for device default
        buf.put((byte) (test ? 1 : 0));
        buf.put((byte) (onOff.size() / 2));

        for (Short time : onOff) {
            buf.putShort(time);
        }

        write(builder, buf.array());
    }
}
