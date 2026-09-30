package com.nip.numberinterestingfacts.ui;

import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Edge-to-edge setup shared by all screens (required behaviour from targetSdk 35).
 * <ul>
 *   <li>The app bar absorbs the status bar and display cutout.</li>
 *   <li>A bottom navigation bar with <em>tappable</em> buttons (3-button navigation) is kept
 *   clear: the whole screen is padded above it, so no content or control is ever drawn beneath
 *   the system buttons, where a tap would reach the system instead of the app.</li>
 *   <li>A non-tappable gesture handle keeps the edge-to-edge look: content may scroll behind it
 *   and the scrolling view pads for it, so the end of the content can still be reached.</li>
 *   <li>The keyboard pushes the screen up so the focused field stays visible.</li>
 * </ul>
 * All values come from {@link WindowInsetsCompat}; nothing is device-specific.
 */
public final class EdgeToEdgeInsets {
    private EdgeToEdgeInsets() {
    }

    /**
     * @param bottomBar optional view pinned to the bottom (the ad slot). When it is visible it
     *                  pads for the gesture area; otherwise the scrolling content does.
     */
    public static void apply(@NonNull ComponentActivity activity, @NonNull View root,
                             @NonNull View appBar, @NonNull View scroll, @Nullable View bottomBar) {
        EdgeToEdge.enable(activity);
        final int scrollBottomBase = scroll.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets tappable = windowInsets.getInsets(WindowInsetsCompat.Type.tappableElement());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            boolean imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime());
            boolean bottomBarVisible = bottomBar != null && bottomBar.getVisibility() == View.VISIBLE;

            // Space reserved outside the content: the keyboard, or tappable system buttons.
            int reservedBottom = imeVisible ? ime.bottom : tappable.bottom;
            // Space the content scrolls behind: whatever is left of the bar (a gesture handle).
            int drawBehindBottom = imeVisible ? 0 : Math.max(0, bars.bottom - tappable.bottom);
            // Side navigation bars (3-button navigation in landscape) are tappable too.
            int left = Math.max(bars.left, tappable.left);
            int right = Math.max(bars.right, tappable.right);

            root.setPadding(0, 0, 0, reservedBottom);
            appBar.setPadding(left, bars.top, right, 0);
            scroll.setPadding(left, scroll.getPaddingTop(), right,
                    scrollBottomBase + (bottomBarVisible ? 0 : drawBehindBottom));
            if (bottomBar != null) {
                bottomBar.setPadding(left, 0, right, drawBehindBottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
