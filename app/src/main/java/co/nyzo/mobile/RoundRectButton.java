package co.nyzo.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class RoundRectButton extends View implements View.OnTouchListener {

    private float padding;
    private Paint borderPaint;
    private Paint fillPaintInactive;
    private Paint fillPaintActive;
    private Icon icon;
    private String label;
    private Paint labelPaint;

    private boolean touchDown;
    private boolean selected;

    public RoundRectButton(Context context) {
        super(context);

        setBackgroundColor(Color.TRANSPARENT);

        float density = context.getResources().getDisplayMetrics().density;
        padding = Math.max(1.0f, density * 2.0f);

        borderPaint = new Paint();
        borderPaint.setColor(0xff333333);
        borderPaint.setStrokeWidth(Math.max(1.0f, density));
        borderPaint.setStyle(Paint.Style.STROKE);

        fillPaintInactive = new Paint();
        fillPaintInactive.setColor(0xffffffff);

        fillPaintActive = new Paint();
        fillPaintActive.setColor(0xffcccccc);

        labelPaint = new Paint();
        labelPaint.setAntiAlias(true);
        labelPaint.setColor(Color.BLACK);

        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        int action = event.getActionMasked();
        boolean previousTouchDown = touchDown;
        if (action == MotionEvent.ACTION_DOWN) {
            touchDown = true;
        } else if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            touchDown = false;
        }

        // If the selected state has changed, invalidate.
        if (touchDown != previousTouchDown) {
            invalidate();
        }

        // If the action is an "up" action within bounds, call the on-click listener. This is the
        // simplest solution for capturing the event and maintaining on-click behavior.
        if (action == MotionEvent.ACTION_UP) {
            float x = event.getX();
            float y = event.getY();
            if (x >= 0.0f && x < getWidth() && y >= 0.0f && y < getHeight()) {
                callOnClick();
            }
        }

        // Return true. This consumes the event, and it ensures that later events are delivered.
        return true;
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        invalidate();
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        float width = getWidth();
        float height = getHeight();
        float density = getContext().getResources().getDisplayMetrics().density;
        float cornerRadius = Math.max(1, (int) (density * 10.0f));

        // Draw the background and border.
        AppearanceUtil.drawRoundedRectangleWithBorder(canvas, padding, padding, width - padding,
                height - padding, cornerRadius, selected || touchDown ? fillPaintActive :
                        fillPaintInactive, borderPaint);

        float textHeight = 0.0f;
        if (label != null && !label.trim().isEmpty()) {
            labelPaint.setTextSize(12.0f);
            float labelUnscaledWidth = labelPaint.measureText(label);
            float maximumTextHeight = icon == null ? height * 0.5f : height * 0.23f;
            textHeight = Math.min(maximumTextHeight, 12.0f * 0.80f * width / labelUnscaledWidth);
            labelPaint.setTextSize(textHeight);
            float labelWidth = labelPaint.measureText(label);
            float y0 = icon == null ? height * 0.5f + textHeight * 0.4f :
                    height - textHeight * 0.5f;
            canvas.drawText(label, (width - labelWidth) * 0.5f, y0, labelPaint);
        }

        if (icon != null) {
            float iconHeight = height - textHeight;
            canvas.drawPath(getScaledPath(icon.getPath(), width, iconHeight, icon.getFillRatio()),
                    icon.getPaint());
        }
    }

    public void setIcon(Icon icon) {
        this.icon = icon;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    private static Path getScaledPath(Path path, float width, float height, float fillRatio) {
        RectF pathBounds = new RectF();
        path.computeBounds(pathBounds, true);

        float adjustedFillRatio = 0.6f * fillRatio;
        float offset = (1.0f - adjustedFillRatio) / 2.0f;

        RectF buttonBounds = new RectF(width * offset, height * offset, width * (1.0f - offset),
                height * (1.0f - offset));

        Matrix matrix = new Matrix();
        matrix.setRectToRect(pathBounds, buttonBounds, Matrix.ScaleToFit.CENTER);

        Path scaledPath = new Path();
        path.transform(matrix, scaledPath);

        return scaledPath;
    }
}
