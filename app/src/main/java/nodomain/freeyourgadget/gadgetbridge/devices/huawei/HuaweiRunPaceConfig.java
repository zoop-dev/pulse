/*  Copyright (C) 2024 Me7c7

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

// TODO: make this configurable
// NOTE: algorithms used in this class are generic. So this data can be used with other devices.
// We can move this class to global scope.
public class HuaweiRunPaceConfig {

    private int zone5HIITRunMax = 300;
    private int zone5HIITRunMin = 330;
    private int zone4AnaerobicMin = 360;
    private int zone3LactateThresholdMin = 390;
    private int zone2MarathonMin = 420;
    private int zone1JogMin = 450;

    public int getZone5HIITRunMax() {
        return zone5HIITRunMax;
    }

    public void setZone5HIITRunMax(int zone5HIITRunMax) {
        this.zone5HIITRunMax = zone5HIITRunMax;
    }

    public int getZone5HIITRunMin() {
        return zone5HIITRunMin;
    }

    public void setZone5HIITRunMin(int zone5HIITRunMin) {
        this.zone5HIITRunMin = zone5HIITRunMin;
    }

    public int getZone4AnaerobicMin() {
        return zone4AnaerobicMin;
    }

    public void setZone4AnaerobicMin(int zone4AnaerobicMin) {
        this.zone4AnaerobicMin = zone4AnaerobicMin;
    }

    public int getZone3LactateThresholdMin() {
        return zone3LactateThresholdMin;
    }

    public void setZone3LactateThresholdMin(int zone3LactateThresholdMin) {
        this.zone3LactateThresholdMin = zone3LactateThresholdMin;
    }

    public int getZone2MarathonMin() {
        return zone2MarathonMin;
    }

    public void setZone2MarathonMin(int zone2MarathonMin) {
        this.zone2MarathonMin = zone2MarathonMin;
    }

    public int getZone1JogMin() {
        return zone1JogMin;
    }

    public void setZone1JogMin(int zone1JogMin) {
        this.zone1JogMin = zone1JogMin;
    }
}
