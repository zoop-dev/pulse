package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum HrvStatus {
    NONE(0),
    POOR(1),
    LOW(2),
    UNBALANCED(3),
    BALANCED(4),
    ;

    public final int id;

    HrvStatus(final int i) {
        id = i;
    }
}
