package com.nip.numberinterestingfacts.facts;

/** The four fact categories the app has always offered. */
public enum FactCategory {
    TRIVIA("trivia"),
    YEAR("year"),
    DATE("date"),
    MATH("math");

    /** Path segment used by the Numbers API style endpoint. */
    public final String apiPath;

    FactCategory(String apiPath) {
        this.apiPath = apiPath;
    }
}
