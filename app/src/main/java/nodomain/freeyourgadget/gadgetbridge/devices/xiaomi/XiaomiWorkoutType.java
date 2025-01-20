/*  Copyright (C) 2023-2024 José Rebelo

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

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class XiaomiWorkoutType {
    private final int code;
    private final String name;

    public XiaomiWorkoutType(final int code, final String name) {
        this.code = code;
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static ActivityKind fromCode(final int code) {
        switch (code) {
            case 1:
                return ActivityKind.OUTDOOR_RUNNING;
            case 2:
                return ActivityKind.WALKING;
            case 3:
                return ActivityKind.HIKING;
            case 4:
                return ActivityKind.TREKKING;
            case 5:
                return ActivityKind.TRAIL_RUN;
            case 6:
                return ActivityKind.OUTDOOR_CYCLING;
            case 7:   // indoor cycling   0x0007
                return ActivityKind.INDOOR_CYCLING;
            case 8:   // freestyle        0x0008
                return ActivityKind.FREE_TRAINING;
            case 12:  // yoga             0x000c
                return ActivityKind.YOGA;
            case 15:
                return ActivityKind.OUTDOOR_WALKING;
            case 16:  // HIIT             0x0010
                return ActivityKind.HIIT;
            case 201: // skateboard       0x00c9
                return ActivityKind.SKATEBOARDING;
            case 202: // roller skating   0x00ca
                return ActivityKind.ROLLER_SKATING;
            case 301: // stair climbing   0x012d
                return ActivityKind.STAIRS;
            case 303: // core training    0x012f
                return ActivityKind.CORE_TRAINING;
            case 304: // flexibility      0x0130
                return ActivityKind.FLEXIBILITY;
            case 305: // pilates          0x0131
                return ActivityKind.PILATES;
            case 307: // stretching       0x0133
                return ActivityKind.STRETCHING;
            case 308: // strength         0x0134
                return ActivityKind.STRENGTH_TRAINING;
            case 310: // aerobics         0x0136
                return ActivityKind.AEROBICS;
            case 311: // physical training
                return ActivityKind.PHYSICAL_TRAINING;
            case 313: // dumbbell
                return ActivityKind.DUMBBELL;
            case 314: // barbell
                return ActivityKind.BARBELL;
            case 318: // sit-ups
                return ActivityKind.SIT_UPS;
            case 320: // upper body       0x0140
                return ActivityKind.UPPER_BODY;
            case 321: // lower body       0x0141
                return ActivityKind.LOWER_BODY;
            case 399: // indoor-Fitness   0x018f
                return ActivityKind.INDOOR_FITNESS;
            case 499: // dancing          0x01f3
                return ActivityKind.DANCE;
            case 501: // Wrestling
                return ActivityKind.WRESTLING;
            case 600: // Soccer           0x0258
                return ActivityKind.SOCCER;
            case 601: // basketball       0x0259
                return ActivityKind.BASKETBALL;
            case 607: // table tennis     0x025f
                return ActivityKind.TABLE_TENNIS;
            case 608: // badminton        0x0260
                return ActivityKind.BADMINTON;
            case 609: // tennis           0x0261
                return ActivityKind.TENNIS;
            case 614: // billiard          0x0266
                return ActivityKind.BILLIARDS;
            case 619: // golf             0x026b
                return ActivityKind.GOLF;
            case 700: // ice skating      0x02bc
                return ActivityKind.ICE_SKATING;
            case 708: // snowboard        0x02c4
                return ActivityKind.SNOWBOARDING;
            case 709: // skiing           0x02c5
                return ActivityKind.SKIING;
            case 808: // shuttlecock      0x0328
                return ActivityKind.SHUTTLECOCK;
        }

        return ActivityKind.UNKNOWN;
    }

    @StringRes
    public static int mapWorkoutName(final int code) {
        final ActivityKind activityKind = fromCode(code);
        if (activityKind != ActivityKind.UNKNOWN) {
            return activityKind.getLabel();
        }
        return -1;
    }

    public static Collection<XiaomiWorkoutType> getWorkoutTypesSupportedByDevice(final GBDevice device) {
        final Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
        final List<String> codes = prefs.getList(XiaomiPreferences.PREF_WORKOUT_TYPES, Collections.emptyList());
        final List<XiaomiWorkoutType> ret = new ArrayList<>(codes.size());

        for (final String code : codes) {
            final int codeInt = Integer.parseInt(code);
            final int codeNameStringRes = XiaomiWorkoutType.mapWorkoutName(codeInt);
            ret.add(new XiaomiWorkoutType(
                    codeInt,
                    codeNameStringRes != -1 ?
                            GBApplication.getContext().getString(codeNameStringRes) :
                            GBApplication.getContext().getString(R.string.widget_unknown_workout, code)
            ));
        }

        return ret;
    }
}
