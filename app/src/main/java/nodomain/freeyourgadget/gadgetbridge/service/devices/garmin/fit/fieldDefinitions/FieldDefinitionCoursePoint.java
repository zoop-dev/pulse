/*  Copyright (C) 2025 Thomas Kuehne

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionCoursePoint extends FieldDefinition {

    public FieldDefinitionCoursePoint(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        super(localNumber, size, baseType, name, scale, offset);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        final Object rawObj = baseType.decode(byteBuffer, scale, offset);
        if (rawObj != null) {
            final Number raw = (Number) rawObj;
            return CoursePoint.fromId(raw.intValue());
        }
        return null;
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof CoursePoint coursePoint) {
            baseType.encode(byteBuffer, coursePoint.getId(), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum CoursePoint {
        GENERIC(0, "generic"),
        SUMMIT(1, "summit"),
        VALLEY(2, "valley"),
        WATER(3, "water"),
        FOOD(4, "food"),
        DANGER(5, "danger"),
        LEFT(6, "left"),
        RIGHT(7, "right"),
        STRAIGHT(8, "straight"),
        FIRST_AID(9, "first_aid"),
        FOURTH_CATEGORY(10, "fourth_category"),
        THIRD_CATEGORY(11, "third_category"),
        SECOND_CATEGORY(12, "second_category"),
        FIRST_CATEGORY(13, "first_category"),
        HORS_CATEGORY(14, "hors_category"),
        SPRINT(15, "sprint"),
        LEFT_FORK(16, "left_fork"),
        RIGHT_FORK(17, "right_fork"),
        MIDDLE_FORK(18, "middle_fork"),
        SLIGHT_LEFT(19, "slight_left"),
        SHARP_LEFT(20, "sharp_left"),
        SLIGHT_RIGHT(21, "slight_right"),
        SHARP_RIGHT(22, "sharp_right"),
        U_TURN(23, "u_turn"),
        SEGMENT_START(24, "segment_start"),
        SEGMENT_END(25, "segment_end"),
        CAMPSITE(27, "campsite"),
        AID_STATION(28, "aid_station"),
        REST_AREA(29, "rest_area"),
        GENERAL_DISTANCE(30, "general_distance"),
        SERVICE(31, "service"),
        ENERGY_GEL(32, "energy_gel"),
        SPORTS_DRINK(33, "sports_drink"),
        MILE_MARKER(34, "mile_marker"),
        CHECKPOINT(35, "checkpoint"),
        SHELTER(36, "shelter"),
        MEETING_SPOT(37, "meeting_spot"),
        OVERLOOK(38, "overlook"),
        TOILET(39, "toilet"),
        SHOWER(40, "shower"),
        GEAR(41, "gear"),
        SHARP_CURVE(42, "sharp_curve"),
        STEEP_INCLINE(43, "steep_incline"),
        TUNNEL(44, "tunnel"),
        BRIDGE(45, "bridge"),
        OBSTACLE(46, "obstacle"),
        CROSSING(47, "crossing"),
        STORE(48, "store"),
        TRANSITION(49, "transition"),
        NAVAID(50, "navaid"),
        TRANSPORT(51, "transport"),
        ALERT(52, "alert"),
        INFO(53, "info");

        private final int id;

        @NonNull
        private final String name;

        CoursePoint(int id, @NonNull String name) {
            this.id = id;
            this.name = name;
        }

        @Nullable
        public static CoursePoint fromId(int id) {
            for (CoursePoint symbol : values()) {
                if (id == symbol.id) {
                    return symbol;
                }
            }
            return null;
        }

        @Nullable
        public static CoursePoint fromName(String name) {
            for (CoursePoint symbol : values()) {
                if (symbol.name.equals(name)) {
                    return symbol;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }
}
