package com.nip.numberinterestingfacts.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Faint graph-paper grid used behind the fact card, echoing the launcher icon. Drawn in code so
 * it stays crisp at every density and costs no bitmap memory.
 */
public final class GridPaperDrawable extends Drawable {
    private final Paint paint = new Paint();
    private final float spacingPx;

    public GridPaperDrawable(int lineColor, float spacingPx, float strokePx) {
        this.spacingPx = spacingPx;
        paint.setColor(lineColor);
        paint.setStrokeWidth(strokePx);
        paint.setAntiAlias(false);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect b = getBounds();
        for (float x = b.left + spacingPx; x < b.right; x += spacingPx) {
            canvas.drawLine(x, b.top, x, b.bottom, paint);
        }
        for (float y = b.top + spacingPx; y < b.bottom; y += spacingPx) {
            canvas.drawLine(b.left, y, b.right, y, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @SuppressWarnings("deprecation") // Still abstract in Drawable; required to override.
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
