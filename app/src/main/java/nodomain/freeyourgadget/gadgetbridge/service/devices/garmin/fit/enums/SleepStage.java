package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum SleepStage {
    UNMEASURABLE(0),
    AWAKE(1),
    LIGHT(2),
    DEEP(3),
    REM(4),
    ;

    public final int id;

    SleepStage(final int i) {
        id = i;
    }
}
