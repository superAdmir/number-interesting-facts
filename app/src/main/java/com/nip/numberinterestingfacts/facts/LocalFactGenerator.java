package com.nip.numberinterestingfacts.facts;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.MonthDay;
import java.time.Year;
import java.time.format.TextStyle;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Calculated facts, worked out on the device from the number itself or the proleptic Gregorian
 * calendar. They are a clearly labelled supplement/fallback to Wikipedia content, never a
 * replacement for trivia or history. Supported input: numbers 0–9999, years 1–9999, any
 * month/day (February 29 included). Out-of-range input throws IllegalArgumentException.
 */
public final class LocalFactGenerator {
    public static final int MAX_NUMBER = 9999;
    public static final int MIN_YEAR = 1;
    public static final int MAX_YEAR = 9999;
    /** Weekday facts are only given for years after the Gregorian calendar was introduced. */
    private static final int FIRST_GREGORIAN_YEAR = 1583;

    private static final String[] ONES = {"zero", "one", "two", "three", "four", "five", "six",
            "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen",
            "sixteen", "seventeen", "eighteen", "nineteen"};
    private static final String[] TENS = {"", "", "twenty", "thirty", "forty", "fifty", "sixty",
            "seventy", "eighty", "ninety"};
    private static final char[] SUPERSCRIPTS = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

    private final Random random;
    private final Clock clock;

    public LocalFactGenerator(@NonNull Random random, @NonNull Clock clock) {
        this.random = random;
        this.clock = clock;
    }

    /**
     * Turns a random query into a concrete one, so the Wikipedia lookup and the calculated
     * supplement always describe the same subject. Ranges favour subjects Wikipedia covers well:
     * numbers 0–999 (individual or range articles) and years from 1000 to last year.
     */
    @NonNull
    public FactQuery resolveRandom(@NonNull FactQuery query) {
        if (!query.isRandom()) return query;
        LocalDate today = LocalDate.now(clock);
        switch (query.category) {
            case DATE:
                return FactQuery.ofDate(randomMonthDay());
            case YEAR:
                return FactQuery.ofNumber(FactCategory.YEAR, 1000 + random.nextInt(today.getYear() - 1000));
            default:
                return FactQuery.ofNumber(query.category, random.nextInt(1000));
        }
    }

    /** Returns one calculated fact for the query, picking a random subject when the query is random. */
    @NonNull
    public Fact generate(@NonNull FactQuery query) {
        LocalDate today = LocalDate.now(clock);
        switch (query.category) {
            case DATE: {
                MonthDay md = query.monthDay != null ? query.monthDay : randomMonthDay();
                return new Fact(FactCategory.DATE, formatMonthDay(md),
                        pick(dateFacts(md, today)), Fact.Source.ON_DEVICE);
            }
            case YEAR: {
                int year = query.number != null ? query.number
                        : FIRST_GREGORIAN_YEAR + random.nextInt(today.getYear() + 50 - FIRST_GREGORIAN_YEAR);
                return new Fact(FactCategory.YEAR, String.valueOf(year), pick(yearFacts(year)),
                        Fact.Source.ON_DEVICE);
            }
            case MATH: {
                int n = query.number != null ? query.number : randomNumber();
                return new Fact(FactCategory.MATH, String.valueOf(n), pick(mathFacts(n)),
                        Fact.Source.ON_DEVICE);
            }
            case TRIVIA:
            default: {
                int n = query.number != null ? query.number : randomNumber();
                return new Fact(FactCategory.TRIVIA, String.valueOf(n), pick(triviaFacts(n)),
                        Fact.Source.ON_DEVICE);
            }
        }
    }

    private int randomNumber() {
        // Favour smaller numbers a little: they have more recognisable properties.
        return random.nextBoolean() ? random.nextInt(1001) : random.nextInt(MAX_NUMBER + 1);
    }

    private MonthDay randomMonthDay() {
        // 2024 is a leap year, so February 29 can be picked too.
        return MonthDay.from(LocalDate.ofYearDay(2024, 1 + random.nextInt(366)));
    }

    private String pick(List<String> facts) {
        return facts.get(random.nextInt(facts.size()));
    }

    @NonNull
    public static String formatMonthDay(@NonNull MonthDay md) {
        return md.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + md.getDayOfMonth();
    }

