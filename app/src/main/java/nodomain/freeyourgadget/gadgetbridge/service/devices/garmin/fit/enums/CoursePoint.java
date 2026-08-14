package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import androidx.annotation.NonNull;

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

    public final int id;

    @NonNull
    public final String name;

    CoursePoint(final int id, @NonNull final String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
