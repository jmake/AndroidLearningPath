package spicy.tech.plotter;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.SurfaceHolder;

class DrawThread extends Thread {

    private final SurfaceHolder holder;
    private final FunctionView functionView;

    private final Paint signalPaint;
    private final Paint axisPaint;
    private final Paint gridPaint;
    private final Paint markerPaint;
    private final Paint startPaint;
    private final Paint textPaint;
    private final Paint timePaint;
    private final Path signalPath;

    private float elapsedTime = 0;
    private float timeWindow = 10.0f;
    boolean running = true;

    private static final int X_TICKS = 10;

    DrawThread(SurfaceHolder holder, FunctionView functionView) {
        this.holder = holder;
        this.functionView = functionView;
        signalPaint = createPaint(functionView.getColor(), 4, Paint.Style.STROKE);
        axisPaint = createPaint(Color.BLACK, 2, Paint.Style.STROKE);
        gridPaint = createPaint(Color.GRAY, 1, Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        markerPaint = createPaint(Color.RED, 2, Paint.Style.STROKE);
        startPaint = createPaint(Color.GREEN, 2, Paint.Style.STROKE);
        textPaint = createPaint(Color.MAGENTA, 24, Paint.Style.FILL);
        timePaint = createPaint(Color.MAGENTA, 48, Paint.Style.FILL);
        signalPath = new Path();
    }

    public synchronized void updateTimeWindow(float value) {
        timeWindow = Math.max(1.0f, Math.min(value, 60.0f));
    }

    public synchronized float getTimeWindow() {
        return timeWindow;
    }

    @Override
    public void run() {
        while (running) {
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawFrame(canvas);
                    elapsedTime += 0.016f;
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }
            try {
                sleep(16);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void drawFrame(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        float xMin;
        float xMax;
        synchronized (this) {
            xMin = elapsedTime - timeWindow;
            xMax = elapsedTime;
        }
        drawGrid(canvas, width, height);
        drawAxis(canvas, width, height);
        drawSignal(canvas, width, height, xMin, xMax);
        drawStartLine(canvas, width, height, xMin, xMax);
        drawCurrentMarker(canvas, width, height, xMax);
        drawXLabels(canvas, width, height, xMin, xMax);
        drawElapsedTime(canvas, elapsedTime);
    }

    private void drawElapsedTime(Canvas canvas, float time) {
        canvas.drawText(String.format("Time: %.1f s", time), 40, 60, timePaint);
    }

    private void drawGrid(Canvas canvas, float width, float height) {
        for (float y = functionView.getYMin(); y <= functionView.getYMax(); y += 0.2f) {
            float screenY = mapY(y, height);
            canvas.drawLine(0, screenY, width, screenY, gridPaint);
        }
        for (int i = 0; i <= X_TICKS; i++) {
            float x = i * width / X_TICKS;
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
    }

    private void drawAxis(Canvas canvas, float width, float height) {
        float zeroY = mapY(0, height);
        canvas.drawLine(0, zeroY, width, zeroY, axisPaint);
        canvas.drawLine(0, 0, 0, height, axisPaint);
    }

    private void drawSignal(Canvas canvas, float width, float height, float xMin, float xMax) {
        signalPath.reset();
        boolean started = false;
        for (int pixel = 0; pixel < width; pixel++) {
            float time = xMin + pixel / width * (xMax - xMin);
            if (time < 0) continue;
            float y = functionView.evaluate(time);
            float screenY = mapY(y, height);
            if (!started) {
                signalPath.moveTo(pixel, screenY);
                started = true;
            } else {
                signalPath.lineTo(pixel, screenY);
            }
        }
        canvas.drawPath(signalPath, signalPaint);
    }

    private void drawStartLine(Canvas canvas, float width, float height, float xMin, float xMax) {
        if (0 < xMin || 0 > xMax) return;
        float screenX = (0 - xMin) / (xMax - xMin) * width;
        canvas.drawLine(screenX, 0, screenX, height, startPaint);
    }

    private void drawCurrentMarker(Canvas canvas, float width, float height, float currentTime) {
        float y = functionView.evaluate(currentTime);
        float screenY = mapY(y, height);
        canvas.drawLine(width - 1, 0, width - 1, height, markerPaint);
        canvas.drawCircle(width - 1, screenY, 6, markerPaint);
    }

    private void drawXLabels(Canvas canvas, float width, float height, float xMin, float xMax) {
        for (int i = 0; i <= X_TICKS; i++) {
            float x = i * width / X_TICKS;
            float value = xMin + i * (xMax - xMin) / X_TICKS;
            canvas.drawText(String.format("%.1f", value), x - 36, height - 10, textPaint);
        }
    }

    private float mapY(float value, float height) {
        return height - ((value - functionView.getYMin()) / (functionView.getYMax() - functionView.getYMin())) * height;
    }

    private Paint createPaint(int color, float size, Paint.Style style) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setStrokeWidth(size);
        paint.setTextSize(size);
        paint.setStyle(style);
        return paint;
    }
}