    // ---- Trivia: how a number is written and what its digits do ----

    @VisibleForTesting
    static List<String> triviaFacts(int n) {
        requireRange(n, 0, MAX_NUMBER);
        List<String> facts = new ArrayList<>();
        facts.add(String.format(Locale.US, "%d is written as “%s” in English.", n, toWords(n)));
        if (n >= 1 && n <= 3999) {
            facts.add(String.format(Locale.US, "In Roman numerals, %d is written as %s.", n, toRoman(n)));
        }
        String binary = Integer.toBinaryString(n);
        facts.add(String.format(Locale.US, "In binary, %d is %s: %s.", n, binary,
                plural(binary.length(), "digit", "digits")));
        if (n >= 10) {
            facts.add(String.format(Locale.US, "In hexadecimal, %d is written as %s.", n,
                    Integer.toHexString(n).toUpperCase(Locale.US)));
            facts.add(String.format(Locale.US, "The digits of %d add up to %d.", n, digitSum(n)));
            if (isPalindrome(n)) {
                facts.add(String.format(Locale.US, "%d is a palindrome: it reads the same forwards and backwards.", n));
            } else if (n % 10 != 0) {
                facts.add(String.format(Locale.US, "Written backwards, %d becomes %d.", n, reverse(n)));
            }
        }
        if (n == 0) {
            facts.add("0 is the only number that is neither positive nor negative.");
        }
        return facts;
    }

    // ---- Math: arithmetic properties ----

    @VisibleForTesting
    static List<String> mathFacts(int n) {
        requireRange(n, 0, MAX_NUMBER);
        List<String> facts = new ArrayList<>();
        if (n == 0) {
            facts.add("0 is even, and adding 0 to any number leaves that number unchanged.");
            facts.add("0 multiplied by any number is 0.");
            return facts;
        }
        if (n == 1) {
            facts.add("1 is neither prime nor composite.");
            facts.add("1 is the only positive whole number that divides every whole number.");
            facts.add("1 is both a perfect square and a perfect cube, and it appears twice in the Fibonacci sequence.");
            return facts;
        }
        facts.add(String.format(Locale.US, "%d is an %s number.", n, n % 2 == 0 ? "even" : "odd"));
        if (isPrime(n)) {
            facts.add(String.format(Locale.US, "%d is a prime number: its only divisors are 1 and itself.", n));
            facts.add(String.format(Locale.US, "%d is the %s prime number.", n, ordinal(primeIndex(n))));
        } else {
            facts.add(String.format(Locale.US, "%d is composite: %d = %s.", n, n, factorize(n)));
        }
        int divisors = divisorCount(n);
        int sigma = divisorSum(n);
        facts.add(String.format(Locale.US, "%d has %s, which add up to %d.", n,
                plural(divisors, "divisor", "divisors"), sigma));
        int aliquot = sigma - n;
        if (aliquot == n) {
            facts.add(String.format(Locale.US, "%d is a perfect number: it equals the sum of its other divisors.", n));
        } else if (aliquot > n) {
            facts.add(String.format(Locale.US, "%d is an abundant number: its other divisors add up to %d, which is more than %d.", n, aliquot, n));
        }
        int root = (int) Math.round(Math.sqrt(n));
        if (root * root == n) {
            facts.add(String.format(Locale.US, "%d is a perfect square: %d × %d.", n, root, root));
        }
        int cubeRoot = (int) Math.round(Math.cbrt(n));
        if (cubeRoot * cubeRoot * cubeRoot == n) {
            facts.add(String.format(Locale.US, "%d is a perfect cube: %d³.", n, cubeRoot));
        }
        if ((n & (n - 1)) == 0) {
            facts.add(String.format(Locale.US, "%d is a power of two: 2%s.", n,
                    superscript(Integer.numberOfTrailingZeros(n))));
        }
        if (isFibonacci(n)) {
            facts.add(String.format(Locale.US, "%d is a Fibonacci number: each Fibonacci number is the sum of the two before it.", n));
        }
        int k = triangularIndex(n);
        if (k > 0) {
            facts.add(String.format(Locale.US, "%d is a triangular number: 1 + 2 + … + %d = %d.", n, k, n));
        }
        int factorialOf = factorialIndex(n);
        if (factorialOf > 0) {
            facts.add(String.format(Locale.US, "%d is a factorial: %d! = %d.", n, factorialOf, n));
        }
        return facts;
    }

