package spicy.tech.plotter;

import android.graphics.Color;

public class SimplestView extends FunctionView
{
    public SimplestView()
    {
        super(createFunction(), -2.5f, 2.5f, Color.BLUE);
    }

    private static Function createFunction()
    {
        return x -> (float) Math.cos(3*x);
    }

}