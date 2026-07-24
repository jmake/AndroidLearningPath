package spicy.tech.plotter;

import android.graphics.Color;

public class DataBuffer {
    private final String name;
    private final float frequencyHz;
    private final int capacity;
    
    private final float[] times;
    private final float[] values;
    private int head = 0;
    private int size = 0;

    private final float yMin;
    private final float yMax;
    private final int color;

    public DataBuffer(String name, float frequencyHz, int durationSeconds, float yMin, float yMax, int color) {
        this.name = name;
        this.frequencyHz = frequencyHz;
        this.capacity = (int) (frequencyHz * durationSeconds);
        this.times = new float[this.capacity];
        this.values = new float[this.capacity];
        this.yMin = yMin;
        this.yMax = yMax;
        this.color = color;
    }

    public synchronized void addData(float time, float value) {
        times[head] = time;
        values[head] = value;
        head = (head + 1) % capacity;
        if (size < capacity) {
            size++;
        }
    }

    public synchronized float getValueAtTime(float time) {
        if (size == 0) return 0f;

        int currentIndex = head - 1;
        if (currentIndex < 0) currentIndex = capacity - 1;

        float closestTimeDiff = Float.MAX_VALUE;
        float closestValue = 0f;

        // Search recent points for the requested timestamp
        int searchLimit = Math.min(size, 2000); // Max points to search back
        
        for (int i = 0; i < searchLimit; i++) {
            float t = times[currentIndex];
            float diff = Math.abs(t - time);
            
            if (diff < closestTimeDiff) {
                closestTimeDiff = diff;
                closestValue = values[currentIndex];
            } else {
                // If diff starts growing, we've passed the closest point
                if (t < time) {
                    break;
                }
            }
            
            currentIndex--;
            if (currentIndex < 0) currentIndex = capacity - 1;
        }

        // If the requested time is more than 2 seconds away from any known data point, draw 0.
        if (closestTimeDiff > 2.0f) {
             return 0f;
        }

        return closestValue;
    }

    public synchronized float getLatestTime() {
        if (size == 0) return 0f;
        int latestIndex = head - 1;
        if (latestIndex < 0) latestIndex = capacity - 1;
        return times[latestIndex];
    }

    public FunctionView getAsFunctionView() {
        FunctionView fv = new FunctionView(this::getValueAtTime, yMin, yMax, color);
        fv.setTimeProvider(this::getLatestTime);
        return fv;
    }
}
