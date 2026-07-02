/*  Copyright (C) 2026 Gadgetbridge contributors

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.devices.xiaomi.XiaomiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.preferences.DevicePrefs;

/**
 * Custom vibration patterns, which ride on the System command type - so {@link XiaomiSystemService}
 * owns this and forwards command/config hooks rather than registering a separate service. Band
 * quirks: a GET never returns segments (only the type mapping + ids), a create missing the required
 * id is silently dropped, and a new CALL-typed pattern is auto-assigned to incoming calls.
 */
public final class XiaomiVibrationManager {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiVibrationManager.class);

    public static final int CMD_GET = 46;
    public static final int CMD_ADD = 58;
    public static final int CMD_REMOVE = 61;

    public static final String PREF_PATTERNS = "vibration_patterns"; // value is a protobuf hex string
    public static final String PREF_PATTERNS_SCREEN = "pref_xiaomi_vibration_patterns"; // settings screen key
    public static final String PREF_REFRESH = "vibration_refresh"; // action: re-fetch patterns from the band
    public static final String PREF_ADD = "vibration_add"; // action: create PREF_ADD_PATTERN
    public static final String PREF_ADD_PATTERN = "vibration_add_pattern"; // protobuf hex of the pattern to create
    public static final String PREF_REMOVE = "vibration_remove"; // action: delete PREF_REMOVE_ID
    public static final String PREF_REMOVE_ID = "vibration_remove_id"; // id of the custom pattern to delete

    private final XiaomiSupport support;

    public XiaomiVibrationManager(final XiaomiSupport support) {
        this.support = support;
    }

    public boolean handleCommand(final XiaomiProto.Command cmd) {
        switch (cmd.getSubtype()) {
            case CMD_GET:
                handlePatterns(cmd.getSystem().getVibrationPatterns());
                return true;
            case CMD_ADD:
            case CMD_REMOVE:
                // reference VibratorError: 0 OK, 1 NUMBER_LIMIT, 2 ID_NOT_EXIST
                LOG.info("[vibration] write response: cmdStatus={} errorCode={}",
                        cmd.getStatus(), cmd.getSystem().getVibrationPatternAck().getStatus());
                return true;
            default:
                return false;
        }
    }

    public boolean onSendConfiguration(final String config) {
        switch (config) {
            case PREF_REFRESH:
                support.sendCommand("get vibration patterns", XiaomiSystemService.COMMAND_TYPE, CMD_GET);
                return true;
            case PREF_ADD:
                ifEditingSupported("add", this::addPattern);
                return true;
            case PREF_REMOVE:
                ifEditingSupported("remove", this::removePattern);
                return true;
            default:
                return false;
        }
    }

    private void ifEditingSupported(final String action, final Runnable editAction) {
        if (support.getCoordinator().supportsCustomVibrationPatterns(support.getDevice())) {
            editAction.run();
        } else {
            LOG.warn("Ignoring vibration {} - editing not supported on this device", action);
        }
    }

    private void handlePatterns(final XiaomiProto.VibrationPatterns vibrationPatterns) {
        LOG.info("[vibration] response: {} notification types, {} custom patterns",
                vibrationPatterns.getNotificationTypeCount(), vibrationPatterns.getCustomVibrationPatternCount());

        // a partial response (the echo after add/remove) lacks the mapping; don't clobber the cache
        if (vibrationPatterns.getNotificationTypeCount() == 0) {
            LOG.info("[vibration] ignoring partial response (no notification types)");
            return;
        }

        support.evaluateGBDeviceEvent(new GBDeviceEventUpdatePreferences()
                .withPreference(PREF_PATTERNS, GB.hexdump(vibrationPatterns.toByteArray())));
    }

    private void addPattern() {
        final XiaomiCoordinator coordinator = support.getCoordinator();
        final XiaomiProto.VibrationPatterns existing = parseVibrationPatterns(prefs().getString(PREF_PATTERNS, null));
        int nextId = 1;
        for (final XiaomiProto.CustomVibrationPattern p : existing.getCustomVibrationPatternList()) {
            if (!coordinator.isProtectedVibrationPatternId(p.getId()) && p.getId() >= nextId) {
                nextId = p.getId() + 1;
            }
        }

        final XiaomiProto.CustomVibrationPattern requested = parseAddPattern(prefs().getString(PREF_ADD_PATTERN, null));
        if (requested == null) {
            LOG.warn("No vibration pattern to add");
            return;
        }
        final XiaomiProto.CustomVibrationPattern pattern = requested.toBuilder().setId(nextId).build();

        LOG.info("[vibration-add] sending ADD for pattern (id={})", nextId);
        support.sendCommand(
                "add vibration pattern",
                XiaomiProto.Command.newBuilder()
                        .setType(XiaomiSystemService.COMMAND_TYPE)
                        .setSubtype(CMD_ADD)
                        .setSystem(XiaomiProto.System.newBuilder().setVibrationPatternCreate(pattern))
                        .build()
        );
    }

    private void removePattern() {
        final int removeId = prefs().getInt(PREF_REMOVE_ID, -1);
        if (removeId < 0) {
            LOG.warn("No vibration pattern id to remove");
            return;
        }
        if (support.getCoordinator().isProtectedVibrationPatternId(removeId)) {
            LOG.warn("Refusing to remove protected vibration pattern id {}", removeId);
            return;
        }

        LOG.info("[vibration-remove] sending REMOVE for id {}", removeId);
        support.sendCommand(
                "remove vibration pattern",
                XiaomiProto.Command.newBuilder()
                        .setType(XiaomiSystemService.COMMAND_TYPE)
                        .setSubtype(CMD_REMOVE)
                        .setSystem(XiaomiProto.System.newBuilder().setVibrationRemove(
                                XiaomiProto.VibrationRemove.newBuilder().addId(removeId)))
                        .build()
        );
    }

    private DevicePrefs prefs() {
        return GBApplication.getDevicePrefs(support.getDevice());
    }

    public static XiaomiProto.VibrationPatterns parseVibrationPatterns(final String hex) {
        final XiaomiProto.VibrationPatterns parsed = parseProtoHex(hex, XiaomiProto.VibrationPatterns.parser());
        return parsed != null ? parsed : XiaomiProto.VibrationPatterns.getDefaultInstance();
    }

    private static XiaomiProto.CustomVibrationPattern parseAddPattern(final String hex) {
        return parseProtoHex(hex, XiaomiProto.CustomVibrationPattern.parser());
    }

    private static <T extends MessageLite> T parseProtoHex(final String hex, final Parser<T> parser) {
        if (hex != null) {
            try {
                return parser.parseFrom(GB.hexStringToByteArray(hex));
            } catch (final InvalidProtocolBufferException e) {
                LOG.warn("failed to parse vibration proto hex", e);
            }
        }
        return null;
    }
}
