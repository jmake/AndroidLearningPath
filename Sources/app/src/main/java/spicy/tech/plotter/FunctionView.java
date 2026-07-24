package spicy.tech.plotter;

import android.graphics.Color;

public class FunctionView {

    public interface Function {
        float evaluate(float x);
    }

    public interface TimeProvider {
        float getLatestTime();
    }

    private final Function function;
    private TimeProvider timeProvider = null;
    private final float yMin;
    private final float yMax;
    private final int color;
    private float timeWindow = 10.0f;
    private boolean isContinuous = false;

    public FunctionView(Function function, float yMin, float yMax, int color) {
        this.function = function;
        this.color = color;
        this.yMin = yMin;
        this.yMax = yMax;
    }
/*
    public static FunctionView defaultFunction() {
        return null; //new FunctionView(x -> (float) Math.cos(3*x), -2.5f, 2.5f, Color.BLUE);
    }
*/
    public void setTimeProvider(TimeProvider provider) {
        this.timeProvider = provider;
    }

    public float getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(float timeWindow) {
        this.timeWindow = Math.max(1.0f, Math.min(timeWindow, 720.0f));
    }

    public boolean isContinuous() {
        return isContinuous;
    }

    public void setContinuous(boolean continuous) {
        this.isContinuous = continuous;
    }

    public float getLatestTime() {
        if (timeProvider != null) {
            return timeProvider.getLatestTime();
        }
        return -1.0f; // -1 signals the DrawThread to use artificial time
    }

    public float evaluate(float x) {
        return function.evaluate(x);
    }

    public float getYMin() {
        return yMin;
    }

    public float getYMax() {
        return yMax;
    }

    public int getColor() {
        return color;
    }
}