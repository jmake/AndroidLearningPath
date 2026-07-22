package spicy.tech.polar;

import android.content.Context;
import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class PolarLogger {
    private static final String TAG = "PolarLogger";
    private BufferedWriter writer;

    public void open(Context context, String filename) {
        try {
            File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            File file = new File(dir, filename + ".csv");
            writer = new BufferedWriter(new FileWriter(file, false));
            Log.d(TAG, "Opened file: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Failed to open file: " + filename, e);
        }
    }

    public void writeLine(String line) {
        if (writer != null) {
            try {
                writer.write(line + "\n");
            } catch (Exception e) { }
        }
    }

    public void flush() {
        if (writer != null) {
            try {
                writer.flush();
            } catch (Exception e) { }
        }
    }

    public void close() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
                writer = null;
            } catch (Exception e) { }
        }
    }
}
