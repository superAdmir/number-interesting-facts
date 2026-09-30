package com.nip.numberinterestingfacts.facts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nip.numberinterestingfacts.facts.wiki.WikiApi;
import com.nip.numberinterestingfacts.facts.wiki.WikiClient;
import com.nip.numberinterestingfacts.facts.wiki.WikiParser;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Single entry point for facts, restoring the original categories from Wikipedia:
 * <ul>
 *   <li>Random → trivia from the number's article ("42 (number)", or its section of a range
 *   article such as "400 (number)");</li>
 *   <li>Math → the article's mathematics section;</li>
 *   <li>Year → the "Events" section of the year article;</li>
 *   <li>Date → the "Events" section of the date article ("March 14").</li>
 * </ul>
 * Every result also carries a calculated fact, computed off the main thread, which the UI shows
 * only as a labelled supplement. Callbacks run on the main thread.
 */
public final class FactRepository {
    public interface Callback {
        void onResult(@NonNull FactResult result);
    }

    /** Random numbers without Wikipedia content are re-drawn a few times before giving up. */
    private static final int MAX_RANDOM_ATTEMPTS = 3;
    /** Number articles: how many candidate sections to try before reporting "not found". */
    private static final int MAX_SECTIONS_TRIED = 3;

    private final LocalFactGenerator local = new LocalFactGenerator(new Random(), Clock.systemDefaultZone());
    private final Random random = new Random();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final WikiClient wiki;
    private volatile boolean cancelled;

    public FactRepository(@NonNull Context context) {
        wiki = new WikiClient(context);
    }

    public void load(@NonNull FactQuery query, @NonNull Object tag, @NonNull Callback callback) {
        cancelled = false;
        attempt(query, 1, tag, callback);
    }

    public void cancel(@NonNull Object tag) {
        cancelled = true;
        wiki.cancel(tag);
        mainHandler.removeCallbacksAndMessages(null);
    }

    /** Stops background work; call when the owning screen is destroyed. */
    public void shutdown(@NonNull Object tag) {
        cancel(tag);
        worker.shutdownNow();
    }

    private void attempt(FactQuery original, int attemptNo, Object tag, Callback callback) {
        FactQuery query = local.resolveRandom(original);
        fetchWikipedia(query, tag, new WikiOutcome() {
            @Override
            public void found(@NonNull Fact fact) {
                deliver(FactResult.Status.FOUND, query, fact, callback);
            }

            @Override
            public void notFound() {
                if (original.isRandom() && attemptNo < MAX_RANDOM_ATTEMPTS) {
                    attempt(original, attemptNo + 1, tag, callback);
                } else {
                    deliver(FactResult.Status.NOT_FOUND, query, null, callback);
                }
            }

            @Override
            public void failed() {
                deliver(FactResult.Status.NETWORK_ERROR, query, null, callback);
            }
        });
    }

    private void deliver(FactResult.Status status, FactQuery query, @Nullable Fact fact, Callback callback) {
        if (cancelled || worker.isShutdown()) return;
        // Calculations are cheap (worst case well under 50 ms), but never run them on the UI thread.
        worker.execute(() -> {
            Fact calculated = local.generate(query);
            String subject = fact != null ? fact.subject : calculated.subject;
            FactResult result = new FactResult(status, query, subject, fact, calculated);
            mainHandler.post(() -> {
                if (!cancelled) callback.onResult(result);
            });
        });
    }

    // ---- Wikipedia lookups ----

    private interface WikiOutcome {
        void found(@NonNull Fact fact);

        void notFound();

        void failed();
    }

    private abstract static class Step<T> implements WikiClient.Callback<T> {
        final WikiOutcome outcome;

        Step(WikiOutcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public void onError(@NonNull Exception error) {
            outcome.failed();
        }
    }

    private void fetchWikipedia(FactQuery query, Object tag, WikiOutcome outcome) {
        switch (query.category) {
            case YEAR:
                fetchEvents(WikiApi.yearTitle(query.number), String.valueOf(query.number), query,
                        tag, outcome);
                break;
            case DATE:
                fetchEvents(WikiApi.dateTitle(query.monthDay),
                        LocalFactGenerator.formatMonthDay(query.monthDay), query, tag, outcome);
                break;
            default:
                fetchNumber(query, tag, outcome);
                break;
        }
    }

