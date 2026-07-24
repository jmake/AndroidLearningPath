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

        // Binary search for the closest time
        int low = 0;
        int high = size - 1;
        float closestTimeDiff = Float.MAX_VALUE;
        float closestValue = 0f;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int actualIndex = (head - size + mid + capacity) % capacity;
            float t = times[actualIndex];
            
            float diff = Math.abs(t - time);
            if (diff < closestTimeDiff) {
                closestTimeDiff = diff;
                closestValue = values[actualIndex];
            }
            
            if (t < time) {
                low = mid + 1;
            } else if (t > time) {
                high = mid - 1;
            } else {
                return closestValue;
            }
        }
        
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

    private FunctionView cachedFunctionView = null;

    public synchronized FunctionView getAsFunctionView() {
        if (cachedFunctionView == null) {
            cachedFunctionView = new FunctionView(this::getValueAtTime, yMin, yMax, color);
            cachedFunctionView.setTimeProvider(this::getLatestTime);
            cachedFunctionView.setTimeWindow(name.equals("HR") ? 60.0f : 5.0f);
        }
        return cachedFunctionView;
    }
}
