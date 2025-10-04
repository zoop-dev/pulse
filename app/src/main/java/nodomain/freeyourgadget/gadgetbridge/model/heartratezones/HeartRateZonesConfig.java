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
package nodomain.freeyourgadget.gadgetbridge.model.heartratezones;

import java.util.List;

public class HeartRateZonesConfig {

    private final HeartRateZonesSpec.PostureType type;
    // warnings
    private boolean warningEnable;
    private int warningHRLimit;

    private HeartRateZones.CalculationMethod currentCalculationMethod;


    private final List<HeartRateZones> configByMethods;


    public HeartRateZonesConfig(HeartRateZonesSpec.PostureType type, boolean warningEnable, int warningHRLimit, HeartRateZones.CalculationMethod currentCalculationMethod, List<HeartRateZones> configByMethods) {
        this.type = type;
        this.warningEnable = warningEnable;
        this.warningHRLimit = warningHRLimit;
        this.currentCalculationMethod = currentCalculationMethod;
        this.configByMethods = configByMethods;
    }

    public HeartRateZonesSpec.PostureType getType() {
        return type;
    }

    public boolean getWarningEnable() {
        return warningEnable;
    }

    public void setWarningEnable(boolean warningEnable) {
        this.warningEnable = warningEnable;
    }

    public int getWarningHRLimit() {
        return warningHRLimit;
    }

    public void setWarningHRLimit(int warningHRLimit) {
        this.warningHRLimit = warningHRLimit;
    }

    public HeartRateZones.CalculationMethod getCurrentCalculationMethod() {
        return currentCalculationMethod;
    }

    public void setCurrentCalculationMethod(HeartRateZones.CalculationMethod currentCalculationMethod) {
        this.currentCalculationMethod = currentCalculationMethod;
    }

    public List<HeartRateZones> getConfigByMethods() {
        return configByMethods;
    }

    public void reset() {
        for(HeartRateZones zn: configByMethods) {
            zn.reset();
        }
    }

    public boolean isValid() {
        boolean valid = HeartRateZonesUtils.checkValue(this.warningHRLimit) && this.warningHRLimit > 0 && !configByMethods.isEmpty();
        for (HeartRateZones cfg : configByMethods) {
            valid &= cfg.isValid();
        }
        return valid;
    }
}
