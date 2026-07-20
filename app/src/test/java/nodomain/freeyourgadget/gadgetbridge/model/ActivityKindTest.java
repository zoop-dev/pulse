package nodomain.freeyourgadget.gadgetbridge.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class ActivityKindTest {
    @Test
    public void enumCheckNoOverlap() {
        // Ensure that no 2 activity kind overlap in codes
        final Map<Integer, Boolean> knownCodes = new HashMap<>();
        for (final ActivityKind kind : ActivityKind.values()) {
            final Boolean existingCode = knownCodes.put(kind.getCode(), true);
            assertNull("ActivityKind with overlapping codes: " + kind, existingCode);
        }
    }

    @Test
    public void rowingAndPaddleSportsAreRowingActivities() {
        assertTrue(ActivityKind.isRowingActivity(ActivityKind.ROWING));
        assertTrue(ActivityKind.isRowingActivity(ActivityKind.ROWING_MACHINE));
        assertTrue(ActivityKind.isRowingActivity(ActivityKind.KAYAKING));
        assertTrue(ActivityKind.isRowingActivity(ActivityKind.PADDLING));
        assertTrue(ActivityKind.isRowingActivity(ActivityKind.STAND_UP_PADDLEBOARDING));

        assertFalse(ActivityKind.isRowingActivity(ActivityKind.RUNNING));
        assertFalse(ActivityKind.isRowingActivity(ActivityKind.SWIMMING));
        assertFalse(ActivityKind.isRowingActivity(ActivityKind.SAILING));
    }
}
