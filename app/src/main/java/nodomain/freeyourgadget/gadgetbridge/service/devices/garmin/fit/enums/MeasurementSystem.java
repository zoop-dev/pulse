package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

public enum MeasurementSystem {
    metric(0),
    imperial(1),
    nautical(2);

    public final int id;

    MeasurementSystem(final int i) {
        id = i;
    }
}
