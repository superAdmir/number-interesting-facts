package com.nip.numberinterestingfacts;

import android.app.Application;

import com.nip.numberinterestingfacts.ads.AdsManager;
import com.nip.numberinterestingfacts.settings.ThemePreferences;

public class NumberFactsApp extends Application {
    private AdsManager adsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        ThemePreferences.apply(ThemePreferences.get(this));
        adsManager = new AdsManager(this);
    }

    public AdsManager getAdsManager() {
        return adsManager;
    }
}
