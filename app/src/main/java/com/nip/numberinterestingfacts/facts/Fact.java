package com.nip.numberinterestingfacts.facts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** A fact ready for display, with its origin for attribution. */
public final class Fact {
    public enum Source {
        /** Verbatim excerpt from an English Wikipedia article (CC BY-SA 4.0). */
        WIKIPEDIA,
        /** Worked out on the device from the number or the Gregorian calendar. */
        ON_DEVICE
    }

    @NonNull public final FactCategory category;
    /** The headline shown large on the fact card, e.g. "1729" or "March 14". */
    @NonNull public final String subject;
    @NonNull public final String text;
    @NonNull public final Source source;
    /** Wikipedia article title, for attribution; null for on-device facts. */
    @Nullable public final String sourceTitle;
    /** Link to the source article; null for on-device facts. */
    @Nullable public final String sourceUrl;

    public Fact(@NonNull FactCategory category, @NonNull String subject, @NonNull String text,
                @NonNull Source source) {
        this(category, subject, text, source, null, null);
    }

    public Fact(@NonNull FactCategory category, @NonNull String subject, @NonNull String text,
                @NonNull Source source, @Nullable String sourceTitle, @Nullable String sourceUrl) {
        this.category = category;
        this.subject = subject;
        this.text = text;
        this.source = source;
        this.sourceTitle = sourceTitle;
        this.sourceUrl = sourceUrl;
    }
}
