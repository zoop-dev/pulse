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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei;

import nodomain.freeyourgadget.gadgetbridge.model.heartratezones.HeartRateZones;

public class HuaweiHeartRateZones extends HeartRateZones {

    public HuaweiHeartRateZones(CalculationMethod method, int HRThreshold) {
        super(method, HRThreshold);
    }

    @Override
    public void reset() {
        switch(this.method) {
            case MHR -> calculateMHRZonesConfig();
            case HRR -> defaultHRRZonesConfig();
            case LTHR -> defaultLTHRZonesConfig();
        }
    }

    @Override
    public long getPercentage(int zone) {
        return switch(this.method) {
            case MHR, LTHR -> Math.round((float) zone * 100.0 / (float) HRThreshold);
            case HRR -> Math.round((float) (zone - HRResting) * 100.0 / (float) (HRThreshold - HRResting));
        };
    }

    private void calculateMHRZonesConfig() {
        zone5 = Math.round(((float) (HRThreshold * 90)) / 100.0f); // Extreme;
        zone4 = Math.round(((float) (HRThreshold * 80)) / 100.0f); // Anaerobic;
        zone3 = Math.round(((float) (HRThreshold * 70)) / 100.0f); // Aerobic;
        zone2 = Math.round(((float) (HRThreshold * 60)) / 100.0f); // FatBurning;
        zone1 = Math.round(((float) (HRThreshold * 50)) / 100.0f); // WarmUp;
    }

    private void defaultHRRZonesConfig() {
        int calcHR = HRThreshold - HRResting;
        zone5 = Math.round(((float) (calcHR * 95)) / 100.0f) + HRResting; // AdvancedAnaerobic
        zone4 = Math.round(((float) (calcHR * 88)) / 100.0f) + HRResting; // BasicAnaerobic
        zone3 = Math.round(((float) (calcHR * 84)) / 100.0f) + HRResting; // Lactate
        zone2 = Math.round(((float) (calcHR * 74)) / 100.0f) + HRResting; // AdvancedAerobic
        zone1 = Math.round(((float) (calcHR * 59)) / 100.0f) + HRResting; // BasicAerobic
    }

    private void defaultLTHRZonesConfig() {
        HRThreshold = Math.round(((float) HRResting) + (((float) ((HRThreshold - HRResting) * 85)) / 100.0f)); // LTHRThresholdHeartRate
        zone5 = Math.round(((float) (HRThreshold * 102)) / 100.0f); // Anaerobic
        zone4 = Math.round(((float) (HRThreshold * 97)) / 100.0f); // Lactate
        zone3 = Math.round(((float) (HRThreshold * 89)) / 100.0f); // AdvancedAerobic
        zone2 = Math.round(((float) (HRThreshold * 80)) / 100.0f); // BasicAerobic
        zone1 = Math.round(((float) (HRThreshold * 67)) / 100.0f); // WarmUp
    }

}
