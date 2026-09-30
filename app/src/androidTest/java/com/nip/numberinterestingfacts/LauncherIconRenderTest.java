package com.nip.numberinterestingfacts;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Renders the real launcher icon resources with Android's own drawables (no UI involved). */
@RunWith(AndroidJUnit4.class)
public class LauncherIconRenderTest {
    private static final int SIZE = 432; // 108 dp canvas at 4 px/dp

    @Test
    public void adaptiveIconRendersWithMasksAndMonochrome() throws IOException {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        for (int res : new int[]{R.mipmap.ic_launcher, R.mipmap.ic_launcher_round}) {
            Drawable d = ContextCompat.getDrawable(ctx, res);
            assertTrue("not adaptive: " + d, d instanceof AdaptiveIconDrawable);
        }
        AdaptiveIconDrawable icon = (AdaptiveIconDrawable) ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher);
        assertNotNull(icon.getForeground());
        assertNotNull(icon.getBackground());
        if (Build.VERSION.SDK_INT >= 33) assertNotNull("monochrome layer missing", icon.getMonochrome());

        File dir = new File(ctx.getFilesDir(), "icon-render");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        // Layers as Android draws them, on the full 108 dp canvas.
        save(dir, "layers-full-canvas", layers(icon.getBackground(), icon.getForeground()));
        // Launcher masks: circle, rounded square, squircle-like, and the system mask of this device.
        save(dir, "mask-circle", masked(icon, circle()));
        save(dir, "mask-rounded-square", masked(icon, rounded(0.22f)));
        save(dir, "mask-device-default", deviceMask(icon));
        if (Build.VERSION.SDK_INT >= 33) {
            save(dir, "monochrome-layer", layers(null, icon.getMonochrome()));
        }
        Log.i("LauncherIconRender", "rendered to " + dir);
    }

    private static Bitmap layers(Drawable bg, Drawable fg) {
        Bitmap b = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        if (bg != null) {
            bg.setBounds(0, 0, SIZE, SIZE);
            bg.draw(c);
        } else {
            c.drawColor(0xFF455A50);
        }
        fg.setBounds(0, 0, SIZE, SIZE);
        fg.draw(c);
        return b;
    }

    /** The visible 72 dp of the canvas, clipped to {@code mask}. */
    private static Bitmap masked(AdaptiveIconDrawable icon, Path mask) {
        return drawVisible(icon, mask, 288);
    }

    private static Bitmap drawVisible(AdaptiveIconDrawable icon, Path mask, int out) {
        Bitmap b = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        c.clipPath(mask);
        // The 108 dp layers are drawn so that their central 72 dp fill the output.
        int full = Math.round(out * 108f / 72f);
        int offset = (full - out) / 2;
        for (Drawable d : new Drawable[]{icon.getBackground(), icon.getForeground()}) {
            d.setBounds(-offset, -offset, full - offset, full - offset);
            d.draw(c);
        }
        return b;
    }

    private static Bitmap deviceMask(AdaptiveIconDrawable icon) {
        int out = 288;
        Bitmap b = Bitmap.createBitmap(out, out, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        icon.setBounds(0, 0, out, out);
        icon.draw(c); // uses the device's own icon mask
        return b;
    }

    private static Path circle() {
        Path p = new Path();
        p.addCircle(144, 144, 144, Path.Direction.CW);
        return p;
    }

    private static Path rounded(float radiusFraction) {
        Path p = new Path();
        p.addRoundRect(new RectF(0, 0, 288, 288), 288 * radiusFraction, 288 * radiusFraction, Path.Direction.CW);
        return p;
    }

    private static void save(File dir, String name, Bitmap b) throws IOException {
        try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
            b.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
