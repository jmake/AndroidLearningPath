package spicy.tech.plotter;

import android.graphics.Color;

public class FunctionView {

    public interface Function {
        float evaluate(float x);
    }

    private final Function function;
    private final float yMin;
    private final float yMax;
    private final int color;

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