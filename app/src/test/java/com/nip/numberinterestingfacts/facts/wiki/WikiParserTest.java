package com.nip.numberinterestingfacts.facts.wiki;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.MonthDay;
import java.util.List;
import java.util.regex.Pattern;

/** Parser tests against real MediaWiki responses captured on 2026-09-25 (see resources/wiki). */
public class WikiParserTest {

    private static String fixture(String name) throws IOException {
        try (InputStream in = WikiParserTest.class.getResourceAsStream("/wiki/" + name)) {
            if (in == null) throw new IOException("missing fixture " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertClean(List<String> items) {
        for (String item : items) {
            assertFalse("citation left in: " + item, item.matches(".*\\[\\d+].*"));
            assertFalse("math markup left in: " + item, item.contains("\\displaystyle"));
            assertTrue("too long: " + item, item.length() <= WikiParser.MAX_EXCERPT);
            assertTrue("too short: " + item, item.length() >= WikiParser.MIN_EXCERPT);
        }
    }

    // ---- Year ----

    @Test
    public void yearArticleResolvesAndHasEvents() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("year_1969_sections.json"));
        assertEquals("1969", page.title);
        assertTrue(WikiApi.isYearArticle(page.title, 1969));
        assertEquals("1", WikiParser.eventsSectionIndex(page));
    }

    @Test
    public void yearEventsAreVerbatimAndDated() throws Exception {
        List<String> events = WikiParser.parseEvents(
                WikiParser.textHtml(fixture("year_1969_events.json")), false);
        assertTrue(events.size() > 50);
        assertTrue(events.contains("January 4 – The government of Spain hands over Ifni to Morocco."));
        assertClean(events);
    }

    @Test
    public void futureYearWithoutEventsIsNotFound() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("year_2027_sections.json"));
        assertNull(WikiParser.eventsSectionIndex(page));
    }

    @Test
    public void yearTitleRules() {
        assertEquals("AD 5", WikiApi.yearTitle(5));
        assertTrue(WikiApi.isYearArticle("AD 5", 5));
        assertTrue(WikiApi.isYearArticle("1969", 1969));
        assertFalse(WikiApi.isYearArticle("3rd millennium", 2100));
        assertFalse(WikiApi.isYearArticle("1969", 1970));
        assertFalse(WikiApi.isYearArticle(null, 1969));
    }

    // ---- Date ----

    @Test
    public void dateEventsAllStartWithAYear() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("date_0314_sections.json"));
        assertEquals(WikiApi.dateTitle(MonthDay.of(3, 14)), page.title);
        assertEquals("1", WikiParser.eventsSectionIndex(page));
        List<String> events = WikiParser.parseEvents(
                WikiParser.textHtml(fixture("date_0314_events.json")), true);
        assertTrue(events.size() > 20);
        assertTrue(events.stream().anyMatch(e -> e.startsWith("1074 – Battle of Mogyoród")));
        for (String e : events) assertTrue(e, e.matches("^\\d{1,4}( ?(BC|BCE|AD|CE))? ?[–—-] .+"));
        assertClean(events);
    }

    @Test
    public void february29HasItsOwnEvents() throws Exception {
        assertEquals("February 29", WikiApi.dateTitle(MonthDay.of(2, 29)));
        List<String> events = WikiParser.parseEvents(
                WikiParser.textHtml(fixture("date_0229_events.json")), true);
        assertTrue(events.size() > 20);
        assertClean(events);
    }

    @Test
    public void nestedListsKeepTheirDate() {
        String html = "<ul><li>July 20 –<ul><li>Apollo 11 lands on the Moon.<sup class=\"reference\">[5]</sup></li>"
                + "<li>A second event happens that day.</li></ul></li><li>July 21 – Another event.</li></ul>";
        List<String> events = WikiParser.parseEvents(html, false);
        assertEquals(List.of("July 20 – Apollo 11 lands on the Moon.", "July 20 – A second event happens that day.",
                "July 21 – Another event."), events);
    }

    // ---- Numbers ----

    @Test
    public void numberArticleSectionsSplitMathAndTrivia() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("number_42_sections.json"));
        assertEquals("42 (number)", page.title);
        List<String> math = WikiParser.numberSectionIndices(page, 42, true);
        List<String> trivia = WikiParser.numberSectionIndices(page, 42, false);
        assertEquals(List.of("1"), math);
        assertTrue(trivia.contains("3"));
        assertFalse(trivia.contains("1"));
        for (String boilerplate : new String[]{"7", "8"}) assertFalse(trivia.contains(boilerplate));
    }

    @Test
    public void numberFactsMentionTheNumberAndSkipFormulas() throws Exception {
        List<String> math = WikiParser.parseNumberFacts(WikiParser.textHtml(fixture("number_42_math.json")), 42);
        assertFalse(math.isEmpty());
        assertTrue(math.get(0).startsWith("42 is a pronic number"));
        Pattern mention = WikiParser.mentionPattern(42);
        for (String f : math) assertTrue(f, mention.matcher(f).find());
        assertClean(math);
        List<String> popular = WikiParser.parseNumberFacts(WikiParser.textHtml(fixture("number_42_popular.json")), 42);
        assertFalse(popular.isEmpty());
        for (String f : popular) assertTrue(f, mention.matcher(f).find());
        assertClean(popular);
    }

    @Test
    public void rangeArticleUsesTheNumbersOwnSection() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("number_437_sections.json"));
        assertEquals("400 (number)", page.title);
        assertEquals("437", page.redirectFragment);
        assertEquals(List.of("43"), WikiParser.numberSectionIndices(page, 437, false));
        assertEquals(List.of("43"), WikiParser.numberSectionIndices(page, 437, true));
        assertFalse(WikiParser.requiresLeadingNumber(page, 437));
        List<String> facts = WikiParser.parseNumberFacts(WikiParser.textHtml(fixture("number_437_section.json")), 437);
        assertFalse(facts.isEmpty());
        assertTrue(facts.get(0), facts.get(0).contains("19 × 23"));
    }

    @Test
    public void missingArticleIsReportedAsMissing() throws Exception {
        WikiParser.SectionsPage page = WikiParser.parseSections(fixture("number_4567_missing.json"));
        assertTrue(page.isMissing());
        assertTrue(WikiParser.numberSectionIndices(page, 42, false).isEmpty());
    }

    // ---- Wrong-subject guards ----

    private static String sectionsJson(String title, String fragment, String sections, String properties) {
        return "{\"parse\":{\"title\":\"" + title + "\""
                + (fragment == null ? "" : ",\"redirects\":[{\"from\":\"x\",\"to\":\"" + title
                + "\",\"tofragment\":\"" + fragment + "\"}]")
                + ",\"sections\":[" + sections + "]" + (properties == null ? "" : ",\"properties\":" + properties) + "}}";
    }

    private static String section(String index, String line, int level) {
        return "{\"index\":\"" + index + "\",\"line\":\"" + line + "\",\"anchor\":\"" + line.replace(' ', '_')
                + "\",\"toclevel\":" + level + "}";
    }

    @Test
    public void yearArticleIsNeverUsedAsANumberArticle() throws Exception {
        // e.g. a title that resolves to the year article "1969" instead of a number article.
        WikiParser.SectionsPage year = WikiParser.parseSections(sectionsJson("1969", null,
                section("1", "Events", 1) + "," + section("2", "Births", 1), null));
        assertFalse(WikiParser.isNumberPage(year, 1969));
        assertTrue(WikiParser.numberSectionIndices(year, 1969, false).isEmpty());
        // A different number's article is rejected too.
        WikiParser.SectionsPage other = WikiParser.parseSections(sectionsJson("6174", null, section("1", "Mathematics", 1), null));
        assertTrue(WikiParser.isNumberPage(other, 6174));
        assertFalse(WikiParser.isNumberPage(other, 6175));
        assertFalse(WikiParser.isNumberPage(WikiParser.parseSections(sectionsJson("Kaprekar's routine", null,
                section("1", "Definition", 1), null)), 6174));
    }

    @Test
    public void disambiguationPagesAreRejected() throws Exception {
        WikiParser.SectionsPage dab = WikiParser.parseSections(sectionsJson("42 (number)", null,
                section("1", "Uses", 1), "{\"disambiguation\":\"\"}"));
        assertTrue(dab.disambiguation);
        assertFalse(WikiParser.isNumberPage(dab, 42));
        WikiParser.SectionsPage dabArray = WikiParser.parseSections(sectionsJson("March 14", null,
                section("1", "Events", 1), "[{\"name\":\"disambiguation\",\"*\":\"\"}]"));
        assertTrue(dabArray.disambiguation);
    }

    @Test
    public void decadeSectionsPreferTheNumbersOwnSubsection() throws Exception {
        String sections = section("5", "610s", 2) + "," + section("6", "611", 3) + "," + section("7", "612", 3);
        WikiParser.SectionsPage page = WikiParser.parseSections(sectionsJson("600 (number)", "610s", sections, null));
        assertEquals(List.of("6"), WikiParser.numberSectionIndices(page, 611, false));
        assertFalse(WikiParser.requiresLeadingNumber(page, 611));
        // No own subsection (e.g. 1984 in "1900 to 1999"): facts must lead with the number.
        WikiParser.SectionsPage range = WikiParser.parseSections(sectionsJson("1000 (number)", "1900_to_1999",
                section("9", "1900 to 1999", 2), null));
        assertEquals(List.of("9"), WikiParser.numberSectionIndices(range, 1984, false));
        assertTrue(WikiParser.requiresLeadingNumber(range, 1984));
        String html = "<ul><li>1983 – a fact that mentions 1984 in passing.</li><li>1984 – the year of the novel.</li></ul>";
        assertEquals(List.of("1984 – the year of the novel."), WikiParser.parseNumberFacts(html, 1984, true));
        assertEquals(2, WikiParser.parseNumberFacts(html, 1984, false).size());
    }

    @Test
    public void mentionMatchesWholeNumbersOnly() {
        Pattern p = WikiParser.mentionPattern(1729);
        assertTrue(p.matcher("1729 is the Hardy–Ramanujan number.").find());
        assertTrue(p.matcher("the number 1,729 appears").find());
        assertTrue(p.matcher("ends with 1729.").find());
        assertFalse(p.matcher("17290 is different").find());
        assertFalse(p.matcher("11729 is different").find());
        assertFalse(p.matcher("1729.5 is not an integer").find());
        assertTrue(WikiParser.mentionPattern(0).matcher("0 is even").find());
        assertFalse(WikiParser.mentionPattern(0).matcher("10 and 2.0").find());
    }

    @Test
    public void longParagraphsAreCutAtSentenceBoundaries() {
        StringBuilder sb = new StringBuilder("42 is a special number. ");
        while (sb.length() < 800) sb.append("This sentence adds more words about it. ");
        String excerpt = WikiParser.excerpt(sb.toString().trim(), WikiParser.mentionPattern(42));
        assertTrue(excerpt.length() <= WikiParser.MAX_EXCERPT);
        assertTrue(excerpt.endsWith("."));
        assertTrue(excerpt.startsWith("42 is a special number."));
    }

    @Test
    public void articleUrlsAreHttpsAndEncoded() {
        assertEquals("https://en.wikipedia.org/wiki/1969#Events", WikiApi.articleUrl("1969", "Events"));
        assertEquals("https://en.wikipedia.org/wiki/42_%28number%29", WikiApi.articleUrl("42 (number)", null));
        assertTrue(WikiApi.sectionsUrl("March 14").startsWith("https://en.wikipedia.org/w/api.php?"));
        assertTrue(WikiApi.sectionsUrl("March 14").endsWith("&page=March_14"));
    }
}
