package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.test.TestBase;

public class CmfActivityTypeTest extends TestBase {
    @Test
    public void validPreferenceValues() {
        final String[] values = getContext().getResources().getStringArray(R.array.pref_cmf_activity_types_values);
        for (String value : values) {
            try {
                CmfActivityType.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                fail(String.format("%s is not a valid CmfActivityType", value));
            }
        }

        final String[] defaults = getContext().getResources().getStringArray(R.array.pref_cmf_activity_types_default);
        for (String value : defaults) {
            try {
                CmfActivityType.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (final IllegalArgumentException e) {
                fail(String.format("%s is not a valid CmfActivityType", value));
            }
        }
    }
}
