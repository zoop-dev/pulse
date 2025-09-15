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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HeartRateZonesConfig;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public class HuaweiWorkoutUtils {
    private static final Map<ActivityKind, Integer> activityHRZoneType = createActivityHRZoneType();

    //TODO: discover and add more activity types. Should be same as in the watch.
    private static Map<ActivityKind, Integer> createActivityHRZoneType() {
        final Map<ActivityKind, Integer>  result = new HashMap<>();
        result.put(ActivityKind.RUNNING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.WALKING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.CYCLING, HeartRateZonesConfig.TYPE_SITTING);
        result.put(ActivityKind.MOUNTAIN_HIKE, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.INDOOR_RUNNING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.POOL_SWIM, HeartRateZonesConfig.TYPE_SWIMMING);
        result.put(ActivityKind.INDOOR_CYCLING, HeartRateZonesConfig.TYPE_SITTING);
        result.put(ActivityKind.SWIMMING_OPENWATER, HeartRateZonesConfig.TYPE_SWIMMING);
        result.put(ActivityKind.INDOOR_WALKING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.HIKING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.JUMP_ROPING, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.PINGPONG, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.BADMINTON, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.TENNIS, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.SOCCER, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.BASKETBALL, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.VOLLEYBALL, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.ELLIPTICAL_TRAINER, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.ROWING_MACHINE, HeartRateZonesConfig.TYPE_SITTING);
        result.put(ActivityKind.STEPPER, HeartRateZonesConfig.TYPE_UPRIGHT);
        result.put(ActivityKind.YOGA, HeartRateZonesConfig.TYPE_OTHER);
        return Collections.unmodifiableMap(result);
    }

    public static Integer getHRZonePostureTypeByActivity(ActivityKind type) {
        if(activityHRZoneType.containsKey(type)) {
            return activityHRZoneType.get(type);
        }
        return null;
    }
}
