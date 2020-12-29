package co.nyzo.mobile;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Path;

public class Icon {

    private Path path;
    private Paint paint;
    private float fillRatio;

    private Icon(Path path, Paint paint) {
        this(path, paint, 1.0f);
    }

    private Icon(Path path, Paint paint, float fillRatio) {
        this.path = path;
        this.paint = paint;
        this.fillRatio = fillRatio;
    }

    public Path getPath() {
        return path;
    }

    public Paint getPaint() {
        return paint;
    }

    public float getFillRatio() {
        return fillRatio;
    }

    public static Icon makeSettingsIcon(Context context) {
        return new Icon(makeSettingsIconPath(), makeSettingsIconPaint(context));
    }

    private static Path makeSettingsIconPath() {
        Path path = new Path();
        float r0 = 20.0f;
        float r1 = 30.0f;
        float r2 = 40.0f;
        float cx = r2;
        float cy = r2;

        // Add the center circle.
        path.addCircle(r2, r2, r0, Path.Direction.CW);

        // Add the gear teeth. For correct alignment, t + 2s + v = 2.
        int n = 7;
        double t = 0.8 / n;  // top of teeth
        double s = 0.2 / n;  // side slope
        double v = 0.8 / n;  // valley
        double angle = -Math.PI * t / 2.0;
        for (int i = 0; i < n; i++) {
            if (i == 0) {
                path.moveTo(cx + sin(angle) * r2, cy - cos(angle) * r2);
            } else {
                path.lineTo(cx + sin(angle) * r2, cy - cos(angle) * r2);
            }
            angle += Math.PI * t;
            path.lineTo(cx + sin(angle) * r2, cy - cos(angle) * r2);
            angle += Math.PI * s;
            path.lineTo(cx + sin(angle) * r1, cy - cos(angle) * r1);
            angle += Math.PI * v;
            path.lineTo(cx + sin(angle) * r1, cy - cos(angle) * r1);
            angle += Math.PI * s;

            if (i == n - 1) {
                path.close();
            }
        }

        return path;
    }

    private static Paint makeSettingsIconPaint(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setColor(0xff666666);
        paint.setStrokeWidth(Math.max(1.0f, density * 3.0f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);

        return paint;
    }

    public static Icon makeCancelIcon(Context context) {
        return new Icon(makeXIconPath(), makeCancelIconPaint(context), 0.7f);
    }

    public static Icon makeCloseIcon(Context context) {
        return new Icon(makeCheckIconPath(), makeCloseIconPaint(context), 0.7f);
    }

    private static Path makeXIconPath() {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        path.lineTo(1.0f, 1.0f);
        path.moveTo(0.0f, 1.0f);
        path.lineTo(1.0f, 0.0f);

        return path;
    }

    private static Paint makeCancelIconPaint(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setColor(0xffff0000);
        paint.setStrokeWidth(Math.max(1.0f, density * 8.0f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);

        return paint;
    }

    private static Paint makeCloseIconPaint(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setColor(0xff444444);
        paint.setStrokeWidth(Math.max(1.0f, density * 8.0f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);

        return paint;
    }

    public static Icon makeConfirmIcon(Context context) {
        return new Icon(makeCheckIconPath(), makeConfirmIconPaint(context), 0.8f);
    }

    private static Path makeCheckIconPath() {
        Path path = new Path();
        path.moveTo(0.0f, 0.6f);
        path.lineTo(0.3f, 1.0f);
        path.lineTo(0.8f, 0.0f);

        return path;
    }

    private static Paint makeConfirmIconPaint(Context context) {
        float density = context.getResources().getDisplayMetrics().density;

        Paint paint = new Paint();
        paint.setColor(0xff008000);
        paint.setStrokeWidth(Math.max(1.0f, density * 8.0f));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);

        return paint;
    }

    private static float sin(double angle) {
        return (float) Math.sin(angle);
    }

    private static float cos(double angle) {
        return (float) Math.cos(angle);
    }
}
