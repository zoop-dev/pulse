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

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

public abstract class HeartRateZones {

    public enum CalculationMethod {
        MHR,
        HRR,
        LTHR,
    }

    public static String methodToString(Context context, CalculationMethod method) {
        return switch (method) {
            case MHR -> context.getString(R.string.hr_settings_zones_calculation_method_mhr);
            case HRR -> context.getString(R.string.hr_settings_zones_calculation_method_hrr);
            case LTHR -> context.getString(R.string.hr_settings_zones_calculation_method_lthr);
        };
    }

    protected final CalculationMethod method;

    protected int HRThreshold;
    protected int HRResting;

    protected int zone5;
    protected int zone4;
    protected int zone3;
    protected int zone2;
    protected int zone1;

    public HeartRateZones(CalculationMethod method, int HRThreshold) {
        this.method = method;
        this.HRThreshold = HRThreshold;
        this.HRResting = HeartRateZonesUtils.DEFAULT_REST_HEART_RATE;
        reset();
    }

    public abstract void reset();

    public abstract long getPercentage(int zone);

    public CalculationMethod getMethod() {
        return method;
    }

    public int getHRThreshold() {
        return HRThreshold;
    }

    public void setHRThreshold(int HRThreshold) {
        this.HRThreshold = HRThreshold;
    }

    public int getHRResting() {
        return HRResting;
    }

    public void setHRResting(int HRResting) {
        this.HRResting = HRResting;
    }

    public int getZone5() {
        return zone5;
    }

    public void setZone5(int zone5) {
        this.zone5 = zone5;
    }

    public int getZone4() {
        return zone4;
    }

    public void setZone4(int zone4) {
        this.zone4 = zone4;
    }

    public int getZone3() {
        return zone3;
    }

    public void setZone3(int zone3) {
        this.zone3 = zone3;
    }

    public int getZone2() {
        return zone2;
    }

    public void setZone2(int zone2) {
        this.zone2 = zone2;
    }

    public int getZone1() {
        return zone1;
    }

    public void setZone1(int zone1) {
        this.zone1 = zone1;
    }



    public boolean isValid() {
        return HeartRateZonesUtils.checkValue(this.HRThreshold) &&
                HeartRateZonesUtils.checkValue(this.HRResting) &&
                HeartRateZonesUtils.checkValue(this.zone1) &&
                HeartRateZonesUtils.checkValue(this.zone2) &&
                HeartRateZonesUtils.checkValue(this.zone3) &&
                HeartRateZonesUtils.checkValue(this.zone4) &&
                HeartRateZonesUtils.checkValue(this.zone5) &&
                this.zone1 <= this.zone2 &&
                this.zone2 <= this.zone3 &&
                this.zone3 <= this.zone4 &&
                this.zone4 <= this.zone5;
    }

    public boolean hasValidData() {
        boolean valid = this.zone1 > 0 && this.zone2 > 0 && this.zone3 > 0 && this.zone4 > 0 && this.zone5 > 0 && this.HRThreshold > 0;
        if (method == CalculationMethod.HRR) {
            valid &= this.HRResting >0;
        }
        return valid;
    }
}
