package com.nip.numberinterestingfacts.facts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Outcome of a lookup. {@link #fact} is the Wikipedia content when found. {@link #calculated}
 * is always present but is only supplementary: the UI labels it and never presents it as
 * trivia or history.
 */
public final class FactResult {
    public enum Status {
        /** Wikipedia content found for the subject. */
        FOUND,
        /** Wikipedia was reached but has nothing suitable for this subject. */
        NOT_FOUND,
        /** Wikipedia could not be reached (offline, timeout, server or parse error). */
        NETWORK_ERROR
    }

    @NonNull public final Status status;
    /** The concrete query (random queries are resolved to a subject first). */
    @NonNull public final FactQuery query;
    @NonNull public final String subject;
    @Nullable public final Fact fact;
    @NonNull public final Fact calculated;

    public FactResult(@NonNull Status status, @NonNull FactQuery query, @NonNull String subject,
                      @Nullable Fact fact, @NonNull Fact calculated) {
        this.status = status;
        this.query = query;
        this.subject = subject;
        this.fact = fact;
        this.calculated = calculated;
    }
}
