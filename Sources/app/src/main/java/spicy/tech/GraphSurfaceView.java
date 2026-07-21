package spicy.tech;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphSurfaceView extends SurfaceView implements SurfaceHolder.Callback
{
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

        private final Paint paint;
        private final Path path;

        DrawThread(SurfaceHolder holder) {
            this.holder = holder;

            paint = new Paint();
            paint.setAntiAlias(true);
            paint.setColor(Color.BLUE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);

            path = new Path();
        }

        @Override
        public void run() {

            while (running) {

                Canvas canvas = null;

                try {
                    canvas = holder.lockCanvas();

                    if (canvas != null) {

                        canvas.drawColor(Color.WHITE);

                        float width = canvas.getWidth();
                        float height = canvas.getHeight();

                        path.reset();

                        for (int x = 0; x < width; x++) {

                            float time = t + x * 0.02f;

                            float y = (float) Math.sin(time);

                            float screenY =
                                    height / 2f -
                                            y * height / 3f;

                            if (x == 0) {
                                path.moveTo(x, screenY);
                            } else {
                                path.lineTo(x, screenY);
                            }
                        }

                        canvas.drawPath(path, paint);

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
    }
}

