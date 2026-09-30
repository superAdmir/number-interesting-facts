package com.nip.numberinterestingfacts.facts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;

public class LocalFactGeneratorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 25);

    @Test
    public void numbersAreSpelledInEnglish() {
        assertEquals("zero", LocalFactGenerator.toWords(0));
        assertEquals("thirteen", LocalFactGenerator.toWords(13));
        assertEquals("forty", LocalFactGenerator.toWords(40));
        assertEquals("twenty-one", LocalFactGenerator.toWords(21));
        assertEquals("one hundred five", LocalFactGenerator.toWords(105));
        assertEquals("one thousand seven hundred twenty-nine", LocalFactGenerator.toWords(1729));
        assertEquals("two thousand", LocalFactGenerator.toWords(2000));
        assertEquals("nine thousand nine hundred ninety-nine", LocalFactGenerator.toWords(9999));
    }

    @Test
    public void romanNumerals() {
        assertEquals("IV", LocalFactGenerator.toRoman(4));
        assertEquals("XLII", LocalFactGenerator.toRoman(42));
        assertEquals("MCMXCIV", LocalFactGenerator.toRoman(1994));
        assertEquals("MMXXVI", LocalFactGenerator.toRoman(2026));
        assertEquals("MMMCMXCIX", LocalFactGenerator.toRoman(3999));
    }

    @Test
    public void ordinals() {
        assertEquals("1st", LocalFactGenerator.ordinal(1));
        assertEquals("2nd", LocalFactGenerator.ordinal(2));
        assertEquals("3rd", LocalFactGenerator.ordinal(3));
        assertEquals("4th", LocalFactGenerator.ordinal(4));
        assertEquals("11th", LocalFactGenerator.ordinal(11));
        assertEquals("12th", LocalFactGenerator.ordinal(12));
        assertEquals("13th", LocalFactGenerator.ordinal(13));
        assertEquals("21st", LocalFactGenerator.ordinal(21));
        assertEquals("101st", LocalFactGenerator.ordinal(101));
        assertEquals("111th", LocalFactGenerator.ordinal(111));
    }

    @Test
    public void primesAndFactorisation() {
        assertFalse(LocalFactGenerator.isPrime(0));
        assertFalse(LocalFactGenerator.isPrime(1));
        assertTrue(LocalFactGenerator.isPrime(2));
        assertTrue(LocalFactGenerator.isPrime(9973));
        assertFalse(LocalFactGenerator.isPrime(9999));
        assertEquals("2³ × 3² × 5", LocalFactGenerator.factorize(360));
        assertEquals("3² × 11 × 101", LocalFactGenerator.factorize(9999));
    }

    @Test
    public void mathFactsDescribeKnownNumbers() {
        assertContains(LocalFactGenerator.mathFacts(7), "7 is the 4th prime number.");
        assertContains(LocalFactGenerator.mathFacts(6), "6 is a perfect number: it equals the sum of its other divisors.");
        assertContains(LocalFactGenerator.mathFacts(28), "28 is a perfect number: it equals the sum of its other divisors.");
        assertContains(LocalFactGenerator.mathFacts(12), "12 is an abundant number: its other divisors add up to 16, which is more than 12.");
        assertContains(LocalFactGenerator.mathFacts(12), "12 has 6 divisors, which add up to 28.");
        assertContains(LocalFactGenerator.mathFacts(64), "64 is a perfect square: 8 × 8.");
        assertContains(LocalFactGenerator.mathFacts(64), "64 is a perfect cube: 4³.");
        assertContains(LocalFactGenerator.mathFacts(64), "64 is a power of two: 2⁶.");
        assertContains(LocalFactGenerator.mathFacts(120), "120 is a factorial: 5! = 120.");
        assertContains(LocalFactGenerator.mathFacts(55), "55 is a triangular number: 1 + 2 + … + 10 = 55.");
        assertTrue(LocalFactGenerator.mathFacts(55).stream().anyMatch(f -> f.startsWith("55 is a Fibonacci number")));
        assertFalse(LocalFactGenerator.mathFacts(56).stream().anyMatch(f -> f.contains("Fibonacci")));
    }

    @Test
    public void triviaFacts() {
        assertContains(LocalFactGenerator.triviaFacts(42), "In Roman numerals, 42 is written as XLII.");
        assertContains(LocalFactGenerator.triviaFacts(42), "In binary, 42 is 101010: 6 digits.");
        assertContains(LocalFactGenerator.triviaFacts(255), "In hexadecimal, 255 is written as FF.");
        assertContains(LocalFactGenerator.triviaFacts(1221), "1221 is a palindrome: it reads the same forwards and backwards.");
        assertContains(LocalFactGenerator.triviaFacts(1729), "Written backwards, 1729 becomes 9271.");
        assertContains(LocalFactGenerator.triviaFacts(1), "In binary, 1 is 1: 1 digit.");
    }

    @Test
    public void yearFactsFollowTheGregorianCalendar() {
        assertContains(LocalFactGenerator.yearFacts(2024), "2024 is a leap year, so it has 366 days.");
        assertContains(LocalFactGenerator.yearFacts(2000), "2000 is a leap year, so it has 366 days.");
        assertContains(LocalFactGenerator.yearFacts(1900),
                "1900 is not a leap year: century years are only leap years when they are divisible by 400.");
        assertContains(LocalFactGenerator.yearFacts(2026), "2026 is a common year of 365 days.");
        assertContains(LocalFactGenerator.yearFacts(2026), "2026 is in the 21st century.");
        assertContains(LocalFactGenerator.yearFacts(2000), "2000 is in the 20th century.");
        assertContains(LocalFactGenerator.yearFacts(2026),
                "In 2026, January 1 falls on a Thursday and December 31 on a Thursday.");
        assertContains(LocalFactGenerator.yearFacts(2026), "2026 has 3 Fridays the 13th.");
        assertContains(LocalFactGenerator.yearFacts(2026), "2026 has 53 ISO weeks instead of the usual 52.");
        assertFalse(LocalFactGenerator.yearFacts(2025).contains("2025 has 53 ISO weeks instead of the usual 52."));
        // No weekday facts before the Gregorian calendar existed.
        assertFalse(LocalFactGenerator.yearFacts(1000).stream().anyMatch(f -> f.contains("January 1")));
    }

    @Test
    public void dateFacts() {
        List<String> piDay = LocalFactGenerator.dateFacts(MonthDay.of(3, 14), TODAY);
        assertContains(piDay, "March 14 is day 73 of the year, or day 74 in leap years.");
        assertContains(piDay, "After March 14, there are 292 days left in a common year.");
        assertContains(piDay, "In 2026, March 14 falls on a Saturday.");
        assertContains(piDay, "March 14 is in March, a month with 31 days.");
        assertContains(LocalFactGenerator.dateFacts(MonthDay.of(1, 1), TODAY), "January 1 is day 1 of the year.");
        assertContains(LocalFactGenerator.dateFacts(MonthDay.of(2, 10), TODAY),
                "February 10 is in February, a month with 28 days, or 29 in leap years.");
        List<String> leapDay = LocalFactGenerator.dateFacts(MonthDay.of(2, 29), TODAY);
        assertContains(leapDay, "The next February 29 is in 2028, when it falls on a Tuesday.");
    }

    @Test
    public void everySupportedInputProducesFacts() {
        for (int n = 0; n <= LocalFactGenerator.MAX_NUMBER; n++) {
            assertFalse(LocalFactGenerator.triviaFacts(n).isEmpty());
            assertFalse(LocalFactGenerator.mathFacts(n).isEmpty());
        }
        for (int y = LocalFactGenerator.MIN_YEAR; y <= LocalFactGenerator.MAX_YEAR; y++) {
            assertFalse(LocalFactGenerator.yearFacts(y).isEmpty());
        }
        for (int d = 1; d <= 366; d++) {
            MonthDay md = MonthDay.from(LocalDate.ofYearDay(2024, d));
            assertFalse(LocalFactGenerator.dateFacts(md, TODAY).isEmpty());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfRangeNumbers() {
        LocalFactGenerator.mathFacts(10000);
    }

    @Test
    public void generateFillsSubjectAndSource() {
        Clock clock = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
        LocalFactGenerator generator = new LocalFactGenerator(new Random(7), clock);
        for (FactCategory c : FactCategory.values()) {
            for (int i = 0; i < 200; i++) {
                Fact fact = generator.generate(FactQuery.random(c));
                assertEquals(c, fact.category);
                assertEquals(Fact.Source.ON_DEVICE, fact.source);
                assertFalse(fact.subject.isEmpty());
                assertFalse(fact.text.isEmpty());
            }
        }
        Fact year = generator.generate(FactQuery.ofNumber(FactCategory.YEAR, 1969));
        assertEquals("1969", year.subject);
        assertTrue(year.text.contains("1969"));
        Fact date = generator.generate(FactQuery.ofDate(MonthDay.of(12, 25)));
        assertEquals("December 25", date.subject);
    }

    private static void assertContains(List<String> facts, String expected) {
        assertTrue("Expected \"" + expected + "\" in " + facts, facts.contains(expected));
    }
}
