package com.nip.numberinterestingfacts;

import android.content.ClipData;
import android.graphics.Rect;
import android.content.ActivityNotFoundException;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nip.numberinterestingfacts.ads.AdsManager;
import com.nip.numberinterestingfacts.facts.Fact;
import com.nip.numberinterestingfacts.facts.FactCategory;
import com.nip.numberinterestingfacts.facts.FactQuery;
import com.nip.numberinterestingfacts.facts.FactRepository;
import com.nip.numberinterestingfacts.facts.FactResult;
import com.nip.numberinterestingfacts.facts.LocalFactGenerator;
import com.nip.numberinterestingfacts.ui.EdgeToEdgeInsets;
import com.nip.numberinterestingfacts.ui.GridPaperDrawable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneOffset;

/**
 * The main screen and launcher activity (its name must stay the same so existing home-screen
 * shortcuts keep working). Hosts the Random, Year, Date and Math categories that used to be
 * four separate screens.
 */
public class RandomActivity extends AppCompatActivity {
    private static final String TAG = "RandomActivity";
    private static final String STATE_CATEGORY = "category";
    private static final String STATE_STATUS = "result_status";
    private static final String STATE_SUBJECT = "result_subject";
    private static final String STATE_FACT_TEXT = "fact_text";
    private static final String STATE_FACT_TITLE = "fact_source_title";
    private static final String STATE_FACT_URL = "fact_source_url";
    private static final String STATE_CALC_TEXT = "calculated_text";
    private static final String STATE_QUERY_NUMBER = "query_number";
    private static final String STATE_QUERY_DATE = "query_date";
    private static final String STATE_PICKED_DATE = "picked_date";
    private static final String DATE_PICKER_TAG = "date_picker";

    private final Object requestTag = new Object();
    private final Runnable loadBannerWhenReady = this::loadBanner;

    private FactRepository repository;
    private AdsManager adsManager;

    private View root;
    private ChipGroup categoryGroup;
    private TextView factEyebrow, factSubject, factText, factSource, lookupTitle;
    private TextView supplementLabel, supplementText;
    private View supplementBlock;
    private MaterialButton retryButton;
    private CircularProgressIndicator factProgress;
    private MaterialButton newFactButton, copyButton, shareButton;
    private TextInputLayout lookupInputLayout;
    private TextInputEditText lookupInput;
    private View adContainer;
    private FrameLayout adSlot;
    @Nullable private AdView adView;

