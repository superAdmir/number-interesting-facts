package com.nip.numberinterestingfacts;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

/**
 * Test hooks for instrumented tests of failure paths (no network, ad load failure) without
 * touching device settings. Every getter returns null unless this is a debug build, and
 * {@code BuildConfig.DEBUG} is a compile-time constant, so R8 removes the hooks from release.
 */
public final class DebugOverrides {
    private static volatile String wikiHost;
    private static volatile String adUnitId;

    private DebugOverrides() {
    }

    @VisibleForTesting
    public static void setWikiHost(@Nullable String host) {
        wikiHost = host;
    }

    @VisibleForTesting
    public static void setAdUnitId(@Nullable String unitId) {
        adUnitId = unitId;
    }

    @Nullable
    public static String wikiHost() {
        return BuildConfig.DEBUG ? wikiHost : null;
    }

    @Nullable
    public static String adUnitId() {
        return BuildConfig.DEBUG ? adUnitId : null;
    }
}
