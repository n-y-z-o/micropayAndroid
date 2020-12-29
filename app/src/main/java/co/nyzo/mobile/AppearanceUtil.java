package co.nyzo.mobile;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

public class AppearanceUtil {

    public static final int colorValid = 0xffeeffee;
    public static final int colorInvalid = 0xffffeeee;

    public static void drawRoundedRectangleWithBorder(Canvas canvas, float left, float top,
                                                      float right, float bottom, float cornerRadius,
                                                      Paint fillPaint, Paint borderPaint) {

        float borderWidth = borderPaint.getStrokeWidth();

        RectF rectangle = new RectF(left + borderWidth / 2, top + borderWidth / 2,
                right - borderWidth / 2, bottom - borderWidth / 2);
        canvas.drawRoundRect(rectangle, cornerRadius, cornerRadius, fillPaint);
        canvas.drawRoundRect(rectangle, cornerRadius, cornerRadius, borderPaint);
    }
}
