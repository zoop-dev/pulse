package nodomain.freeyourgadget.gadgetbridge.activities.charts.sleep;


public abstract class AbstractOverlayData {

    private final float yMin;
    private final float yMax;
    private final float yDelta;

    public AbstractOverlayData(float yMin, float yMax) {
        this.yMin = yMin;
        this.yMax = yMax;
        this.yDelta = yMax - yMin;
    }

    abstract boolean isData(int i);

    abstract boolean isMeasured(int i);

    abstract int getMainColor();

    abstract int getAverageColor();

    public float getYDelta() {
        return yDelta;
    }

    abstract int getLength();

    protected float getAdjustedValueInternal(float val) {
        if(val < yMin) {
            return 0;
        }
        if(val > yMax) {
            return yDelta;
        }
        return val - yMin;
    }

    abstract float getAdjustedValue(int i);

    abstract boolean hasAverage();

    abstract float getAdjustedAverageValue();

    abstract String getCurrentValue(int i);

    abstract float adjustYLabelDelta(float delta);

    abstract String getYLabelValue(float val);

    String[] getYLabels(int horizontalLineCount) {
        float yLabelsDelta = adjustYLabelDelta(yDelta / horizontalLineCount);
        String[] res = new String[horizontalLineCount + 1];
        float yStart = yMax;
        for (int i = 0; i <= horizontalLineCount; i++) {
            res[i] = getYLabelValue(yStart);
            yStart -= yLabelsDelta;
        }
        return res;
    }
}
