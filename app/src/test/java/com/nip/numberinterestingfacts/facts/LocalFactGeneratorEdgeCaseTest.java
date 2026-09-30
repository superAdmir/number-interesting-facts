package com.nip.numberinterestingfacts.facts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

/** Edge cases and documented input limits of the calculated (supplementary) facts. */
public class LocalFactGeneratorEdgeCaseTest {

    private static void assertRejected(Runnable r) {
        try {
            r.run();
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // documented limit
        }
    }

    @Test
    public void inputLimitsAreEnforced() {
        assertRejected(() -> LocalFactGenerator.mathFacts(-1));
        assertRejected(() -> LocalFactGenerator.triviaFacts(-1));
        assertRejected(() -> LocalFactGenerator.mathFacts(10000));
        assertRejected(() -> LocalFactGenerator.triviaFacts(Integer.MAX_VALUE));
        assertRejected(() -> LocalFactGenerator.mathFacts(Integer.MIN_VALUE));
        assertRejected(() -> LocalFactGenerator.yearFacts(0));
        assertRejected(() -> LocalFactGenerator.yearFacts(10000));
    }

    @Test
    public void zeroAndOne() {
        List<String> zeroMath = LocalFactGenerator.mathFacts(0);
        assertTrue(zeroMath.stream().noneMatch(f -> f.contains("prime")));
        assertTrue(LocalFactGenerator.triviaFacts(0).contains("In binary, 0 is 0: 1 digit."));
        // No Roman numeral for 0 (there is none).
        assertTrue(LocalFactGenerator.triviaFacts(0).stream().noneMatch(f -> f.contains("Roman")));
        assertTrue(LocalFactGenerator.mathFacts(1).contains("1 is neither prime nor composite."));
    }

    @Test
    public void romanNumeralLimits() {
        assertTrue(LocalFactGenerator.triviaFacts(3999).contains("In Roman numerals, 3999 is written as MMMCMXCIX."));
        assertTrue(LocalFactGenerator.triviaFacts(4000).stream().noneMatch(f -> f.contains("Roman")));
        assertTrue(LocalFactGenerator.yearFacts(4000).stream().noneMatch(f -> f.contains("Roman")));
        assertTrue(LocalFactGenerator.triviaFacts(1).contains("In Roman numerals, 1 is written as I."));
    }

    @Test
    public void largestInputsDoNotOverflow() {
        // 9999 = 3² × 11 × 101, σ = 13 × 12 × 102 = 15912: sums stay far below int overflow.
        assertTrue(LocalFactGenerator.mathFacts(9999).contains("9999 has 12 divisors, which add up to 15912."));
        assertTrue(LocalFactGenerator.mathFacts(9973).contains("9973 is the 1229th prime number."));
        assertTrue(LocalFactGenerator.mathFacts(8192).contains("8192 is a power of two: 2¹³."));
        assertTrue(LocalFactGenerator.mathFacts(5040).contains("5040 is a factorial: 7! = 5040."));
    }

    @Test
    public void leapYearCenturyRules() {
        assertTrue(LocalFactGenerator.yearFacts(1600).contains("1600 is a leap year, so it has 366 days."));
        assertTrue(LocalFactGenerator.yearFacts(2000).contains("2000 is a leap year, so it has 366 days."));
        for (int y : new int[]{1700, 1800, 1900, 2100}) {
            assertTrue(String.valueOf(y), LocalFactGenerator.yearFacts(y).contains(y
                    + " is not a leap year: century years are only leap years when they are divisible by 400."));
        }
        assertTrue(LocalFactGenerator.yearFacts(2024).contains("2024 is a leap year, so it has 366 days."));
    }

    @Test
    public void yearsBeforeTheGregorianCalendarAreQualified() {
        List<String> y1000 = LocalFactGenerator.yearFacts(1000);
        assertTrue(y1000.contains("By Gregorian calendar rules, 1000 is not a leap year: century years are only leap years when they are divisible by 400."));
        assertTrue(LocalFactGenerator.yearFacts(1).contains("By Gregorian calendar rules, 1 is a common year of 365 days."));
        assertTrue(LocalFactGenerator.yearFacts(1582).stream().noneMatch(f -> f.contains("January 1 falls")));
        assertTrue(LocalFactGenerator.yearFacts(1583).stream().anyMatch(f -> f.contains("January 1 falls")));
    }

