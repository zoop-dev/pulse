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
            case 9: // "Pool swimming"
                return ActivityKind.POOL_SWIM;
            case 10: // "Open Water"
                return ActivityKind.SWIMMING_OPENWATER;
            case 11: // "Elliptical"
                return ActivityKind.ELLIPTICAL_TRAINER;
            case 12:  // yoga             0x000c
                return ActivityKind.YOGA;
            case 13: // "Rower"
                return ActivityKind.ROWING_MACHINE;
            case 14: // "Jump rope"
                return ActivityKind.JUMP_ROPING;
            case 15:
                return ActivityKind.OUTDOOR_WALKING;
            case 16:  // HIIT             0x0010
                return ActivityKind.HIIT;
            case 17: // "Triathlon"
                return ActivityKind.TRIATHLON;
            case 100: // "Sailing"
                return ActivityKind.SAILING;
            case 101: // "Paddleboarding"
                return ActivityKind.STAND_UP_PADDLEBOARDING;
            case 102: // "Water polo"
                return ActivityKind.WATER_POLO;
            case 103: // "Other water sports"
                return ActivityKind.OTHER_WATER_SPORTS;
            case 104: // "Water skiing"
                return ActivityKind.WATER_SKIING;
            case 105: // "Kayaking"
                return ActivityKind.KAYAKING;
            case 106: // "Rafting"
                return ActivityKind.RAFTING;
            case 107: // "Rowing"
                return ActivityKind.ROWING;
            case 108: // "Powerboating"
                return ActivityKind.POWERBOATING;
            case 109: // "Finswimming"
                return ActivityKind.FINSWIMMING;
            case 110: // "Diving"
                return ActivityKind.DIVING;
            case 111: // "Artistic swimming"
                return ActivityKind.ARTISTIC_SWIMMING;
            case 112: // "Snorkeling"
                return ActivityKind.SNORKELING;
            case 113: // "Kitesurfing"
                return ActivityKind.KITESURFING;
            case 114: // "Flowriding"
                return ActivityKind.FLOWRIDING;
            case 115: // "Dragon Boat"
                return ActivityKind.DRAGON_BOAT;
            case 200: // "Rock climbing"
                return ActivityKind.ROCK_CLIMBING;
            case 201: // skateboard       0x00c9
                return ActivityKind.SKATEBOARDING;
            case 202: // roller skating   0x00ca
                return ActivityKind.ROLLER_SKATING;
            case 203: // "Parkour"
                return ActivityKind.PARKOUR;
            case 204: // "ATV"
                return ActivityKind.ATV;
            case 205: // "Paragliding"
                return ActivityKind.PARAGLIDING;
            case 206: // "BMX"
                return ActivityKind.BMX;
            case 207: // "Race Walk"
                return ActivityKind.RACE_WALKING;
            case 300: // "Stair climber"
                return ActivityKind.STAIR_CLIMBER;
            case 301: // stair climbing   0x012d
                return ActivityKind.STAIRS;
            case 302: // "Stepper"
                return ActivityKind.STEPPER;
            case 303: // core training    0x012f
                return ActivityKind.CORE_TRAINING;
            case 304: // flexibility      0x0130
                return ActivityKind.FLEXIBILITY;
            case 305: // pilates          0x0131
                return ActivityKind.PILATES;
            case 306: // "Gymnastics"
                return ActivityKind.GYMNASTICS;
            case 307: // stretching       0x0133
                return ActivityKind.STRETCHING;
            case 308: // strength         0x0134
                return ActivityKind.STRENGTH_TRAINING;
            case 309: // "Cross training"
                return ActivityKind.CROSS_TRAINING;
            case 310: // aerobics         0x0136
                return ActivityKind.AEROBICS;
            case 311: // physical training
                return ActivityKind.PHYSICAL_TRAINING;
            case 312: // "Wall ball"
                return ActivityKind.WALL_BALL;
            case 313: // dumbbell
                return ActivityKind.DUMBBELL;
            case 314: // barbell
                return ActivityKind.BARBELL;
            case 315: // "Weightlifting"
                return ActivityKind.WEIGHTLIFTING;
            case 316: // "Deadlift"
                return ActivityKind.DEADLIFT;
            case 317: // "Burpee"
                return ActivityKind.BURPEE;
            case 318: // sit-ups
                return ActivityKind.SIT_UPS;
            case 319: // "Functional training"
                return ActivityKind.FUNCTIONAL_TRAINING;
            case 320: // upper body       0x0140
                return ActivityKind.UPPER_BODY;
            case 321: // lower body       0x0141
                return ActivityKind.LOWER_BODY;
            case 322: // "Abs"
                return ActivityKind.ABS;
            case 323: // "Back"
                return ActivityKind.BACK;
            case 324: // "Spinning"
                return ActivityKind.SPINNING;
            case 325: // "Air walker"
                return ActivityKind.AIR_WALKER;
            case 326: // "Step aerobics"
                return ActivityKind.STEP_AEROBICS;
            case 327: // "Horizontal bar"
                return ActivityKind.HORIZONTAL_BAR;
            case 328: // "Parallel bars"
                return ActivityKind.PARALLEL_BARS;
            case 329: // "Mass gymnastics"
                return ActivityKind.MASS_GYMNASTICS;
            case 330: // "Cardio combat"
                return ActivityKind.CARDIO_COMBAT;
            case 331: // "Battle rope"
                return ActivityKind.BATTLE_ROPE;
            case 332: // "Aerobic combo"
                return ActivityKind.AEROBIC_COMBO;
            case 333: // "Indoor walking"
                return ActivityKind.INDOOR_WALKING;
            case 399: // indoor-Fitness   0x018f
                return ActivityKind.INDOOR_FITNESS;
            case 400: // "Square dancing"
                return ActivityKind.SQUARE_DANCE;
            case 401: // "Belly dance"
                return ActivityKind.BELLY_DANCE;
            case 402: // "Ballet"
                return ActivityKind.BALLET;
            case 403: // "Street dance"
                return ActivityKind.STREET_DANCE;
            case 404: // "Zumba"
                return ActivityKind.ZUMBA;
            case 405: // "Folk dance"
                return ActivityKind.FOLK_DANCE;
            case 406: // "Jazz dance"
                return ActivityKind.JAZZ_DANCE;
            case 407: // "Latin dance"
                return ActivityKind.LATIN_DANCE;
            case 408: // "Hip hop"
                return ActivityKind.HIP_HOP;
            case 409: // "Pole dance"
                return ActivityKind.POLE_DANCE;
            case 410: // "Breaking"
                return ActivityKind.BREAKING;
            case 411: // "Ballroom dance"
                return ActivityKind.BALLROOM_DANCE;
            case 412: // "Modern dance"
                return ActivityKind.MODERN_DANCE;
            case 499: // dancing          0x01f3
                return ActivityKind.DANCE;
            case 500: // "Boxing"
                return ActivityKind.BOXING;
            case 501: // Wrestling
                return ActivityKind.WRESTLING;
            case 502: // "Martial arts"
                return ActivityKind.MARTIAL_ARTS;
            case 503: // "Tai chi"
                return ActivityKind.TAI_CHI;
            case 504: // "Muay Thai"
                return ActivityKind.MUAY_THAI;
            case 505: // "Judo"
                return ActivityKind.JUDO;
            case 506: // "Taekwondo"
                return ActivityKind.TAEKWONDO;
            case 507: // "Karate"
                return ActivityKind.KARATE;
            case 508: // "Kickboxing"
                return ActivityKind.KICKBOXING;
            case 509: // "Kendo"
                return ActivityKind.KENDO;
            case 510: // "Fencing"
                return ActivityKind.FENCING;
            case 511: // "Jujitsu"
                return ActivityKind.JUJITSU;
            case 600: // Soccer           0x0258
                return ActivityKind.SOCCER;
            case 601: // basketball       0x0259
                return ActivityKind.BASKETBALL;
            case 602: // "Volleyball"
                return ActivityKind.VOLLEYBALL;
            case 603: // "Baseball"
                return ActivityKind.BASEBALL;
            case 604: // "Softball"
                return ActivityKind.SOFTBALL;
            case 605: // "American football"
                return ActivityKind.AMERICAN_FOOTBALL;
            case 606: // "Hockey"
                return ActivityKind.HOCKEY;
            case 607: // table tennis     0x025f
                return ActivityKind.TABLE_TENNIS;
            case 608: // badminton        0x0260
                return ActivityKind.BADMINTON;
            case 609: // tennis           0x0261
                return ActivityKind.TENNIS;
            case 610: // "Cricket"
                return ActivityKind.CRICKET;
            case 611: // "Handball"
                return ActivityKind.HANDBALL;
            case 612: // "Bowling"
                return ActivityKind.BOWLING;
            case 613: // "Squash"
                return ActivityKind.SQUASH;
            case 614: // billiard          0x0266
                return ActivityKind.BILLIARDS;
            case 615: // "Shuttlecock"
                return ActivityKind.SHUTTLECOCK;
            case 616: // "Beach soccer"
                return ActivityKind.BEACH_SOCCER;
            case 617: // "Beach volleyball"
                return ActivityKind.BEACH_VOLLEYBALL;
            case 618: // "Sepak takraw"
                return ActivityKind.SEPAK_TAKRAW;
            case 619: // golf             0x026b
                return ActivityKind.GOLF;
            case 620: // "Table football"
                return ActivityKind.TABLE_FOOTBALL;
            case 621: // "Futsal"
                return ActivityKind.FUTSAL;
            case 622: // "Hacky sack"
                return ActivityKind.HACKY_SACK;
            case 623: // "Bocce"
                return ActivityKind.BOCCE;
            case 624: // "Jai alai"
                return ActivityKind.JAI_ALAI;
            case 625: // "Gateball"
                return ActivityKind.GATEBALL;
            case 626: // "Dodgeball"
                return ActivityKind.DODGEBALL;
            case 627: // "Shuffleboard"
                return ActivityKind.SHUFFLEBOARD;
            case 700: // ice skating      0x02bc
                return ActivityKind.ICE_SKATING;
            case 701: // "Curling"
                return ActivityKind.CURLING;
            case 702: // "Other winter sports"
                return ActivityKind.OTHER_WINTER_SPORTS;
            case 703: // "Snowmobile"
                return ActivityKind.SNOWMOBILING;
            case 704: // "Ice hockey"
                return ActivityKind.ICE_HOCKEY;
            case 705: // "Bobsleigh"
                return ActivityKind.BOBSLEIGH;
            case 706: // "Sledding"
                return ActivityKind.SLEDDING;
            case 707: // "Indoor ice skating"
                return ActivityKind.INDOOR_ICE_SKATING;
            case 708: // snowboard        0x02c4
                return ActivityKind.SNOWBOARDING;
            case 709: // skiing           0x02c5
                return ActivityKind.SKIING;
            case 800: // "Archery"
                return ActivityKind.ARCHERY;
            case 801: // "Darts"
                return ActivityKind.DARTS;
            case 802: // "Horse riding"
                return ActivityKind.HORSE_RIDING;
            case 803: // "Tug of war"
                return ActivityKind.TUG_OF_WAR;
            case 804: // "Hula hoop"
                return ActivityKind.HULA_HOOP;
            case 805: // "Kite flying"
                return ActivityKind.KITE_FLYING;
            case 806: // "Fishing"
                return ActivityKind.FISHING;
            case 807: // "Frisbee"
                return ActivityKind.FRISBEE;
            case 808: // shuttlecock      0x0328
                return ActivityKind.SHUTTLECOCK;
            case 809: // "Swinging"
                return ActivityKind.SWING;
            case 810: // "Somatosensory game"
                return ActivityKind.SOMATOSENSORY_GAME;
            case 811: // "Esports"
                return ActivityKind.ESPORTS;
            case 900: // "Chess"
                return ActivityKind.CHESS;
            case 901: // "Checkers"
                return ActivityKind.CHECKERS;
            case 902: // "Weiqi"
                return ActivityKind.WEIQI;
            case 903: // "Bridge"
                return ActivityKind.BRIDGE;
            case 904: // "Board game"
                return ActivityKind.BOARD_GAME;
            case 10000: // "Equestrian"
                return ActivityKind.EQUESTRIAN;
            case 10001: // "Athletics"
                return ActivityKind.ATHLETICS;
            case 10002: // "Auto racing"
                return ActivityKind.AUTO_RACING;
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
