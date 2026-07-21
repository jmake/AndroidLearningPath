package spicy.tech;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread thread;

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
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new DrawThread(holder);
        thread.start();
    }

    @Override
    public void surfaceChanged(
            SurfaceHolder holder,
            int format,
            int width,
            int height) {
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

        private boolean running = true;

        private float t = 0;


        private final Paint signalPaint;
        private final Paint axisPaint;
        private final Paint gridPaint;
        private final Paint markerPaint;
        private final Paint textPaint;

        private final Path signalPath;


        private static final float Y_MIN = -1.2f;
        private static final float Y_MAX = 1.2f;

        private static final float X_MIN = 0f;
        private static final float X_MAX = 10f;

        private static final int X_TICKS = 10;


        DrawThread(SurfaceHolder holder) {

            this.holder = holder;

            signalPaint = createPaint(
                    Color.BLUE,
                    4,
                    Paint.Style.STROKE
            );

            axisPaint = createPaint(
                    Color.BLACK,
                    2,
                    Paint.Style.STROKE
            );

            gridPaint = createPaint(
                    Color.GRAY,
                    1,
                    Paint.Style.STROKE
            );

            gridPaint.setPathEffect(
                    new DashPathEffect(
                            new float[]{10, 10},
                            0
                    )
            );

            markerPaint = createPaint(
                    Color.RED,
                    2,
                    Paint.Style.STROKE
            );

            textPaint = createPaint(
                    Color.BLACK,
                    28,
                    Paint.Style.FILL
            );


            signalPath = new Path();
        }


        @Override
        public void run() {

            while (running) {

                Canvas canvas = null;

                try {

                    canvas = holder.lockCanvas();

                    if (canvas != null) {

                        drawFrame(canvas);

                        t += 0.05f;
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

            drawGrid(canvas, width, height);
            drawAxis(canvas, width, height);
            drawSignal(canvas, width, height);
            drawCurrentMarker(canvas, width, height);
            drawXLabels(canvas, width, height);
        }


        private void drawGrid(
                Canvas canvas,
                float width,
                float height) {


            for (float y = Y_MIN; y <= Y_MAX; y += 0.2f) {

                float screenY = mapY(
                        y,
                        height
                );

                canvas.drawLine(
                        0,
                        screenY,
                        width,
                        screenY,
                        gridPaint
                );
            }


            for (int i = 0; i <= X_TICKS; i++) {

                float x = mapX(
                        i,
                        width
                );

                canvas.drawLine(
                        x,
                        0,
                        x,
                        height,
                        gridPaint
                );
            }
        }


        private void drawAxis(
                Canvas canvas,
                float width,
                float height) {


            float zeroY = mapY(
                    0,
                    height
            );

            canvas.drawLine(
                    0,
                    zeroY,
                    width,
                    zeroY,
                    axisPaint
            );

            canvas.drawLine(
                    0,
                    0,
                    0,
                    height,
                    axisPaint
            );
        }


        private void drawSignal(
                Canvas canvas,
                float width,
                float height) {


            signalPath.reset();


            for (int x = 0; x < width; x++) {

                float time =
                        t +
                                X_MIN +
                                x / width * (X_MAX - X_MIN);


                float y =
                        (float) Math.sin(time);


                float screenY =
                        mapY(
                                y,
                                height
                        );


                if (x == 0) {
                    signalPath.moveTo(
                            x,
                            screenY
                    );
                } else {
                    signalPath.lineTo(
                            x,
                            screenY
                    );
                }
            }


            canvas.drawPath(
                    signalPath,
                    signalPaint
            );
        }


        private void drawCurrentMarker(
                Canvas canvas,
                float width,
                float height) {


            float lastTime =
                    t + X_MAX;


            float lastY =
                    (float) Math.sin(lastTime);


            float screenY =
                    mapY(
                            lastY,
                            height
                    );


            canvas.drawLine(
                    width - 1,
                    0,
                    width - 1,
                    height,
                    markerPaint
            );


            canvas.drawCircle(
                    width - 1,
                    screenY,
                    6,
                    markerPaint
            );
        }


        private void drawXLabels(
                Canvas canvas,
                float width,
                float height) {


            for (int i = 0; i <= X_TICKS; i++) {


                float x =
                        mapX(
                                i,
                                width
                        );


                float value =
                        X_MIN +
                                i *
                                        (X_MAX - X_MIN)
                                        /
                                        X_TICKS;


                canvas.drawText(
                        String.format("%.1f", value),
                        x - 15,
                        height - 10,
                        textPaint
                );
            }
        }


        private float mapX(
                int tick,
                float width) {

            return tick *
                    width /
                    X_TICKS;
        }


        private float mapY(
                float value,
                float height) {

            return height -
                    ((value - Y_MIN)
                            /
                            (Y_MAX - Y_MIN))
                            *
                            height;
        }


        private Paint createPaint(
                int color,
                float width,
                Paint.Style style) {


            Paint paint = new Paint();

            paint.setAntiAlias(true);
            paint.setColor(color);
            paint.setStrokeWidth(width);
            paint.setStyle(style);

            return paint;
        }
    }
}