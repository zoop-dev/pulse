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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.VibrationPatternData;
import nodomain.freeyourgadget.gadgetbridge.devices.VibrationPatternDataSource;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.services.XiaomiVibrationManager;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiVibrationDataSource implements VibrationPatternDataSource {
    private final Context context;
    private final GBDevice device;
    private final DeviceCoordinator coordinator;
    private final SharedPreferences devicePrefs;
    private List<Integer> selectableTypeIds;

    public XiaomiVibrationDataSource(final Context context, final GBDevice device) {
        this.context = context;
        this.device = device;
        this.coordinator = device.getDeviceCoordinator();
        this.devicePrefs = GBApplication.getDeviceSpecificSharedPrefs(device.getAddress());
    }

    @Override
    public VibrationPatternData loadData() {
        final XiaomiProto.VibrationPatterns patterns = loadRaw();

        final Map<Integer, String> customByName = new HashMap<>();
        for (final XiaomiProto.CustomVibrationPattern p : patterns.getCustomVibrationPatternList()) {
            customByName.put(p.getId(), patternName(p.getId(), p.getName()));
        }

        final Set<Integer> usedIds = new HashSet<>();

        final List<VibrationPatternData.TypeEntry> types = new ArrayList<>();
        for (final XiaomiProto.VibrationNotificationType t : patterns.getNotificationTypeList()) {
            usedIds.add(t.getPreset());
            types.add(new VibrationPatternData.TypeEntry(
                    t.getNotificationType(),
                    getNotificationTypeName(t.getNotificationType()),
                    t.getPreset(),
                    getPatternName(t.getPreset(), customByName)
            ));
        }

        final List<VibrationPatternData.PatternEntry> patternsList = new ArrayList<>();
        for (final XiaomiProto.CustomVibrationPattern p : patterns.getCustomVibrationPatternList()) {
            final boolean canDelete = !coordinator.isProtectedVibrationPatternId(p.getId())
                    && !usedIds.contains(p.getId());

            final List<VibrationPatternData.Segment> segs = new ArrayList<>();
            for (final XiaomiProto.Vibration v : p.getVibrationList()) {
                segs.add(new VibrationPatternData.Segment(
                        v.getVibrate() != 0, v.getMs(), v.getStrength()));
            }

            patternsList.add(new VibrationPatternData.PatternEntry(
                    p.getId(),
                    patternName(p.getId(), p.getName()),
                    p.getType(),
                    getNotificationTypeName(p.getType()),
                    segs,
                    canDelete
            ));
        }

        return new VibrationPatternData(types, patternsList);
    }

    @Override
    public List<Integer> getSelectableNotificationTypeIds() {
        if (selectableTypeIds == null) {
            selectableTypeIds = new ArrayList<>();
            for (final XiaomiProto.VibrationType type : XiaomiProto.VibrationType.values()) {
                if (type.getNumber() > 0) {
                    selectableTypeIds.add(type.getNumber());
                }
            }
        }
        return selectableTypeIds;
    }

    @Override
    public String getNotificationTypeName(final int typeId) {
        final XiaomiProto.VibrationType type = XiaomiProto.VibrationType.forNumber(typeId);
        if (type != null) {
            switch (type) {
                case VIBRATION_TYPE_CALL:
                    return context.getString(R.string.xiaomi_vibration_type_call);
                case VIBRATION_TYPE_TASK:
                    return context.getString(R.string.xiaomi_vibration_type_task);
                case VIBRATION_TYPE_EVENT:
                    return context.getString(R.string.xiaomi_vibration_type_event);
                case VIBRATION_TYPE_ALARM:
                    return context.getString(R.string.xiaomi_vibration_type_alarm);
                case VIBRATION_TYPE_NOTIFICATION:
                    return context.getString(R.string.xiaomi_vibration_type_notification);
                case VIBRATION_TYPE_STANDING:
                    return context.getString(R.string.xiaomi_vibration_type_standing);
                case VIBRATION_TYPE_SMS:
                    return context.getString(R.string.xiaomi_vibration_type_sms);
                case VIBRATION_TYPE_GOAL:
                    return context.getString(R.string.xiaomi_vibration_type_goal);
                default:
                    break;
            }
        }
        return context.getString(R.string.xiaomi_vibration_type_unknown, typeId);
    }

    @Override
    public void requestRefresh() {
        GBApplication.deviceService(device).onSendConfiguration(XiaomiVibrationManager.PREF_REFRESH);
    }

    @Override
    public void addPattern(final String name, final int notificationTypeId,
                           final List<VibrationPatternData.Segment> segments) {
        final XiaomiProto.CustomVibrationPattern.Builder pb = XiaomiProto.CustomVibrationPattern.newBuilder()
                .setName(name != null ? name : "")
                .setType(notificationTypeId);
        for (final VibrationPatternData.Segment seg : segments) {
            final XiaomiProto.Vibration.Builder vb = XiaomiProto.Vibration.newBuilder()
                    .setVibrate(seg.on ? 1 : 0)
                    .setMs(seg.durationMs);
            if (seg.on && seg.strengthPercent > 0) {
                vb.setStrength(seg.strengthPercent);
            }
            pb.addVibration(vb);
        }

        devicePrefs.edit()
                .putString(XiaomiVibrationManager.PREF_ADD_PATTERN, GB.hexdump(pb.build().toByteArray()))
                .apply();

        GBApplication.deviceService(device).onSendConfiguration(XiaomiVibrationManager.PREF_ADD);
        GBApplication.deviceService(device).onSendConfiguration(XiaomiVibrationManager.PREF_REFRESH);
    }

    @Override
    public void deletePattern(final int id) {
        devicePrefs.edit()
                .putInt(XiaomiVibrationManager.PREF_REMOVE_ID, id)
                .apply();
        GBApplication.deviceService(device).onSendConfiguration(XiaomiVibrationManager.PREF_REMOVE);
        GBApplication.deviceService(device).onSendConfiguration(XiaomiVibrationManager.PREF_REFRESH);
    }

    private XiaomiProto.VibrationPatterns loadRaw() {
        return XiaomiVibrationManager.parseVibrationPatterns(
                devicePrefs.getString(XiaomiVibrationManager.PREF_PATTERNS, null));
    }

    private String patternName(final int id, final String name) {
        return name == null || name.isEmpty()
                ? context.getString(R.string.xiaomi_vibration_pattern_unnamed, id)
                : name;
    }

    private String getPatternName(final int patternId, final Map<Integer, String> customNames) {
        if (patternId == 0) {
            return context.getString(R.string.xiaomi_vibration_default);
        }
        final String custom = customNames.get(patternId);
        if (custom != null) {
            return custom;
        }
        final int res = coordinator.getVibrationPresetNameRes(patternId);
        if (res != 0) {
            return context.getString(res);
        }
        return context.getString(R.string.xiaomi_vibration_preset, patternId);
    }
}
