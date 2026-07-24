package spicy.tech.plotter;

import android.graphics.Color;

public class SyntheticECGView extends FunctionView
{
    public SyntheticECGView()
    {
        super(createFunction(), -0.3f, 1.1f, Color.BLUE);
        setTimeWindow(3.0f);
        setContinuous(true);
    }

    private static Function createFunction()
    {
        SyntheticECG ecg = new SyntheticECG(70);
        return ecg::get;
    }

}