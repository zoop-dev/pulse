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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.enums;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void outdoorAliasesFallBackToGenericSport() {
        // Xiaomi-specific OUTDOOR_* kinds had no GarminSport entry, causing exports to fall
        // back to GENERIC (sport=0) — third-party importers showed these as unrecognised
        // "workout" sessions. They now resolve onto the matching street sub-variant
        // (running/walking/cycling sport with subtype=2 STREET), which keeps the FIT sport
        // code recognised instead of generic. OUTDOOR_RUNNING has no direct entry and is
        // aliased to STREET_RUNNING (STREET_RUN 1/2); OUTDOOR_WALKING / OUTDOOR_CYCLING map
        // directly to STREET_WALKING (11/2) / STREET_CYCLING (2/2).
        final Optional<GarminSport> run = GarminSport.fromActivityKind(ActivityKind.OUTDOOR_RUNNING);
        assertTrue("OUTDOOR_RUNNING should map", run.isPresent());
        assertEquals(1, run.get().getType());
        assertEquals(2, run.get().getSubtype());

        final Optional<GarminSport> walk = GarminSport.fromActivityKind(ActivityKind.OUTDOOR_WALKING);
        assertTrue("OUTDOOR_WALKING should map", walk.isPresent());
        assertEquals(11, walk.get().getType());
        assertEquals(2, walk.get().getSubtype());

        final Optional<GarminSport> bike = GarminSport.fromActivityKind(ActivityKind.OUTDOOR_CYCLING);
        assertTrue("OUTDOOR_CYCLING should map", bike.isPresent());
        assertEquals(2, bike.get().getType());
        assertEquals(2, bike.get().getSubtype());
    }
}
