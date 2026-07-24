package spicy.tech.plotter;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread thread;
    private ScaleGestureDetector scaleDetector;

    //private FunctionView functionView = new SimplestView();
    private FunctionView functionView = new SyntheticECGView();

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

    public void setFunctionView(FunctionView functionView) {
        this.functionView = functionView;
        if (thread != null) {
            thread.setFunctionView(functionView);
            thread.requestDraw();
        }
    }

    private void init() {
        getHolder().addCallback(this);
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                if (thread != null) {
                    float factor = detector.getScaleFactor();
                    thread.updateTimeWindow(thread.getTimeWindow() / factor);
                    thread.requestDraw();
                }
                return true;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        return true;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        thread = new DrawThread(holder, functionView);
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

    public void resetGraph() {
        if (thread != null) {
            thread.resetTime();
            thread.requestDraw();
        }
    }
    
    public void requestDraw() {
        if (thread != null) {
            thread.requestDraw();
        }
    }
}