/*  Copyright (C) 2024-2026 José Rebelo, Thomas Kuehne, Daniele Gobbetti

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

import java.util.Optional;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.RecordHeader;

public class FitMonitoring extends AbstractFitMonitoring {
    public FitMonitoring(final RecordDefinition recordDefinition, final RecordHeader recordHeader) {
        super(recordDefinition, recordHeader);
    }

    public Long computeTimestamp(final Long lastMonitoringTimestamp) {
        final Integer timestamp16 = getTimestamp16();

        if (timestamp16 != null && lastMonitoringTimestamp != null) {
            final int referenceGarminTs = GarminTimeUtils.unixTimeToGarminTimestamp(lastMonitoringTimestamp.intValue());
            int timeDiff = (timestamp16 & 0xFFFF) - (referenceGarminTs & 0xFFFF);

            // Handle rollover
            if (timeDiff < -32768) {
                timeDiff += 65536;
            } else if (timeDiff > 32768) {
                timeDiff -= 65536;
            }

            return lastMonitoringTimestamp + timeDiff;
        }

        if (lastMonitoringTimestamp != null) {
            return lastMonitoringTimestamp;
        }

        return getComputedTimestamp();
    }

    public Optional<Integer> getComputedActivityType() {
        final Integer activityType = getActivityType();
        if (activityType != null) {
            return Optional.of(activityType);
        }

        final Integer currentActivityTypeIntensity = getCurrentActivityTypeIntensity();
        if (currentActivityTypeIntensity != null) {
            return Optional.of(currentActivityTypeIntensity & 0x1F);
        }

        return Optional.empty();
    }

    public Integer getComputedIntensity() {
        final Integer currentActivityTypeIntensity = getCurrentActivityTypeIntensity();
        if (currentActivityTypeIntensity != null) {
            return (currentActivityTypeIntensity >> 5) & 0x7;
        }

        return null;
    }
}
