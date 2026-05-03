package nodomain.freeyourgadget.gadgetbridge.model;

public enum DistanceUnit {
    METRIC,
    IMPERIAL;

    private static final double METER_TO_FEET = 3.28084;

    public static double meterToFeet(double meter){
        return meter * METER_TO_FEET;
    }

    public static double feetToMeter(double feet){
        return feet / METER_TO_FEET;
    }
}
