package com.nip.numberinterestingfacts;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nip.numberinterestingfacts.ads.AdsManager;
import com.nip.numberinterestingfacts.settings.ThemePreferences;
import com.nip.numberinterestingfacts.ui.EdgeToEdgeInsets;

public class SettingsActivity extends AppCompatActivity {
    private static final String[] THEME_VALUES = {
            ThemePreferences.THEME_SYSTEM, ThemePreferences.THEME_LIGHT, ThemePreferences.THEME_DARK};
    private static final int[] THEME_LABELS = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};

    private AdsManager adsManager;
    private View privacyOptionsRow;
    private TextView themeSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        adsManager = ((NumberFactsApp) getApplication()).getAdsManager();
        EdgeToEdgeInsets.apply(this, findViewById(R.id.root), findViewById(R.id.appBar),
                findViewById(R.id.scroll), null);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        for (int id : new int[]{R.id.header1, R.id.header2, R.id.header3}) {
            ViewCompat.setAccessibilityHeading(findViewById(id), true);
        }
        themeSummary = findViewById(R.id.themeSummary);
        findViewById(R.id.themeRow).setOnClickListener(v -> showThemeDialog());
        updateThemeSummary();

        privacyOptionsRow = findViewById(R.id.privacyOptionsRow);
        privacyOptionsRow.setOnClickListener(v ->
                adsManager.showPrivacyOptionsForm(this, this::updatePrivacyOptionsRow));
        findViewById(R.id.privacyPolicyRow).setOnClickListener(v ->
                startActivity(DocumentActivity.privacyPolicy(this)));

        findViewById(R.id.licensesRow).setOnClickListener(v ->
                startActivity(DocumentActivity.licenses(this)));

        TextView version = findViewById(R.id.versionSummary);
        version.setText(BuildConfig.VERSION_NAME);
        TextView sourceNote = findViewById(R.id.factSourceNote);
        sourceNote.setText(R.string.settings_fact_source);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePrivacyOptionsRow();
    }

    private void updatePrivacyOptionsRow() {
        // UMP decides whether a privacy options entry point is required (e.g. EEA/UK users).
        privacyOptionsRow.setVisibility(adsManager.isPrivacyOptionsRequired() ? View.VISIBLE : View.GONE);
    }

    private void updateThemeSummary() {
        themeSummary.setText(THEME_LABELS[indexOfTheme(ThemePreferences.get(this))]);
    }

    private void showThemeDialog() {
        CharSequence[] labels = new CharSequence[THEME_LABELS.length];
        for (int i = 0; i < labels.length; i++) labels[i] = getString(THEME_LABELS[i]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.settings_theme)
                .setSingleChoiceItems(labels, indexOfTheme(ThemePreferences.get(this)), (dialog, which) -> {
                    dialog.dismiss();
                    ThemePreferences.set(this, THEME_VALUES[which]);
                    updateThemeSummary();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static int indexOfTheme(String value) {
        for (int i = 0; i < THEME_VALUES.length; i++) if (THEME_VALUES[i].equals(value)) return i;
        return 0;
    }
}
