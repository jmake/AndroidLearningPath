package spicy.tech.plotter;
/*
SyntheticECG ecg = new SyntheticECG(70);
float sample = ecg.get(time);

float y = ecg.get((float) elapsedSeconds);
canvas.drawPoint(x, y, paint);
*/
public class SyntheticECG {

    private static class Wave {

        double amplitude;
        double position;
        double width;

        Wave(double amplitude, double position, double width) {
            this.amplitude = amplitude;
            this.position = position;
            this.width = width;
        }
    }


    private final Wave[] waves = {
            new Wave( 0.25, 0.18, 0.025), // P
            new Wave(-0.15, 0.36, 0.010), // Q
            new Wave( 1.00, 0.40, 0.012), // R
            new Wave(-0.25, 0.43, 0.010), // S
            new Wave( 0.35, 0.60, 0.040)  // T
    };


    private final double rrInterval;


    public SyntheticECG(float heartRate) {
        rrInterval = 60.0 / heartRate;
    }


    public float get(float time) {

        double phase = time % rrInterval;

        double value = 0.0;

        for (Wave wave : waves) {

            double x = (phase - wave.position) / wave.width;

            value += wave.amplitude *
                    Math.exp(-0.5 * x * x);
        }

        return (float) value;
    }
}
