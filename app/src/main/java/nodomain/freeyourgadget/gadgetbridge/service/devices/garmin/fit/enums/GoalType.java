package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum GoalType {
    time(0),
    distance(1),
    calories(2),
    frequency(3),
    steps(4),
    ascent(5),
    active_minutes(6),
    hydration(7),
    weight(8),
    ;

    public final int id;

    GoalType(final int i) {
        id = i;
    }
}