    private FactCategory category = FactCategory.TRIVIA;
    @Nullable private FactResult currentResult;
    @Nullable private FactQuery lastQuery;
    @Nullable private MonthDay pickedDate;
    private int requestSequence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random);
        repository = new FactRepository(this);
        adsManager = ((NumberFactsApp) getApplication()).getAdsManager();

        root = findViewById(R.id.root);
        categoryGroup = findViewById(R.id.categoryGroup);
        factEyebrow = findViewById(R.id.factEyebrow);
        factSubject = findViewById(R.id.factSubject);
        factText = findViewById(R.id.factText);
        factSource = findViewById(R.id.factSource);
        supplementBlock = findViewById(R.id.supplementBlock);
        supplementLabel = findViewById(R.id.supplementLabel);
        supplementText = findViewById(R.id.supplementText);
        retryButton = findViewById(R.id.retryButton);
        factProgress = findViewById(R.id.factProgress);
        newFactButton = findViewById(R.id.newFactButton);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        lookupTitle = findViewById(R.id.lookupTitle);
        lookupInputLayout = findViewById(R.id.lookupInputLayout);
        lookupInput = findViewById(R.id.lookupInput);
        adContainer = findViewById(R.id.adContainer);
        adSlot = findViewById(R.id.adSlot);

        EdgeToEdgeInsets.apply(this, root, findViewById(R.id.appBar), findViewById(R.id.scroll), adContainer);
        decorateFactCard();
        ViewCompat.setAccessibilityHeading(factSubject, true);
        ViewCompat.setAccessibilityHeading(lookupTitle, true);
        setUpToolbar();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
        }
        checkChipFor(category);
        categoryGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) onCategorySelected(categoryForChip(checkedIds.get(0)));
        });
        updateLookupUi();

        newFactButton.setOnClickListener(v -> loadFact(FactQuery.random(category)));
        copyButton.setOnClickListener(v -> copyFact());
        shareButton.setOnClickListener(v -> shareFact());
        retryButton.setOnClickListener(v -> {
            if (lastQuery != null) loadFact(lastQuery);
        });
        factSource.setMovementMethod(LinkMovementMethod.getInstance());
        lookupInputLayout.setEndIconOnClickListener(v -> showDatePicker());
        lookupButton().setOnClickListener(v -> performLookup());
        lookupInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performLookup();
                return true;
            }
            return false;
        });
        reattachDatePickerListener();
        revealLookupAboveKeyboard();

        if (currentResult != null) {
            showResult(currentResult);
        } else {
            loadFact(FactQuery.random(category));
        }

        // Once per process (also after process death, when savedInstanceState is non-null).
        adsManager.gatherConsent(this, () -> { });
        adsManager.whenReady(loadBannerWhenReady);
    }

    private MaterialButton lookupButton() {
        return findViewById(R.id.lookupButton);
    }

    private void decorateFactCard() {
        MaterialCardView card = findViewById(R.id.factCard);
        float density = getResources().getDisplayMetrics().density;
        View inner = card.getChildAt(0);
        inner.setBackground(new GridPaperDrawable(
                ContextCompat.getColor(this, R.color.grid_line), 24 * density, Math.max(1f, density)));
    }

    private void setUpToolbar() {
        com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.action_privacy_policy) {
                startActivity(DocumentActivity.privacyPolicy(this));
                return true;
            } else if (id == R.id.action_exit) {
                confirmExit();
                return true;
            }
            return false;
        });
    }

    // ---- Categories ----

    private void onCategorySelected(@NonNull FactCategory selected) {
        if (selected == category) return;
        category = selected;
        lookupInput.setText(null);
        lookupInputLayout.setError(null);
        updateLookupUi();
        loadFact(FactQuery.random(category));
        if (adView != null) loadBanner();
        // Switching category is the natural break between pages where an interstitial may appear.
        adsManager.onCategorySwitched(this);
    }

    private FactCategory categoryForChip(int chipId) {
        if (chipId == R.id.chipYear) return FactCategory.YEAR;
        if (chipId == R.id.chipDate) return FactCategory.DATE;
        if (chipId == R.id.chipMath) return FactCategory.MATH;
        return FactCategory.TRIVIA;
    }

    private void checkChipFor(FactCategory c) {
        int id;
        switch (c) {
            case YEAR: id = R.id.chipYear; break;
            case DATE: id = R.id.chipDate; break;
            case MATH: id = R.id.chipMath; break;
            default: id = R.id.chipTrivia; break;
        }
        categoryGroup.check(id);
    }

    private void updateLookupUi() {
        boolean isDate = category == FactCategory.DATE;
        int title, hint, helper;
        switch (category) {
            case YEAR:
                title = R.string.lookup_title_year;
                hint = R.string.lookup_hint_year;
                helper = R.string.lookup_helper_year;
                break;
            case DATE:
                title = R.string.lookup_title_date;
                hint = R.string.lookup_hint_date;
                helper = R.string.lookup_helper_date;
                break;
            default:
                title = R.string.lookup_title_number;
                hint = R.string.lookup_hint_number;
                helper = R.string.lookup_helper_number;
                break;
        }
        lookupTitle.setText(title);
        lookupInputLayout.setHint(getString(hint));
        lookupInputLayout.setHelperText(getString(helper));

        if (isDate) {
            lookupInputLayout.setEndIconMode(TextInputLayout.END_ICON_CUSTOM);
            lookupInputLayout.setEndIconDrawable(R.drawable.ic_event_24);
            lookupInputLayout.setEndIconContentDescription(R.string.date_picker_title);
            lookupInputLayout.setStartIconDrawable(null);
            // The number field's 4-digit limit must not truncate "September 25".
            lookupInput.setFilters(new InputFilter[0]);
            lookupInput.setInputType(InputType.TYPE_NULL);
            lookupInput.setFocusable(false);
            lookupInput.setOnClickListener(v -> showDatePicker());
            lookupInput.setText(pickedDate != null ? LocalFactGenerator.formatMonthDay(pickedDate) : null);
        } else {
            lookupInputLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            lookupInput.setOnClickListener(null);
            lookupInputLayout.setStartIconDrawable(R.drawable.ic_search_24);
            lookupInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});
            lookupInput.setInputType(InputType.TYPE_CLASS_NUMBER);
            lookupInput.setFocusableInTouchMode(true);
            lookupInput.setFocusable(true);
        }
    }

    /**
     * When the keyboard opens for the lookup field, keep the whole lookup card (field, its helper
     * or error text, and Show fact) above the keyboard. The system on its own only reveals the
     * text cursor line, leaving the error message and the button hidden.
     */
    private void revealLookupAboveKeyboard() {
        View lookupCard = findViewById(R.id.lookupCard);
        boolean[] imeWasVisible = {false};
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            boolean imeVisible = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
            if (imeVisible && !imeWasVisible[0] && lookupInput.hasFocus()) {
                lookupCard.post(() -> lookupCard.requestRectangleOnScreen(
                        new Rect(0, 0, lookupCard.getWidth(), lookupCard.getHeight()), false));
            }
            imeWasVisible[0] = imeVisible;
        });
    }

    // ---- Facts ----

    private void performLookup() {
        lookupInputLayout.setError(null);
        FactQuery query;
        if (category == FactCategory.DATE) {
            if (pickedDate == null) {
                lookupInputLayout.setError(getString(R.string.error_pick_date));
                return;
            }
            query = FactQuery.ofDate(pickedDate);
        } else {
            int min = category == FactCategory.YEAR ? LocalFactGenerator.MIN_YEAR : 0;
            Integer value = parseNumber(lookupInput.getText() == null ? "" : lookupInput.getText().toString(), min);
            if (value == null) {
                lookupInputLayout.setError(getString(category == FactCategory.YEAR
                        ? R.string.error_enter_year : R.string.error_enter_number));
                return;
            }
            query = FactQuery.ofNumber(category, value);
        }
        hideKeyboard();
        loadFact(query);
    }

    /** Returns the number when the input is a whole number in [min, 9999], otherwise null. */
    @Nullable
    static Integer parseNumber(@NonNull String input, int min) {
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.length() > 4 || !trimmed.matches("\\d+")) return null;
        int value = Integer.parseInt(trimmed);
        return value >= min && value <= LocalFactGenerator.MAX_NUMBER ? value : null;
    }

    private void loadFact(@NonNull FactQuery query) {
        int sequence = ++requestSequence;
        lastQuery = query;
        setLoading(true);
        repository.load(query, requestTag, result -> {
            // Ignore answers to requests the user has already replaced.
            if (sequence != requestSequence || isFinishing() || isDestroyed()) return;
            showResult(result);
        });
    }

    private void setLoading(boolean loading) {
        factProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        factText.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        if (loading) {
            supplementBlock.setVisibility(View.GONE);
            retryButton.setVisibility(View.GONE);
        }
        newFactButton.setEnabled(!loading);
        boolean hasContent = !loading && currentResult != null;
        copyButton.setEnabled(hasContent);
        shareButton.setEnabled(hasContent);
    }

    private void showResult(@NonNull FactResult result) {
        currentResult = result;
        lastQuery = result.query;
        int eyebrow;
        switch (result.query.category) {
            case YEAR: eyebrow = R.string.eyebrow_year; break;
            case DATE: eyebrow = R.string.eyebrow_date; break;
            case MATH: eyebrow = R.string.eyebrow_math; break;
            default: eyebrow = R.string.eyebrow_trivia; break;
        }
        factEyebrow.setText(eyebrow);
        factSubject.setText(result.subject);
        // Long subjects (dates like "September 30") use a smaller display size.
        TextViewCompat.setTextAppearance(factSubject, R.style.TextAppearance_NumberFacts_Subject);
        if (result.subject.length() > 6) {
            factSubject.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        }

        Fact fact = result.fact;
        if (fact != null) {
            factText.setText(fact.text);
            factSource.setText(attribution(fact));
            supplementLabel.setText(R.string.supplement_label);
        } else {
            factText.setText(emptyStateMessage(result));
            factSource.setText(R.string.source_none);
            supplementLabel.setText(R.string.supplement_label_fallback);
        }
        supplementText.setText(result.calculated.text);
        setLoading(false);
        supplementBlock.setVisibility(View.VISIBLE);
        retryButton.setVisibility(result.status == FactResult.Status.NETWORK_ERROR ? View.VISIBLE : View.GONE);
    }

    private String emptyStateMessage(FactResult result) {
        if (result.status == FactResult.Status.NETWORK_ERROR) return getString(R.string.network_error);
        switch (result.query.category) {
            case YEAR: return getString(R.string.not_found_year, result.subject);
            case DATE: return getString(R.string.not_found_date, result.subject);
            default: return getString(R.string.not_found_number, result.subject);
        }
    }

    /**
     * "Source: Wikipedia, “Title” · CC BY-SA 4.0 · Excerpt, citations removed" with links to the
     * article and to the licence, as CC BY-SA 4.0 §3(a) and Wikipedia's reuse guidance require.
     */
    private CharSequence attribution(Fact fact) {
        SpannableStringBuilder sb = new SpannableStringBuilder(getString(R.string.source_wikipedia_prefix));
        appendLink(sb, getString(R.string.source_wikipedia_article, fact.sourceTitle), fact.sourceUrl);
        sb.append(" · ");
        appendLink(sb, getString(R.string.source_license), getString(R.string.license_url));
        sb.append(" · ").append(getString(R.string.source_modification));
        return sb;
    }

    private void appendLink(SpannableStringBuilder sb, String label, @Nullable String url) {
        int start = sb.length();
        sb.append(label);
        if (url == null) return;
        sb.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openUrl(url);
            }
        }, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, url, Toast.LENGTH_LONG).show();
        }
    }

    private void copyFact() {
        if (currentResult == null) return;
        ClipboardManager clipboard = ContextCompat.getSystemService(this, ClipboardManager.class);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), shareText(currentResult)));
        // Android 13+ shows its own confirmation.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareFact() {
        if (currentResult == null) return;
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, shareText(currentResult));
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)));
    }

    private String shareText(FactResult result) {
        Fact fact = result.fact;
        if (fact != null) {
            return getString(R.string.share_text_wikipedia_format, fact.subject, fact.text,
                    fact.sourceTitle, fact.sourceUrl);
        }
        // Without Wikipedia content only the labelled calculated fact is shared.
        return getString(R.string.share_text_calculated_format, result.calculated.subject, result.calculated.text);
    }

    // ---- Date picker ----

    private void showDatePicker() {
        if (getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG) != null) return;
        LocalDate initial = pickedDate != null ? pickedDate.atYear(LocalDate.now().getYear()) : LocalDate.now();
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.date_picker_title)
                .setSelection(initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        picker.addOnPositiveButtonClickListener(this::onDatePicked);
        picker.show(getSupportFragmentManager(), DATE_PICKER_TAG);
    }

    @SuppressWarnings("unchecked")
    private void reattachDatePickerListener() {
        // After rotation the picker is restored by the fragment manager but loses its listener.
        Object restored = getSupportFragmentManager().findFragmentByTag(DATE_PICKER_TAG);
        if (restored instanceof MaterialDatePicker) {
            ((MaterialDatePicker<Long>) restored).addOnPositiveButtonClickListener(this::onDatePicked);
        }
    }

    @VisibleForTesting
    void onDatePicked(Long selectionUtcMillis) {
        if (selectionUtcMillis == null) return;
        pickedDate = monthDayFromPickerSelection(selectionUtcMillis);
        lookupInput.setText(LocalFactGenerator.formatMonthDay(pickedDate));
        lookupInputLayout.setError(null);
        loadFact(FactQuery.ofDate(pickedDate));
    }

    /**
     * MaterialDatePicker reports the chosen day as UTC midnight, so it is converted in UTC:
     * the device time zone must not shift the day (e.g. UTC−10 would otherwise turn
     * March 14 into March 13).
     */
    static MonthDay monthDayFromPickerSelection(long selectionUtcMillis) {
        return MonthDay.from(Instant.ofEpochMilli(selectionUtcMillis).atZone(ZoneOffset.UTC).toLocalDate());
    }

    // ---- Exit, keyboard, ads ----

    private void confirmExit() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.exit_dialog_title)
                .setPositiveButton(R.string.exit_dialog_confirm, (d, w) -> finishAffinity())
                .setNegativeButton(R.string.exit_dialog_cancel, null)
                .show();
    }

    private void hideKeyboard() {
        View focus = getCurrentFocus();
        InputMethodManager imm = ContextCompat.getSystemService(this, InputMethodManager.class);
        if (focus != null && imm != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            focus.clearFocus();
        }
    }

    /** Each category keeps the banner unit its own screen used in version 1.5. */
    static int bannerUnitFor(@NonNull FactCategory c) {
        switch (c) {
            case YEAR: return R.string.admob_banner_year_unit_id;
            case DATE: return R.string.admob_banner_date_unit_id;
            case MATH: return R.string.admob_banner_math_unit_id;
            default: return R.string.admob_banner_random_unit_id;
        }
    }

    private void loadBanner() {
        if (isFinishing() || isDestroyed()) return;
        // Every request is gated on UMP's canRequestAds(), not on a stored or remembered choice.
        if (!adsManager.canRequestAds()) {
            removeBanner();
            return;
        }
        String override = DebugOverrides.adUnitId();
        String unitId = override != null ? override : getString(bannerUnitFor(category));
        if (adView != null) {
            if (unitId.equals(adView.getAdUnitId())) return;
            // Category changed: replace the banner with that category's unit (one request,
            // just like opening a separate category screen in 1.5).
            adSlot.removeView(adView);
            adView.destroy();
            adView = null;
        }
        AdView view = new AdView(this);
        view.setAdUnitId(unitId);
        // Same 320x50 footprint as version 1.5, to keep the placement unchanged.
        view.setAdSize(AdSize.BANNER);
        view.setAdListener(new AdListener() {
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError error) {
                // The slot simply stays hidden; the AdView retries at its next refresh or screen.
                Log.w(TAG, "Banner failed to load: " + error.getCode() + " " + error.getMessage());
            }

            @Override
            public void onAdLoaded() {
                if (adContainer.getVisibility() != View.VISIBLE) {
                    adContainer.setVisibility(View.VISIBLE);
                    ViewCompat.requestApplyInsets(root);
                }
            }
        });
        adSlot.addView(view, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER_HORIZONTAL));
        adView = view;
        view.loadAd(new AdRequest.Builder().build());
    }

    private void removeBanner() {
        if (adView == null) return;
        adSlot.removeView(adView);
        adView.destroy();
        adView = null;
        adContainer.setVisibility(View.GONE);
        ViewCompat.requestApplyInsets(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adView == null) return;
        // Consent can change in Settings → Privacy options while this screen is paused.
        if (adsManager.canRequestAds()) {
            adView.resume();
        } else {
            removeBanner();
        }
    }

    @Override
    protected void onPause() {
        if (adView != null) adView.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        repository.shutdown(requestTag);
        adsManager.cancelWhenReady(loadBannerWhenReady);
        if (adView != null) {
            adView.destroy();
            adView = null;
        }
        super.onDestroy();
    }

    // ---- State ----

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CATEGORY, category.name());
        FactResult r = currentResult;
        if (r != null && r.query.category == category) {
            outState.putString(STATE_STATUS, r.status.name());
            outState.putString(STATE_SUBJECT, r.subject);
            outState.putString(STATE_CALC_TEXT, r.calculated.text);
            if (r.query.number != null) outState.putInt(STATE_QUERY_NUMBER, r.query.number);
            if (r.query.monthDay != null) outState.putString(STATE_QUERY_DATE, r.query.monthDay.toString());
            if (r.fact != null) {
                outState.putString(STATE_FACT_TEXT, r.fact.text);
                outState.putString(STATE_FACT_TITLE, r.fact.sourceTitle);
                outState.putString(STATE_FACT_URL, r.fact.sourceUrl);
            }
        }
        if (pickedDate != null) outState.putString(STATE_PICKED_DATE, pickedDate.toString());
    }

    private void restoreState(@NonNull Bundle state) {
        try {
            category = FactCategory.valueOf(state.getString(STATE_CATEGORY, FactCategory.TRIVIA.name()));
            String picked = state.getString(STATE_PICKED_DATE);
            if (picked != null) pickedDate = MonthDay.parse(picked);
            String status = state.getString(STATE_STATUS);
            String subject = state.getString(STATE_SUBJECT);
            String calcText = state.getString(STATE_CALC_TEXT);
            if (status != null && subject != null && calcText != null) {
                FactQuery query;
                String date = state.getString(STATE_QUERY_DATE);
                if (date != null) query = FactQuery.ofDate(MonthDay.parse(date));
                else query = FactQuery.ofNumber(category, state.getInt(STATE_QUERY_NUMBER));
                String factText = state.getString(STATE_FACT_TEXT);
                Fact fact = factText == null ? null : new Fact(category, subject, factText,
                        Fact.Source.WIKIPEDIA, state.getString(STATE_FACT_TITLE), state.getString(STATE_FACT_URL));
                Fact calculated = new Fact(category, subject, calcText, Fact.Source.ON_DEVICE);
                currentResult = new FactResult(FactResult.Status.valueOf(status), query, subject, fact, calculated);
            }
        } catch (RuntimeException e) {
            // Corrupt or foreign state: start fresh rather than crash.
            category = FactCategory.TRIVIA;
            currentResult = null;
            pickedDate = null;
        }
    }
}
