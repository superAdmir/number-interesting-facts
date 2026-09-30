package com.nip.numberinterestingfacts.facts.wiki;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses MediaWiki Action API ({@code action=parse}) responses into short, verbatim excerpts.
 * Text is never rewritten: markup, citation markers and formulas are removed, and items that
 * cannot be shown faithfully as plain text (e.g. containing math markup) are skipped.
 */
public final class WikiParser {
    /** Longest excerpt shown on the fact card. */
    static final int MAX_EXCERPT = 450;
    static final int MIN_EXCERPT = 15;

    private static final Pattern CITATION = Pattern.compile(
            "\\[(?:\\d+|[a-z]|note \\d+|citation needed|clarification needed|when\\?|who\\?|failed verification|better source needed|dubious – discuss|according to whom\\?)]");
    private static final Pattern DATE_EVENT = Pattern.compile("^\\d{1,4}(?:\\s?(?:BC|BCE|AD|CE))?\\s*[–—-]\\s+\\S.*");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?])\\s+(?=[A-Z0-9“\"(])");
    private static final char[] SUPERSCRIPTS = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};

    private WikiParser() {
    }

    /** One section entry of {@code prop=sections}. */
    public static final class Section {
        public final String index;
        public final String line;
        public final String anchor;
        public final int tocLevel;

        Section(String index, String line, String anchor, int tocLevel) {
            this.index = index;
            this.line = line;
            this.anchor = anchor;
            this.tocLevel = tocLevel;
        }
    }

    /** Result of {@code prop=sections|properties}: the resolved page and its sections. */
    public static final class SectionsPage {
        @Nullable public final String title;
        @Nullable public final String redirectFragment;
        @NonNull public final List<Section> sections;
        /** True for disambiguation pages, which never hold a fact about one subject. */
        public final boolean disambiguation;

        SectionsPage(@Nullable String title, @Nullable String redirectFragment, @NonNull List<Section> sections,
                     boolean disambiguation) {
            this.title = title;
            this.redirectFragment = redirectFragment;
            this.sections = sections;
            this.disambiguation = disambiguation;
        }

        public boolean isMissing() {
            return title == null;
        }
    }

    /** Parses {@code prop=sections}. A missing page yields {@link SectionsPage#isMissing()}. */
    @NonNull
    public static SectionsPage parseSections(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (root.has("error")) {
            String code = root.getJSONObject("error").optString("code");
            if ("missingtitle".equals(code) || "invalidtitle".equals(code)) {
                return new SectionsPage(null, null, Collections.emptyList(), false);
            }
            throw new JSONException("API error: " + code);
        }
        JSONObject parse = root.getJSONObject("parse");
        String fragment = null;
        JSONArray redirects = parse.optJSONArray("redirects");
        if (redirects != null && redirects.length() > 0) {
            String f = redirects.getJSONObject(redirects.length() - 1).optString("tofragment", "");
            if (!f.isEmpty()) fragment = f;
        }
        List<Section> sections = new ArrayList<>();
        JSONArray array = parse.optJSONArray("sections");
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                JSONObject s = array.getJSONObject(i);
                sections.add(new Section(s.optString("index"), Jsoup.parse(s.optString("line")).text(),
                        s.optString("anchor"), s.optInt("toclevel", 1)));
            }
        }
        boolean disambiguation = false;
        Object props = parse.opt("properties");
        if (props instanceof JSONObject) {
            disambiguation = ((JSONObject) props).has("disambiguation");
        } else if (props instanceof JSONArray) {
            JSONArray array2 = (JSONArray) props;
            for (int i = 0; i < array2.length(); i++) {
                if ("disambiguation".equals(array2.getJSONObject(i).optString("name"))) disambiguation = true;
            }
        }
        return new SectionsPage(parse.getString("title"), fragment, sections, disambiguation);
    }

    /** Index of the top-level "Events" section of a year or date article, or null. */
    @Nullable
    public static String eventsSectionIndex(@NonNull SectionsPage page) {
        for (Section s : page.sections) {
            if (s.tocLevel == 1 && "Events".equalsIgnoreCase(s.line)) return s.index;
        }
        return null;
    }

    private static boolean isBoilerplate(String line) {
        String l = line.toLowerCase(Locale.ROOT);
        return l.equals("references") || l.equals("external links") || l.equals("see also")
                || l.startsWith("notes") || l.equals("further reading") || l.equals("sources")
                || l.equals("bibliography") || l.equals("citations") || l.equals("footnotes");
    }

    private static boolean isMath(String line) {
        return line.toLowerCase(Locale.ROOT).contains("mathemat");
    }

    private static final Pattern NUMBER_ARTICLE = Pattern.compile("\\d+ \\(number\\)");

    /**
     * Whether a resolved page may supply facts about {@code number}: a number article
     * ("42 (number)", or a range article such as "400 (number)"), or a page titled exactly with
     * the number ("0", "6174") that is not a year/date article. Disambiguation pages never qualify.
     */
    public static boolean isNumberPage(@NonNull SectionsPage page, int number) {
        if (page.isMissing() || page.disambiguation) return false;
        if (NUMBER_ARTICLE.matcher(page.title).matches()) return true;
        return String.valueOf(number).equals(page.title) && eventsSectionIndex(page) == null;
    }

    /** Section holding exactly this number in a range article ("611" inside "610s"), or null. */
    @Nullable
    static String exactNumberSection(@NonNull SectionsPage page, int number) {
        String n = String.valueOf(number);
        for (Section s : page.sections) {
            if (n.equals(s.line) || n.equals(s.anchor)) return s.index;
        }
        return null;
    }

    /**
     * True when facts from this page must start with the number: range sections that cover
     * several numbers ("610s", "1900 to 1999") only give facts that lead with the number itself.
     */
    public static boolean requiresLeadingNumber(@NonNull SectionsPage page, int number) {
        return page.redirectFragment != null && exactNumberSection(page, number) == null;
    }

    /**
     * Candidate sections of a number article, in preference order. Range articles are reached
     * through a redirect fragment; the number's own subsection is preferred when it exists.
     */
    @NonNull
    public static List<String> numberSectionIndices(@NonNull SectionsPage page, int number, boolean math) {
        List<String> result = new ArrayList<>();
        if (!isNumberPage(page, number)) return result;
        if (page.redirectFragment != null) {
            String exact = exactNumberSection(page, number);
            if (exact != null) {
                result.add(exact);
                return result;
            }
            for (Section s : page.sections) {
                if (s.anchor.equals(page.redirectFragment)) result.add(s.index);
            }
            return result;
        }
        List<String> mathSections = new ArrayList<>();
        List<String> otherSections = new ArrayList<>();
        for (Section s : page.sections) {
            if (s.tocLevel != 1 || isBoilerplate(s.line)) continue;
            (isMath(s.line) ? mathSections : otherSections).add(s.index);
        }
        if (math) {
            result.addAll(mathSections);
            if (result.isEmpty()) result.addAll(otherSections);
        } else {
            result.addAll(otherSections);
            if (result.isEmpty()) result.addAll(mathSections);
        }
        return result;
    }

    /** Extracts the HTML of {@code prop=text}. */
    @NonNull
    public static String textHtml(@NonNull String json) throws JSONException {
        return new JSONObject(json).getJSONObject("parse").getString("text");
    }

    /**
     * Events from the "Events" section of a year article ("January 4 – …") or date article
     * ("1074 – …"). Nested lists keep their parent's date as a prefix.
     */
    @NonNull
    public static List<String> parseEvents(@NonNull String html, boolean requireLeadingYear) {
        Document doc = clean(html);
        List<String> events = new ArrayList<>();
        for (Element li : doc.select("li")) {
            if (!li.select("ul, ol").isEmpty()) continue; // parent of a nested list
            if (!li.select(".mwe-math-element").isEmpty()) continue;
            String text = normalise(li.text());
            Element parentLi = li.parent() != null ? li.parent().parent() : null;
            if (parentLi != null && "li".equals(parentLi.tagName())) {
                Element prefixCopy = parentLi.clone();
                prefixCopy.select("ul, ol").remove();
                String prefix = normalise(prefixCopy.text()).replaceAll("[\\s–—:-]+$", "");
                if (!prefix.isEmpty()) text = prefix + " – " + text;
            }
            if (text.length() < MIN_EXCERPT || text.length() > MAX_EXCERPT) continue;
            if (requireLeadingYear && !DATE_EVENT.matcher(text).matches()) continue;
            events.add(text);
        }
        return events;
    }

    /**
     * Paragraphs and list items of a number-article section that mention {@code number}.
     * Long paragraphs are cut at a sentence boundary, never mid-sentence.
     */
    @NonNull
    public static List<String> parseNumberFacts(@NonNull String html, int number) {
        return parseNumberFacts(html, number, false);
    }

    /** As above; with {@code requireLeading}, only facts that begin with the number are kept. */
    @NonNull
    public static List<String> parseNumberFacts(@NonNull String html, int number, boolean requireLeading) {
        Document doc = clean(html);
        Pattern mention = mentionPattern(number);
        Pattern leading = Pattern.compile("^(?:The )?(?:number )?" + mention.pattern());
        List<String> facts = new ArrayList<>();
        for (Element el : doc.select("p, li")) {
            if (el.tagName().equals("li") && !el.select("ul, ol").isEmpty()) continue;
            if (!el.select(".mwe-math-element").isEmpty()) continue;
            String text = normalise(el.text());
            if (!mention.matcher(text).find()) continue;
            String excerpt = excerpt(text, mention);
            if (excerpt == null) continue;
            if (requireLeading && !leading.matcher(excerpt).find()) continue;
            facts.add(excerpt);
        }
        return facts;
    }

    /** Matches the number as a whole token, e.g. 1729 or 1,729 but not 17290 or 1729.5. */
    @NonNull
    static Pattern mentionPattern(int number) {
        String plain = String.valueOf(number);
        String grouped = String.format(Locale.US, "%,d", number);
        String alternatives = plain.equals(grouped) ? Pattern.quote(plain)
                : Pattern.quote(plain) + "|" + Pattern.quote(grouped);
        return Pattern.compile("(?<![\\d.,])(?:" + alternatives + ")(?![\\d]|[.,]\\d)");
    }

    /** Shortest run of whole sentences that fits and still mentions the number. */
    @Nullable
    static String excerpt(@NonNull String text, @NonNull Pattern mention) {
        if (text.length() <= MAX_EXCERPT) return text.length() >= MIN_EXCERPT ? text : null;
        String[] sentences = SENTENCE_END.split(text);
        StringBuilder sb = new StringBuilder();
        for (String sentence : sentences) {
            if (sb.length() + sentence.length() + 1 > MAX_EXCERPT) break;
            if (sb.length() > 0) sb.append(' ');
            sb.append(sentence);
        }
        String result = sb.toString();
        return result.length() >= MIN_EXCERPT && mention.matcher(result).find() ? result : null;
    }

    private static Document clean(String html) {
        Document doc = Jsoup.parseBodyFragment(html);
        doc.select("sup.reference, style, script, link, .mw-editsection, table, figure, .thumb, "
                + ".hatnote, .shortdescription, .reflist, .navbox, .noprint, .mw-empty-elt, "
                + ".reference, .mw-ref").remove();
        // Keep simple exponents readable: 2<sup>10</sup> becomes 2¹⁰.
        for (Element sup : doc.select("sup")) {
            String t = sup.text().trim();
            sup.text(t.matches("\\d+") ? toSuperscript(t) : "^" + t);
        }
        return doc;
    }

    private static String toSuperscript(String digits) {
        StringBuilder sb = new StringBuilder();
        for (char c : digits.toCharArray()) sb.append(SUPERSCRIPTS[c - '0']);
        return sb.toString();
    }

    private static String normalise(String text) {
        String t = text.replace(' ', ' ');
        Matcher m = CITATION.matcher(t);
        t = m.replaceAll("");
        return t.replaceAll("\\s+", " ").replaceAll(" ([,.;:])", "$1").trim();
    }
}
