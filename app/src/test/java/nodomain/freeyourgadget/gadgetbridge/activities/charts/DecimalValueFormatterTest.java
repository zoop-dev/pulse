package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DecimalValueFormatterTest {
    @Test
    public void roundsToFixedDecimals() {
        assertEquals("328", new DecimalValueFormatter(0).format(328.084));
        assertEquals("329", new DecimalValueFormatter(0).format(328.7));
        assertEquals("0", new DecimalValueFormatter(0).format(0));
    }

    @Test
    public void keepsRequestedDecimalsWithDotSeparator() {
        assertEquals("3.11", new DecimalValueFormatter(2).format(3.106855));
        assertEquals("39.4", new DecimalValueFormatter(1).format(39.37));
        assertEquals("22.37", new DecimalValueFormatter(2).getFormattedValue(22.36936f));
    }
}
