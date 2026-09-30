# QA build type only (never used by release): keep what instrumented tests reference, so the
# shrunk QA build can be tested. Everything else is shrunk and optimised exactly like release.
-keep class com.nip.numberinterestingfacts.R$* { *; }
-keepclassmembers class com.nip.numberinterestingfacts.RandomActivity { void onDatePicked(java.lang.Long); }
-keepclassmembers class com.nip.numberinterestingfacts.NumberFactsApp { *** getAdsManager(); }
-keepclassmembers class com.nip.numberinterestingfacts.ads.AdsManager {
    boolean isSdkReady();
    boolean isInterstitialLoaded();
    boolean isConsentCheckedThisSession();
    boolean canRequestAds();
    int consentStatus();
    boolean umpCanRequestAds();
    boolean privacyOptionsRequiredForTesting();
}
-keep class com.nip.numberinterestingfacts.DebugOverrides { public static *; }
# androidx.test's runner calls androidx.tracing from the app's classpath; the app itself does not
# use it, so R8 removes it. Test infrastructure only.
-keep class androidx.tracing.** { *; }
# The androidx.test/Espresso libraries in the test APK also resolve the Kotlin standard library
# from the app. Test infrastructure only.
-keep class kotlin.** { *; }
