package nodomain.freeyourgadget.gadgetbridge.util.kotlin

import org.junit.Assert.assertEquals
import org.junit.Test

class IntExtensionsTest {
    @Test
    fun testCoerceInWithStep_valueWithinRange() {
        // Value already on a step boundary within range
        assertEquals(50, 50.coerceIn(0, 100, 10))

        // Value within range but not on step boundary - should round to nearest step
        assertEquals(50, 52.coerceIn(0, 100, 10))
        assertEquals(50, 48.coerceIn(0, 100, 10))
    }

    @Test
    fun testCoerceInWithStep_valueBelowMin() {
        // Value below minimum should be coerced to minimum (respecting step)
        assertEquals(0, (-10).coerceIn(0, 100, 10))
        assertEquals(10, 5.coerceIn(10, 100, 10))
    }

    @Test
    fun testCoerceInWithStep_valueAboveMax() {
        // Value above maximum should be coerced to maximum
        assertEquals(100, 150.coerceIn(0, 100, 10))
        assertEquals(100, 105.coerceIn(0, 100, 10))
    }

    @Test
    fun testCoerceInWithStep_roundingToNearestStep() {
        // Test rounding behavior: rounds to nearest step
        assertEquals(10, 6.coerceIn(0, 100, 10))
        assertEquals(10, 7.coerceIn(0, 100, 10))
        assertEquals(10, 8.coerceIn(0, 100, 10))
        assertEquals(10, 9.coerceIn(0, 100, 10))
        assertEquals(10, 10.coerceIn(0, 100, 10))
        assertEquals(10, 11.coerceIn(0, 100, 10))
        assertEquals(10, 12.coerceIn(0, 100, 10))
        assertEquals(10, 13.coerceIn(0, 100, 10))
        assertEquals(10, 14.coerceIn(0, 100, 10))
        // > 15 should round to 20
        assertEquals(20, 15.coerceIn(0, 100, 10))
        assertEquals(20, 16.coerceIn(0, 100, 10))
        assertEquals(20, 17.coerceIn(0, 100, 10))
        assertEquals(20, 18.coerceIn(0, 100, 10))
        assertEquals(20, 19.coerceIn(0, 100, 10))
    }

    @Test
    fun testCoerceInWithStep_differentStepSizes() {
        // Test with step size of 5
        assertEquals(15, 14.coerceIn(0, 100, 5))
        assertEquals(15, 16.coerceIn(0, 100, 5))

        // Test with step size of 25
        assertEquals(25, 24.coerceIn(0, 100, 25))
        assertEquals(25, 30.coerceIn(0, 100, 25))

        // Test with step size of 1 (should behave like normal coerceIn)
        assertEquals(42, 42.coerceIn(0, 100, 1))
        assertEquals(10, 10.coerceIn(0, 100, 1))
        assertEquals(15, 15.coerceIn(0, 100, 1))
        assertEquals(0, (-5).coerceIn(0, 100, 1))
        assertEquals(100, 105.coerceIn(0, 100, 1))
    }

    @Test
    fun testCoerceInWithStep_negativeRanges() {
        // Test with negative min and max
        assertEquals(-50, (-52).coerceIn(-100, 0, 10))
        assertEquals(-50, (-48).coerceIn(-100, 0, 10))
        assertEquals(-100, (-150).coerceIn(-100, 0, 10))
        assertEquals(0, 50.coerceIn(-100, 0, 10))
    }

    @Test
    fun testCoerceInWithStep_rangeSpanningZero() {
        // Test with range spanning negative to positive
        assertEquals(0, 2.coerceIn(-50, 50, 10))
        assertEquals(10, 12.coerceIn(-50, 50, 10))
        assertEquals(-10, (-12).coerceIn(-50, 50, 10))
    }

    @Test
    fun testCoerceInWithStep_edgeCases() {
        // Value exactly at minimum
        assertEquals(0, 0.coerceIn(0, 100, 10))

        // Value exactly at maximum
        assertEquals(100, 100.coerceIn(0, 100, 10))

        // Single value range (min == max)
        assertEquals(50, 50.coerceIn(50, 50, 10))
        assertEquals(50, 45.coerceIn(50, 50, 10))
        assertEquals(50, 55.coerceIn(50, 50, 10))
    }

    @Test
    fun testCoerceInWithStep_largeStepSize() {
        // Step size larger than range
        assertEquals(0, 25.coerceIn(0, 50, 100))
        assertEquals(0, 49.coerceIn(0, 50, 100))
        assertEquals(50, 51.coerceIn(0, 50, 100))
        assertEquals(50, 99.coerceIn(0, 50, 100))
        assertEquals(50, 101.coerceIn(0, 50, 100))
        assertEquals(50, 150.coerceIn(0, 50, 100))
        assertEquals(50, 200.coerceIn(0, 50, 100))
        assertEquals(50, 250.coerceIn(0, 50, 100))
        assertEquals(0, (-10).coerceIn(0, 50, 100))
    }

    @Test
    fun testCoerceInWithStep_smallValues() {
        // Test with small ranges and steps
        assertEquals(0, 0.coerceIn(0, 10, 2))
        assertEquals(2, 1.coerceIn(0, 10, 2))
        assertEquals(2, 2.coerceIn(0, 10, 2))
        assertEquals(4, 3.coerceIn(0, 10, 2))
        assertEquals(4, 4.coerceIn(0, 10, 2))
        assertEquals(6, 5.coerceIn(0, 10, 2))
        assertEquals(6, 6.coerceIn(0, 10, 2))
        assertEquals(8, 7.coerceIn(0, 10, 2))
        assertEquals(8, 8.coerceIn(0, 10, 2))
    }

    @Test
    fun testCoerceInWithStep_maxNotOnStepBoundary() {
        // When max is not on a step boundary
        // e.g., 0, 10, 20, 30, 40, 50... but max is 45
        assertEquals(40, 42.coerceIn(0, 45, 10))
        assertEquals(45, 48.coerceIn(0, 45, 10))  // Rounded to 50, coerced to 45
    }
}
