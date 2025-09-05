/*  Copyright (C) 2025 Me7c7

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
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