    @Test
    public void february29AndYearEnd() {
        // From March 2097 the next February 29 skips 2100, which is not a leap year.
        List<String> leap = LocalFactGenerator.dateFacts(MonthDay.of(2, 29), LocalDate.of(2097, 3, 1));
        assertTrue(leap.stream().anyMatch(f -> f.startsWith("The next February 29 is in 2104")));
        // On February 29 itself, that day counts as the next one.
        List<String> onTheDay = LocalFactGenerator.dateFacts(MonthDay.of(2, 29), LocalDate.of(2028, 2, 29));
        assertTrue(onTheDay.stream().anyMatch(f -> f.startsWith("The next February 29 is in 2028")));
        List<String> dec31 = LocalFactGenerator.dateFacts(MonthDay.of(12, 31), LocalDate.of(2026, 9, 25));
        assertTrue(dec31.contains("December 31 is the last day of the year."));
        assertTrue(dec31.contains("December 31 is day 365 of the year, or day 366 in leap years."));
        assertTrue(dec31.stream().noneMatch(f -> f.contains("0 days")));
    }

    @Test
    public void todayComesFromTheDeviceClockAndZone() {
        // 2026-12-31T23:30Z is already January 1, 2027 in Tokyo: "this year" follows the device zone.
        java.time.Instant instant = java.time.Instant.parse("2026-12-31T23:30:00Z");
        LocalFactGenerator tokyo = new LocalFactGenerator(new Random(1), Clock.fixed(instant, ZoneId.of("Asia/Tokyo")));
        LocalFactGenerator utc = new LocalFactGenerator(new Random(1), Clock.fixed(instant, ZoneOffset.UTC));
        FactQuery q = FactQuery.ofDate(MonthDay.of(7, 4));
        boolean tokyo2027 = false, utc2026 = false;
        for (int i = 0; i < 50; i++) {
            tokyo2027 |= tokyo.generate(q).text.startsWith("In 2027, July 4");
            utc2026 |= utc.generate(q).text.startsWith("In 2026, July 4");
        }
        assertTrue(tokyo2027);
        assertTrue(utc2026);
    }

    @Test
    public void randomQueriesResolveInsideTheDocumentedRanges() {
        Clock clock = Clock.fixed(java.time.Instant.parse("2026-09-25T12:00:00Z"), ZoneOffset.UTC);
        LocalFactGenerator g = new LocalFactGenerator(new Random(11), clock);
        for (int i = 0; i < 2000; i++) {
            FactQuery year = g.resolveRandom(FactQuery.random(FactCategory.YEAR));
            assertTrue(year.number >= 1000 && year.number <= 2025);
            FactQuery number = g.resolveRandom(FactQuery.random(FactCategory.TRIVIA));
            assertTrue(number.number >= 0 && number.number <= 999);
            FactQuery date = g.resolveRandom(FactQuery.random(FactCategory.DATE));
            assertNotNull(date.monthDay);
        }
        FactQuery concrete = FactQuery.ofNumber(FactCategory.MATH, 28);
        assertEquals(concrete, g.resolveRandom(concrete));
    }

    @Test
    public void worstCaseCalculationIsFast() {
        // The UI never calculates on the main thread, but calls must also stay cheap.
        long worst = 0;
        long start = System.nanoTime();
        for (int n = 0; n <= LocalFactGenerator.MAX_NUMBER; n++) {
            long t = System.nanoTime();
            LocalFactGenerator.mathFacts(n);
            LocalFactGenerator.triviaFacts(n);
            worst = Math.max(worst, System.nanoTime() - t);
        }
        long totalMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("calculated facts: all 0..9999 in " + totalMs + " ms, worst single number "
                + worst / 1_000 + " µs");
        assertTrue("worst case " + worst / 1_000_000 + " ms", worst < 50_000_000L);
    }

    @Test
    public void februaryHasNoPhantomDays() {
        assertFalse(LocalFactGenerator.dateFacts(MonthDay.of(2, 28), LocalDate.of(2026, 1, 1)).isEmpty());
        assertTrue(LocalFactGenerator.dateFacts(MonthDay.of(3, 1), LocalDate.of(2026, 1, 1))
                .contains("March 1 is day 60 of the year, or day 61 in leap years."));
    }
}
