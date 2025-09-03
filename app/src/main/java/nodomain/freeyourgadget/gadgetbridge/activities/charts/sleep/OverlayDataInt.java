package nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep;

import java.util.Locale;

public class OverlayDataInt extends AbstractOverlayData{

    public static int NO_DATA = Integer.MIN_VALUE;

    private final int average;


    private final int[] data;

    private final int mainColor;
    private final int averageColor;


    public OverlayDataInt(int yMin, int yMax, int[] data, int average, int mainColor, int averageColor) {
        super(yMin, yMax);
        this.average = average;
        this.data = data;
        this.mainColor = mainColor;
        this.averageColor = averageColor;
    }

    @Override
    public boolean isData(int i) {
        if(data == null || i < 0 || i > data.length) {
            return false;
        }
        return this.data[i] != NO_DATA;
    }

    @Override
    public boolean isMeasured(int i) {
        if(data == null || i < 0 || i > data.length) {
            return false;
        }
        return this.data[i] >= 0;
    }

    @Override
    public int getMainColor() {
        return mainColor;
    }

    @Override
    public int getAverageColor() {
        return averageColor;
    }

    @Override
    public int getLength() {
        if(data == null) {
            return 0;
        }
        return data.length;
    }

    @Override
    public float getAdjustedValue(int i) {
        if(data == null || i < 0 || i > data.length) {
            return 0;
        }
        return getAdjustedValueInternal(this.data[i]);
    }

    @Override
    public boolean hasAverage() {
        return average != NO_DATA;
    }

    @Override
    public float getAdjustedAverageValue() {
        return getAdjustedValueInternal(average);
    }

    @Override
    public String getCurrentValue(int i) {
        return String.format(Locale.ROOT, "%d", this.data[i]);
    }

    @Override
    public float adjustYLabelDelta(float delta) {
        return (int)delta;
    }

    @Override
    public String getYLabelValue(float val) {
        return String.format(Locale.ROOT, "%d", (int)val);
    }
}
