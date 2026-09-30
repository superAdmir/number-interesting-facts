package com.nip.numberinterestingfacts.ads;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import com.nip.numberinterestingfacts.BuildConfig;
import com.nip.numberinterestingfacts.DebugOverrides;
import com.nip.numberinterestingfacts.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns consent (UMP), one-time Mobile Ads initialization, and interstitial pacing for the
 * whole process. No ad is requested before {@link ConsentInformation#canRequestAds()} is true
 * and the SDK has finished initializing.
 */
public final class AdsManager {
    private static final String TAG = "AdsManager";

    private final Context appContext;
    private final ConsentInformation consentInformation;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean initializationStarted = new AtomicBoolean(false);
    private final List<Runnable> whenReady = new ArrayList<>();
    private final InterstitialPacing pacing = new InterstitialPacing(SystemClock.elapsedRealtime());
    private boolean sdkReady;
    private boolean consentFlowRunning;
    /** True once this session's consent update (and form, if required) has finished. */
    private boolean consentCheckedThisSession;

    @Nullable private InterstitialAd interstitialAd;
    private boolean interstitialLoading;

    public AdsManager(@NonNull Context context) {
        appContext = context.getApplicationContext();
        consentInformation = UserMessagingPlatform.getConsentInformation(appContext);
    }

    /**
     * Refreshes consent status and shows the consent form when one is required. Runs once per
     * process; concurrent or repeated calls are ignored. {@code onFinished} runs on the main thread.
     */
    public void gatherConsent(@NonNull Activity activity, @NonNull Runnable onFinished) {
        if (!BuildConfig.ADS_ENABLED) {
            onFinished.run();
            return;
        }
        if (consentFlowRunning || consentCheckedThisSession) return;
        consentFlowRunning = true;
        ConsentRequestParameters.Builder builder = new ConsentRequestParameters.Builder();
        ConsentDebugSettings debugSettings = debugSettings(activity);
        if (debugSettings != null) builder.setConsentDebugSettings(debugSettings);
        ConsentRequestParameters params = builder.build();
        consentInformation.requestConsentInfoUpdate(activity, params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, formError -> {
                    logFormError(formError);
                    finishConsentFlow(onFinished);
                }),
                requestError -> {
                    // Offline or UMP unreachable: UMP's canRequestAds() then reflects the last
                    // stored consent state, which is what Google's guidance says to rely on.
                    logFormError(requestError);
                    finishConsentFlow(onFinished);
                });
        // Deliberately no early initialisation here: canRequestAds() would still report the
        // previous session's cached status (e.g. "not required" before the user entered the EEA).
    }

    public boolean isConsentCheckedThisSession() {
        return consentCheckedThisSession;
    }

    /** Debug builds only: lets QA force a consent region on a registered test device. */
    @Nullable
    private static ConsentDebugSettings debugSettings(Activity activity) {
        if (!BuildConfig.DEBUG || BuildConfig.UMP_DEBUG_GEOGRAPHY.isEmpty()
                || BuildConfig.UMP_TEST_DEVICE_ID.isEmpty()) {
            return null;
        }
        int geography;
        switch (BuildConfig.UMP_DEBUG_GEOGRAPHY) {
            case "EEA": geography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA; break;
            case "REGULATED_US_STATE": geography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_REGULATED_US_STATE; break;
            case "OTHER": geography = ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_OTHER; break;
            default: return null;
        }
        return new ConsentDebugSettings.Builder(activity)
                .setDebugGeography(geography)
                .addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_ID)
                .build();
    }

    private void finishConsentFlow(Runnable onFinished) {
        consentFlowRunning = false;
        consentCheckedThisSession = true;
        initializeIfAllowed();
        onFinished.run();
    }

    /** Ads may be requested only after this session's consent check and when UMP allows it. */
    public boolean canRequestAds() {
        return consentCheckedThisSession && consentInformation.canRequestAds();
    }

    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    public void showPrivacyOptionsForm(@NonNull Activity activity, @NonNull Runnable onDismissed) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, formError -> {
            logFormError(formError);
            initializeIfAllowed();
            onDismissed.run();
        });
    }

    /** Runs {@code action} on the main thread once ads may be requested. */
    public void whenReady(@NonNull Runnable action) {
        if (sdkReady && canRequestAds()) {
            action.run();
        } else {
            whenReady.add(action);
        }
    }

    public void cancelWhenReady(@NonNull Runnable action) {
        whenReady.remove(action);
    }

    private void initializeIfAllowed() {
        if (!BuildConfig.ADS_ENABLED || !canRequestAds()) return;
        if (!initializationStarted.compareAndSet(false, true)) return;
        // Initialization does disk and network work; Google recommends a background thread.
        new Thread(() -> MobileAds.initialize(appContext, status -> mainHandler.post(() -> {
            sdkReady = true;
            List<Runnable> pending = new ArrayList<>(whenReady);
            whenReady.clear();
            for (Runnable r : pending) r.run();
            loadInterstitialIfNeeded();
        })), "mobile-ads-init").start();
    }

    // ---- Interstitial: shown only at a category switch, never on launch or exit ----

    /**
     * Call when the user moves to another fact category (a natural break between pages).
     * Shows a preloaded interstitial only when the pacing rules allow it.
     */
    public void onCategorySwitched(@NonNull AppCompatActivity activity) {
        if (!sdkReady || !canRequestAds()) return;
        long now = SystemClock.elapsedRealtime();
        boolean eligible = pacing.recordSwitchAndCheck(now);
        if (!eligible || interstitialAd == null) {
            loadInterstitialIfNeeded();
            return;
        }
        if (!activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) return;
        InterstitialAd ad = interstitialAd;
        interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                loadInterstitialIfNeeded();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull com.google.android.gms.ads.AdError adError) {
                Log.w(TAG, "Interstitial failed to show: " + adError.getMessage());
                loadInterstitialIfNeeded();
            }
        });
        pacing.recordShown(now);
        ad.show(activity);
    }

    /** UMP consent status (ConsentInformation.ConsentStatus: 0 unknown, 1 not required, 2 required, 3 obtained). */
    @VisibleForTesting
    public int consentStatus() {
        return consentInformation.getConsentStatus();
    }

    /** UMP's own answer, without this app's "checked this session" condition. */
    @VisibleForTesting
    public boolean umpCanRequestAds() {
        return consentInformation.canRequestAds();
    }

    @VisibleForTesting
    public boolean privacyOptionsRequiredForTesting() {
        return isPrivacyOptionsRequired();
    }

    @VisibleForTesting
    public boolean isSdkReady() {
        return sdkReady;
    }

    @VisibleForTesting
    public boolean isInterstitialLoaded() {
        return interstitialAd != null;
    }

    private void loadInterstitialIfNeeded() {
        if (!sdkReady || !canRequestAds() || interstitialAd != null || interstitialLoading) return;
        interstitialLoading = true;
        String override = DebugOverrides.adUnitId();
        String unitId = override != null ? override : appContext.getString(R.string.admob_interstitial_unit_id);
        InterstitialAd.load(appContext, unitId,
                new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd ad) {
                        interstitialLoading = false;
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError error) {
                        // Retried lazily at the next category switch, never in a loop.
                        interstitialLoading = false;
                        Log.w(TAG, "Interstitial failed to load: " + error.getMessage());
                    }
                });
    }

    private static void logFormError(@Nullable FormError error) {
        if (error != null) {
            Log.w(TAG, "Consent: " + error.getErrorCode() + " " + error.getMessage());
        }
    }
}
