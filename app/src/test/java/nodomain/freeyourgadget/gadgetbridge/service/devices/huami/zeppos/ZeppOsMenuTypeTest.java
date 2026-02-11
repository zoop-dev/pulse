package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class ZeppOsMenuTypeTest extends TestBase {
    @Test
    public void testDisplayItemNameLookup_allValuesValid() {
        final Set<String> validAppValues = new HashSet<>(Arrays.asList(
                getContext().getResources().getStringArray(R.array.pref_zepp_os_apps_values)
        ));

        for (Map.Entry<String, String> entry : ZeppOsMenuType.displayItemNameLookup.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            assertTrue(
                    String.format("Value '%s' (key: %s) in displayItemNameLookup is not in pref_zepp_os_apps_values",
                            value, key),
                    validAppValues.contains(value)
            );
        }
    }

    @Test
    public void testShortcutsNameLookup_allValuesValid() {
        final Set<String> validAppValues = new HashSet<>(Arrays.asList(
                getContext().getResources().getStringArray(R.array.pref_zepp_os_apps_values)
        ));

        for (Map.Entry<String, String> entry : ZeppOsMenuType.shortcutsNameLookup.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            assertTrue(
                    String.format("Value '%s' (key: %s) in shortcutsNameLookup is not in pref_zepp_os_apps_values",
                            value, key),
                    validAppValues.contains(value)
            );
        }
    }

    @Test
    public void testControlCenterNameLookup_allValuesValid() {
        final Set<String> validControlCenterValues = new HashSet<>(Arrays.asList(
                getContext().getResources().getStringArray(R.array.pref_huami2021_control_center_values)
        ));

        for (Map.Entry<String, String> entry : ZeppOsMenuType.controlCenterNameLookup.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();

            assertTrue(
                    String.format("Value '%s' (key: %s) in controlCenterNameLookup is not in pref_huami2021_control_center_values", value, key),
                    validControlCenterValues.contains(value)
            );
        }
    }

    @Test
    public void testAllKeys_areUppercaseHex() {
        // Verify that all keys follow the expected format: 8-digit uppercase hex strings
        final String hexPattern = "^[0-9A-F]{8}$";

        for (String key : ZeppOsMenuType.displayItemNameLookup.keySet()) {
            assertTrue(
                    String.format("Key '%s' in displayItemNameLookup should be 8-digit uppercase hex", key),
                    key.matches(hexPattern)
            );
        }

        for (String key : ZeppOsMenuType.shortcutsNameLookup.keySet()) {
            assertTrue(
                    String.format("Key '%s' in shortcutsNameLookup should be 8-digit uppercase hex", key),
                    key.matches(hexPattern)
            );
        }

        for (String key : ZeppOsMenuType.controlCenterNameLookup.keySet()) {
            assertTrue(
                    String.format("Key '%s' in controlCenterNameLookup should be 8-digit uppercase hex", key),
                    key.matches(hexPattern)
            );
        }
    }
}
