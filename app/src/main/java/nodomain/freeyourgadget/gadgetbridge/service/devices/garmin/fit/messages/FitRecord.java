/*  Copyright (C) 2024-2026 José Rebelo, Daniele Gobbetti, punchdeerflyscropio, a0z,
        Thomas Kuehne, DanyPM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.messages;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityPoint;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitRecord extends AbstractFitRecord {
    public FitRecord(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);
    }

    public ActivityPoint toActivityPoint() {
        final ActivityPoint.Builder builder = new ActivityPoint.Builder(getComputedTimestamp() * 1000L);
        builder.setBodyEnergy(getBodyBattery());
        builder.setCadence(getCadence());
        builder.setCnsToxicity(getCnsLoad());
        builder.setDepth(getDepth());
        builder.setDistance(getDistance());
        builder.setHdop(getGpsAccuracy());
        builder.setHeartRate(getHeartRate());
        builder.setLatitude(getLatitude());
        builder.setLongitude(getLongitude());
        builder.setN2Load(getN2Load());
        builder.setPower(getPower());
        builder.setStamina(getStamina());
        builder.setStepLength(getStepLength());
        builder.setTemperature(getTemperature());

        builder.setVerticalOscillation(getOscillation());
        builder.setStanceTimePercent(getStanceTimePercent());
        builder.setStanceTime(getStanceTime());
        builder.setVerticalRatio(getVerticalRatio());
        builder.setStanceTimeBalance(getStanceTimeBalance());
        builder.setPerformanceCondition(getPerformanceCondition());

        final Double enhancedAltitude = getEnhancedAltitude();
        if (enhancedAltitude == null) {
            builder.setAltitude(getAltitude());
        } else {
            builder.setAltitude(enhancedAltitude);
        }

        final Float enhancedRespirationRate = getEnhancedRespirationRate();
        if (enhancedRespirationRate == null) {
            builder.setRespiratoryRate(getRespirationRate());
        } else {
            builder.setRespiratoryRate(enhancedRespirationRate);
        }

        final Double enhancedSpeed = getEnhancedSpeed();
        if (enhancedSpeed == null) {
            builder.setSpeed(getSpeed());
        } else {
            builder.setSpeed(enhancedSpeed.floatValue());
        }

        return builder.build();
    }
}