    // ---- Years: the Gregorian calendar ----

    @VisibleForTesting
    static List<String> yearFacts(int year) {
        requireRange(year, MIN_YEAR, MAX_YEAR);
        List<String> facts = new ArrayList<>();
        boolean leap = Year.isLeap(year);
        String leapFact;
        if (leap) {
            leapFact = String.format(Locale.US, "%d is a leap year, so it has 366 days.", year);
        } else if (year % 100 == 0) {
            leapFact = String.format(Locale.US, "%d is not a leap year: century years are only leap years when they are divisible by 400.", year);
        } else {
            leapFact = String.format(Locale.US, "%d is a common year of 365 days.", year);
        }
        // Before 1583 the Julian calendar was in use; say which rules the statement follows.
        facts.add(year >= FIRST_GREGORIAN_YEAR ? leapFact : "By Gregorian calendar rules, " + lowerFirst(leapFact));
        facts.add(String.format(Locale.US, "%d is in the %s century.", year, ordinal((year - 1) / 100 + 1)));
        if (year <= 3999) {
            facts.add(String.format(Locale.US, "In Roman numerals, %d is written as %s.", year, toRoman(year)));
        }
        if (year >= FIRST_GREGORIAN_YEAR) {
            LocalDate jan1 = LocalDate.of(year, 1, 1);
            facts.add(String.format(Locale.US, "In %d, January 1 falls on a %s and December 31 on a %s.",
                    year, dayName(jan1.getDayOfWeek()), dayName(LocalDate.of(year, 12, 31).getDayOfWeek())));
            int friday13 = 0;
            for (Month month : Month.values()) {
                if (LocalDate.of(year, month, 13).getDayOfWeek() == DayOfWeek.FRIDAY) friday13++;
            }
            facts.add(String.format(Locale.US, "%d has %s.", year,
                    plural(friday13, "Friday the 13th", "Fridays the 13th")));
            long isoWeeks = LocalDate.of(year, 6, 1).range(IsoFields.WEEK_OF_WEEK_BASED_YEAR).getMaximum();
            if (isoWeeks == 53) {
                facts.add(String.format(Locale.US, "%d has 53 ISO weeks instead of the usual 52.", year));
            }
        }
        return facts;
    }

    // ---- Dates: day-of-year arithmetic ----

    @VisibleForTesting
    static List<String> dateFacts(@NonNull MonthDay md, @NonNull LocalDate today) {
        List<String> facts = new ArrayList<>();
        String name = formatMonthDay(md);
        Month month = md.getMonth();
        if (md.getMonth() == Month.FEBRUARY && md.getDayOfMonth() == 29) {
            facts.add("February 29 only exists in leap years, where it is day 60 of the year.");
            int year = today.getYear();
            while (!Year.isLeap(year) || LocalDate.of(year, 2, 29).isBefore(today)) year++;
            facts.add(String.format(Locale.US, "The next February 29 is in %d, when it falls on a %s.",
                    year, dayName(LocalDate.of(year, 2, 29).getDayOfWeek())));
            return facts;
        }
        int dayOfYear = md.atYear(2023).getDayOfYear(); // 2023 is a common year
        if (month.getValue() > 2) {
            facts.add(String.format(Locale.US, "%s is day %d of the year, or day %d in leap years.",
                    name, dayOfYear, dayOfYear + 1));
        } else {
            facts.add(String.format(Locale.US, "%s is day %d of the year.", name, dayOfYear));
        }
        if (dayOfYear == 365) {
            facts.add("December 31 is the last day of the year.");
        } else {
            facts.add(String.format(Locale.US, "After %s, there are %s left in a common year.", name,
                    plural(365 - dayOfYear, "day", "days")));
        }
        facts.add(String.format(Locale.US, "%s is in %s, a month with %s.", name,
                month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                month == Month.FEBRUARY ? "28 days, or 29 in leap years"
                        : month.length(false) + " days"));
        int thisYear = today.getYear();
        facts.add(String.format(Locale.US, "In %d, %s falls on a %s.", thisYear, name,
                dayName(md.atYear(thisYear).getDayOfWeek())));
        facts.add(String.format(Locale.US, "In %d, %s falls on a %s.", thisYear + 1, name,
                dayName(md.atYear(thisYear + 1).getDayOfWeek())));
        return facts;
    }

