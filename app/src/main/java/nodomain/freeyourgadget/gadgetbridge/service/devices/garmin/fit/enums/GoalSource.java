package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum GoalSource {
    auto(0),
    community(1),
    manual(2),
    ;

    public final int id;

    GoalSource(final int i) {
        id = i;
    }
}
