package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum AlarmLabel {
    NONE(0),
    WAKE_UP(1),
    WORKOUT(2),
    REMINDER(3),
    APPOINTMENT(4),
    TRAINING(5),
    CLASS(6),
    MEDITATE(7),
    BEDTIME(8),
    ;

    public final int id;

    AlarmLabel(final int i) {
        id = i;
    }
}
