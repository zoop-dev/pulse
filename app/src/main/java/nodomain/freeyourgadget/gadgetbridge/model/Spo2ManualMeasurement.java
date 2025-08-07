package nodomain.freeyourgadget.gadgetbridge.model;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Spo2ManualMeasurement {
    private long timestamp;
    private int value;

    public Spo2ManualMeasurement(long timestamp, int value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getTime() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date(timestamp);
        return simpleDateFormat.format(date);
    }

    public String getValue() {
        return String.valueOf(value);
    }
}