    private void fetchEvents(String title, String subject, FactQuery query, Object tag, WikiOutcome outcome) {
        wiki.get(WikiApi.sectionsUrl(title), tag, WikiParser::parseSections, new Step<WikiParser.SectionsPage>(outcome) {
            @Override
            public void onResult(@NonNull WikiParser.SectionsPage page) {
                // The resolved page must be exactly this year or date, never a redirect elsewhere
                // (e.g. far-future years redirect to a millennium overview).
                boolean matches = !page.disambiguation && (query.category == FactCategory.YEAR
                        ? WikiApi.isYearArticle(page.title, query.number)
                        : title.equals(page.title));
                String events = matches ? WikiParser.eventsSectionIndex(page) : null;
                if (events == null) {
                    outcome.notFound();
                    return;
                }
                boolean requireYear = query.category == FactCategory.DATE;
                wiki.get(WikiApi.sectionTextUrl(page.title, events), tag,
                        body -> WikiParser.parseEvents(WikiParser.textHtml(body), requireYear),
                        new Step<List<String>>(outcome) {
                            @Override
                            public void onResult(@NonNull List<String> items) {
                                if (items.isEmpty()) {
                                    outcome.notFound();
                                } else {
                                    outcome.found(new Fact(query.category, subject, pick(items),
                                            Fact.Source.WIKIPEDIA, page.title,
                                            WikiApi.articleUrl(page.title, "Events")));
                                }
                            }
                        });
            }
        });
    }

    private void fetchNumber(FactQuery query, Object tag, WikiOutcome outcome) {
        int n = query.number;
        boolean math = query.category == FactCategory.MATH;
        wiki.get(WikiApi.sectionsUrl(WikiApi.numberTitle(n)), tag, WikiParser::parseSections,
                new Step<WikiParser.SectionsPage>(outcome) {
                    @Override
                    public void onResult(@NonNull WikiParser.SectionsPage page) {
                        // Rejects missing pages, disambiguations and anything that is not a
                        // number article (e.g. a year article), so no wrong-subject facts appear.
                        List<String> sections = new ArrayList<>(WikiParser.numberSectionIndices(page, n, math));
                        if (sections.isEmpty()) {
                            outcome.notFound();
                            return;
                        }
                        if (page.redirectFragment == null) Collections.shuffle(sections, random);
                        trySection(page, sections, 0, query, tag, outcome);
                    }
                });
    }

    private void trySection(WikiParser.SectionsPage page, List<String> sections, int i, FactQuery query,
                            Object tag, WikiOutcome outcome) {
        if (i >= sections.size() || i >= MAX_SECTIONS_TRIED) {
            outcome.notFound();
            return;
        }
        int n = query.number;
        boolean leading = WikiParser.requiresLeadingNumber(page, n);
        wiki.get(WikiApi.sectionTextUrl(page.title, sections.get(i)), tag,
                body -> WikiParser.parseNumberFacts(WikiParser.textHtml(body), n, leading),
                new Step<List<String>>(outcome) {
                    @Override
                    public void onResult(@NonNull List<String> facts) {
                        if (facts.isEmpty()) {
                            trySection(page, sections, i + 1, query, tag, outcome);
                        } else {
                            String anchor = anchorOf(page, sections.get(i));
                            outcome.found(new Fact(query.category, String.valueOf(n), pick(facts),
                                    Fact.Source.WIKIPEDIA, page.title, WikiApi.articleUrl(page.title, anchor)));
                        }
                    }
                });
    }

    @Nullable
    private static String anchorOf(WikiParser.SectionsPage page, String index) {
        for (WikiParser.Section s : page.sections) if (s.index.equals(index)) return s.anchor;
        return null;
    }

    private String pick(List<String> items) {
        return items.get(random.nextInt(items.size()));
    }
}
