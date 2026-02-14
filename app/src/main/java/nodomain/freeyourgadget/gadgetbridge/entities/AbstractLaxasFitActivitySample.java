/*  Copyright (C) 2021-2024 Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.entities;

public abstract class AbstractLaxasFitActivitySample extends AbstractActivitySample {
    private static int totalSteps;
    private static int totalDistance_m;
    private static int totalCalories;

    public int getTotalDistance_m() {
        return totalDistance_m;
    }

    public void setTotalDistance_m(int totalDistance_m) {
        int diff = totalDistance_m - getTotalDistance_m();
        if(diff < 0) diff = totalDistance_m;
        setDistanceCm(diff*100);
        AbstractLaxasFitActivitySample.totalDistance_m = totalDistance_m;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        int diff = totalSteps - getTotalSteps();
        if(diff < 0) diff = totalSteps;
        setSteps(diff);
        AbstractLaxasFitActivitySample.totalSteps = totalSteps;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        int diff = totalCalories - getTotalCalories();
        if(diff<0) diff = totalCalories;
        setActiveCalories(diff);
        AbstractLaxasFitActivitySample.totalCalories = totalCalories;
    }
}
