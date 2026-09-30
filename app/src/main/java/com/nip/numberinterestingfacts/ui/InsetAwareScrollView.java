package com.nip.numberinterestingfacts.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;

/**
 * A NestedScrollView for edge-to-edge screens. Its top/bottom padding holds the system-bar insets
 * and, with {@code clipToPadding="false"}, content scrolls behind the bars. The framework still
 * treats that padding as visible when it scrolls a child into view (focus changes, keyboard,
 * TalkBack, {@code requestRectangleOnScreen}), which could park a button behind the navigation
 * bar. Here the padding counts as covered, so such requests stop above the bars.
 */
public class InsetAwareScrollView extends NestedScrollView {

    public InsetAwareScrollView(@NonNull Context context) {
        super(context);
    }

    public InsetAwareScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InsetAwareScrollView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        if (getChildCount() == 0) return 0;
        View child = getChildAt(0);
        int bottomMargin = child.getLayoutParams() instanceof ViewGroup.MarginLayoutParams
                ? ((ViewGroup.MarginLayoutParams) child.getLayoutParams()).bottomMargin : 0;
        int contentBottom = child.getBottom() + bottomMargin;

        int scrollY = getScrollY();
        int screenTop = scrollY + getPaddingTop();
        int screenBottom = scrollY + getHeight() - getPaddingBottom();
        int fadingEdge = getVerticalFadingEdgeLength();
        if (rect.top > 0) screenTop += fadingEdge;
        if (rect.bottom < contentBottom) screenBottom -= fadingEdge;
        int visible = screenBottom - screenTop;

        int delta = 0;
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            delta = rect.height() > visible ? rect.top - screenTop : rect.bottom - screenBottom;
        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            delta = rect.height() > visible ? rect.bottom - screenBottom : rect.top - screenTop;
        }
        // Clamp to the real scroll range, which includes the bottom padding.
        int maxScroll = Math.max(0, contentBottom + getPaddingBottom() - getHeight());
        int target = Math.max(0, Math.min(scrollY + delta, maxScroll));
        return target - scrollY;
    }
}
