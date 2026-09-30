package com.nip.numberinterestingfacts.facts.wiki;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nip.numberinterestingfacts.DebugOverrides;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.MonthDay;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Wikipedia page titles and MediaWiki Action API URLs (https://www.mediawiki.org/wiki/API:Parse).
 * The Action API is used rather than the REST "feed" endpoints, which Wikimedia is restructuring
 * from July 2026 (https://wikitech.wikimedia.org/wiki/API_Portal/Deprecation).
 */
public final class WikiApi {
    public static final String HOST = "https://en.wikipedia.org";
    private static final String API_PATH = "/w/api.php?action=parse&format=json&formatversion=2"
            + "&disableeditsection=1&disablelimitreport=1&redirects=1";

    private static String api() {
        String override = DebugOverrides.wikiHost();
        return (override != null ? override : HOST) + API_PATH;
    }

    private WikiApi() {
    }

    /** "AD 1969" resolves to the year article for every year from 1 onwards ("1969" is ambiguous for small years). */
    @NonNull
    public static String yearTitle(int year) {
        return "AD " + year;
    }

    /** Year articles are titled "1969", or "AD 5" for early years. */
    public static boolean isYearArticle(@Nullable String resolvedTitle, int year) {
        return String.valueOf(year).equals(resolvedTitle) || ("AD " + year).equals(resolvedTitle);
    }

    @NonNull
    public static String dateTitle(@NonNull MonthDay md) {
        return md.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + md.getDayOfMonth();
    }

    @NonNull
    public static String numberTitle(int number) {
        return number + " (number)";
    }

    @NonNull
    public static String sectionsUrl(@NonNull String title) {
        return api() + "&prop=sections%7Cproperties&page=" + encode(title);
    }

    @NonNull
    public static String sectionTextUrl(@NonNull String title, @NonNull String sectionIndex) {
        return api() + "&prop=text&section=" + encode(sectionIndex) + "&page=" + encode(title);
    }

    /** Public article URL used for attribution (CC BY-SA requires a link to the source). */
    @NonNull
    public static String articleUrl(@NonNull String title, @Nullable String fragment) {
        String url = HOST + "/wiki/" + encode(title.replace(' ', '_')).replace("%2F", "/");
        return fragment == null || fragment.isEmpty() ? url : url + "#" + encode(fragment);
    }

    private static String encode(String s) {
        try {
            return URLEncoder.encode(s.replace(' ', '_'), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
