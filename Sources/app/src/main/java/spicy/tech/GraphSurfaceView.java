package spicy.tech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread thread;
    private ScaleGestureDetector scaleDetector;

    public GraphSurfaceView(Context context) {
        super(context);
        init();
    }

    public GraphSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraphSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {

        getHolder().addCallback(this);

        scaleDetector = new ScaleGestureDetector(
                getContext(),
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {

                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {

                        if (thread != null) {

                            float factor = detector.getScaleFactor();

                            thread.updateTimeWindow(
                                    thread.getTimeWindow() / factor
                            );
                        }

                        return true;
                    }
                }
        );
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        scaleDetector.onTouchEvent(event);

        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        thread = new DrawThread(holder);
        thread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        if (thread != null) {

            thread.running = false;

            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }

            thread = null;
        }
    }

    private static class DrawThread extends Thread {

        private final SurfaceHolder holder;

        private final Paint signalPaint;
        private final Paint axisPaint;
        private final Paint gridPaint;
        private final Paint markerPaint;
        private final Paint textPaint;

        private final Path signalPath;

        private float elapsedTime = 0;
        private float timeWindow = 10.0f;
        private boolean running = true;

        private static final int X_TICKS = 10;
        private static final float Y_MIN = -1.2f;
        private static final float Y_MAX = 1.2f;

        DrawThread(SurfaceHolder holder) {

            this.holder = holder;

            signalPaint = createPaint(Color.BLUE, 4, Paint.Style.STROKE);

            axisPaint = createPaint(Color.BLACK, 2, Paint.Style.STROKE);

            gridPaint = createPaint(Color.GRAY, 1, Paint.Style.STROKE);

            gridPaint.setPathEffect(
                    new DashPathEffect(
                            new float[]{10, 10},
                            0
                    )
            );

            markerPaint = createPaint(Color.RED, 2, Paint.Style.STROKE);

            textPaint = createPaint(Color.MAGENTA, 24, Paint.Style.FILL);

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

            drawCurrentMarker(canvas, width, height, xMax);

            drawXLabels(canvas, width, height, xMin, xMax);
        }

        private void drawGrid(Canvas canvas, float width, float height) {

            for (float y = Y_MIN; y <= Y_MAX; y += 0.2f) {

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

            for (int pixel = 0; pixel < width; pixel++) {

                float time = xMin + pixel / width * (xMax - xMin);

                float y = (float) Math.sin(time);

                float screenY = mapY(y, height);

                if (pixel == 0) {

                    signalPath.moveTo(pixel, screenY);

                } else {

                    signalPath.lineTo(pixel, screenY);
                }
            }

            canvas.drawPath(signalPath, signalPaint);
        }

        private void drawCurrentMarker(Canvas canvas, float width, float height, float currentTime) {

            float y = (float) Math.sin(currentTime);

            float screenY = mapY(y, height);

            canvas.drawLine(width - 1, 0, width - 1, height, markerPaint);

            canvas.drawCircle(width - 1, screenY, 6, markerPaint);
        }

        private void drawXLabels(Canvas canvas, float width, float height, float xMin, float xMax) {

            for (int i = 0; i <= X_TICKS; i++) {

                float x = i * width / X_TICKS;

                float value = xMin + i * (xMax - xMin) / X_TICKS;

                canvas.drawText(
                        String.format("%.1f", value),
                        x - 36,
                        height - 10,
                        textPaint
                );
            }
        }

        private float mapY(float value, float height)
        {
            return height - ((value - Y_MIN) / (Y_MAX - Y_MIN)) * height;
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
}