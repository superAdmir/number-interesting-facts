package com.nip.numberinterestingfacts;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void packageNameMatchesThePublishedApp() {
        // The template assertion expected "com.example.numberinterestingfacts", which never
        // matched this app; the published identity is com.nip.numberinterestingfacts.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.nip.numberinterestingfacts", appContext.getPackageName());
    }
}
