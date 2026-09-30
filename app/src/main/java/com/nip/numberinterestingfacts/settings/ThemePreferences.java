package com.nip.numberinterestingfacts.settings;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * The app's only persisted setting. Versions up to 1.5 stored no data at all, so this file and
 * key are new; the stored values are stable identifiers and must not be renamed.
 */
public final class ThemePreferences {
    public static final String FILE_NAME = "settings";
    public static final String KEY_THEME = "theme_mode";

    public static final String THEME_SYSTEM = "system";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    private ThemePreferences() {
    }

    @NonNull
    public static String get(@NonNull Context context) {
        String value = prefs(context).getString(KEY_THEME, THEME_SYSTEM);
        return toNightMode(value) == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ? THEME_SYSTEM : value;
    }

    public static void set(@NonNull Context context, @NonNull String value) {
        prefs(context).edit().putString(KEY_THEME, value).apply();
        apply(value);
    }

    public static void apply(@NonNull String value) {
        AppCompatDelegate.setDefaultNightMode(toNightMode(value));
    }

    static int toNightMode(String value) {
        if (THEME_LIGHT.equals(value)) return AppCompatDelegate.MODE_NIGHT_NO;
        if (THEME_DARK.equals(value)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
    }
}
