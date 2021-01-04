package co.nyzo.mobile.util;

import android.util.DisplayMetrics;

public class LayoutUtil {

    public static final float recommendedMarginSize = 16.0f;

    public static float size(float size, DisplayMetrics metrics) {
        return Math.min(metrics.scaledDensity * size * 1.5f, Math.min(metrics.widthPixels, metrics.heightPixels) *
                size / 320.0f);
    }
}
