package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

/*  Copyright (C) 2024-2026 José Rebelo, Thomas Kuehne

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
import static org.junit.Assert.assertTrue;

import android.util.Pair;

import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class GarminSportTest extends TestBase {
    @Test
    public void testNoDuplicates() {
        // Ensure there are no duplicated sports with the same type and subtype
        final Set<GarminSport> duplicates = new HashSet<>();
        final Set<Pair<Integer, Integer>> seen = new HashSet<>();

        for (final GarminSport sport : GarminSport.values()) {
            final Pair<Integer, Integer> codePair = Pair.create(sport.getType(), sport.getSubtype());
            if (seen.contains(codePair)) {
                duplicates.add(sport);
            }
            seen.add(codePair);
        }

        assertTrue("Duplicated sport codes: " + duplicates, duplicates.isEmpty());
    }

    // Ensure that every GarminSport has a subsport=0 fallback
    @Test
    public void testFallbackSport() {
        int defaultIcon = ActivityKind.ACTIVITY.getIcon();
        for (final GarminSport sport : GarminSport.values()) {
            if (sport.getSubtype() != 0) {
                Optional<GarminSport> fallback = GarminSport.fromCodes(sport.getType(), 0);
                assertTrue(sport.name() + " has no fallback", fallback.isPresent());
            }
        }
    }
}
