package com.nip.numberinterestingfacts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LookupInputTest {
    @Test
    public void acceptsWholeNumbersInRange() {
        assertEquals(Integer.valueOf(0), RandomActivity.parseNumber("0", 0));
        assertEquals(Integer.valueOf(42), RandomActivity.parseNumber(" 42 ", 0));
        assertEquals(Integer.valueOf(7), RandomActivity.parseNumber("0007", 0));
        assertEquals(Integer.valueOf(9999), RandomActivity.parseNumber("9999", 0));
    }

    @Test
    public void rejectsEverythingElse() {
        assertNull(RandomActivity.parseNumber("", 0));
        assertNull(RandomActivity.parseNumber("   ", 0));
        assertNull(RandomActivity.parseNumber("-1", 0));
        assertNull(RandomActivity.parseNumber("1.5", 0));
        assertNull(RandomActivity.parseNumber("12a", 0));
        assertNull(RandomActivity.parseNumber("10000", 0));
        // Year 0 does not exist in the app's year range.
        assertNull(RandomActivity.parseNumber("0", 1));
    }
}
