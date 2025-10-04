package nodomain.freeyourgadget.gadgetbridge.model.heartratezones;

public class HeartRateZonesUtils {
    public static final int MAXIMUM_HEART_RATE = 220;
    public static final int DEFAULT_REST_HEART_RATE = 60;

    public static boolean checkValue(int val) {
        return val >= 0 && val < MAXIMUM_HEART_RATE;
    }
}
