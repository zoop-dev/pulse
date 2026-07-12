package nodomain.freeyourgadget.gadgetbridge.devices;


public enum SleepAsAndroidFeature {
    HEART_RATE,
    ALARMS,
    NOTIFICATIONS,
    ACCELEROMETER,
    OXIMETRY,
    SPO2,
    // Device only gets SPO2 via periodic fetching of recorded samples, not live readings.
    // Should be reported alongside SPO2.
    SPO2_AUTOFETCH
}
