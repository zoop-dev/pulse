package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class ZeppOsActivityTypeTest {
    @Test
    public void testNoDuplicates() {
        // Ensure there are no duplicated activity types with the same code
        final Set<ZeppOsActivityType> duplicates = new HashSet<>();
        final Set<Byte> seen = new HashSet<>();

        for (final ZeppOsActivityType sport : ZeppOsActivityType.values()) {
            if (seen.contains(sport.getCode())) {
                duplicates.add(sport);
            }
            seen.add(sport.getCode());
        }

        assertTrue("Duplicated ZeppOsActivityType codes: " + duplicates, duplicates.isEmpty());
    }
}