    // ---- Helpers ----

    private static void requireRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(value + " is outside " + min + ".." + max);
        }
    }

    @VisibleForTesting
    static String toWords(int n) {
        if (n < 20) return ONES[n];
        if (n < 100) return TENS[n / 10] + (n % 10 != 0 ? "-" + ONES[n % 10] : "");
        if (n < 1000) return ONES[n / 100] + " hundred" + (n % 100 != 0 ? " " + toWords(n % 100) : "");
        return toWords(n / 1000) + " thousand" + (n % 1000 != 0 ? " " + toWords(n % 1000) : "");
    }

    @VisibleForTesting
    static String toRoman(int n) {
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (n >= values[i]) {
                n -= values[i];
                sb.append(symbols[i]);
            }
        }
        return sb.toString();
    }

    @VisibleForTesting
    static String ordinal(int n) {
        int mod100 = n % 100;
        String suffix;
        if (mod100 >= 11 && mod100 <= 13) suffix = "th";
        else if (n % 10 == 1) suffix = "st";
        else if (n % 10 == 2) suffix = "nd";
        else if (n % 10 == 3) suffix = "rd";
        else suffix = "th";
        return n + suffix;
    }

    private static String lowerFirst(String s) {
        // The sentences start with a year, so only letters need lowering.
        return Character.isLetter(s.charAt(0)) ? Character.toLowerCase(s.charAt(0)) + s.substring(1) : s;
    }

    private static String plural(int count, String one, String many) {
        return count + " " + (count == 1 ? one : many);
    }

    private static String dayName(DayOfWeek day) {
        return day.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private static int digitSum(int n) {
        int sum = 0;
        for (; n > 0; n /= 10) sum += n % 10;
        return sum;
    }

    private static int reverse(int n) {
        int r = 0;
        for (; n > 0; n /= 10) r = r * 10 + n % 10;
        return r;
    }

    private static boolean isPalindrome(int n) {
        return reverse(n) == n;
    }

    @VisibleForTesting
    static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int d = 2; (long) d * d <= n; d++) {
            if (n % d == 0) return false;
        }
        return true;
    }

    private static int primeIndex(int prime) {
        int count = 0;
        for (int i = 2; i <= prime; i++) if (isPrime(i)) count++;
        return count;
    }

    @VisibleForTesting
    static String factorize(int n) {
        StringBuilder sb = new StringBuilder();
        for (int p = 2; (long) p * p <= n; p++) {
            int exp = 0;
            while (n % p == 0) {
                n /= p;
                exp++;
            }
            if (exp > 0) appendFactor(sb, p, exp);
        }
        if (n > 1) appendFactor(sb, n, 1);
        return sb.toString();
    }

    private static void appendFactor(StringBuilder sb, int p, int exp) {
        if (sb.length() > 0) sb.append(" × ");
        sb.append(p);
        if (exp > 1) sb.append(superscript(exp));
    }

    private static String superscript(int n) {
        StringBuilder sb = new StringBuilder();
        for (char c : String.valueOf(n).toCharArray()) sb.append(SUPERSCRIPTS[c - '0']);
        return sb.toString();
    }

    private static int divisorCount(int n) {
        int count = 0;
        for (int d = 1; d <= n; d++) if (n % d == 0) count++;
        return count;
    }

    private static int divisorSum(int n) {
        int sum = 0;
        for (int d = 1; d <= n; d++) if (n % d == 0) sum += d;
        return sum;
    }

    private static boolean isFibonacci(int n) {
        int a = 0, b = 1;
        while (a < n) {
            int next = a + b;
            a = b;
            b = next;
        }
        return a == n;
    }

    /** Returns k when n = 1 + 2 + ... + k, otherwise 0. */
    private static int triangularIndex(int n) {
        int k = (int) Math.round((Math.sqrt(8.0 * n + 1) - 1) / 2);
        return k > 0 && k * (k + 1) / 2 == n ? k : 0;
    }

    /** Returns k when n = k!, for k >= 3 (1 and 2 are covered elsewhere), otherwise 0. */
    private static int factorialIndex(int n) {
        int f = 2;
        for (int k = 3; f < n; k++) {
            f *= k;
            if (f == n) return k;
        }
        return 0;
    }
}
