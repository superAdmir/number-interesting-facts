package com.nip.numberinterestingfacts.facts;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.MonthDay;
import java.util.Objects;

/**
 * What the user asked for: a category plus either a number (trivia, year, math) or a
 * month/day (date). A query with neither value means "surprise me".
 */
public final class FactQuery {
    @NonNull public final FactCategory category;
    @Nullable public final Integer number;
    @Nullable public final MonthDay monthDay;

    private FactQuery(@NonNull FactCategory category, @Nullable Integer number,
                      @Nullable MonthDay monthDay) {
        this.category = category;
        this.number = number;
        this.monthDay = monthDay;
    }

    public static FactQuery random(@NonNull FactCategory category) {
        return new FactQuery(category, null, null);
    }

    public static FactQuery ofNumber(@NonNull FactCategory category, int number) {
        if (category == FactCategory.DATE) {
            throw new IllegalArgumentException("Date facts need a month and day");
        }
        return new FactQuery(category, number, null);
    }

    public static FactQuery ofDate(@NonNull MonthDay monthDay) {
        return new FactQuery(FactCategory.DATE, null, monthDay);
    }

    public boolean isRandom() {
        return number == null && monthDay == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FactQuery)) return false;
        FactQuery that = (FactQuery) o;
        return category == that.category && Objects.equals(number, that.number)
                && Objects.equals(monthDay, that.monthDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(category, number, monthDay);
    }
}
