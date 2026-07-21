package spicy.tech.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FileLogger
{
    private static final String TAG = "FileLogger";

    private Uri fileUri;
    private final Context context;

    public FileLogger(Context context) {
        this.context = context.getApplicationContext();
    }

    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "values.txt");
        return intent;
    }

    public void setFileUri(Uri uri) {
        fileUri = uri;
    }

    public boolean hasFile() {
        return fileUri != null;
    }

    public void append(String text) {
        Log.d(TAG, text);

        if (fileUri == null) {
            return;
        }

        try (OutputStream outputStream = context.getContentResolver()
                .openOutputStream(fileUri, "wa")) {

            if (outputStream != null) {
                outputStream.write((text + "\n").getBytes(StandardCharsets.UTF_8));
            }

        } catch (IOException e) {
            Log.e(TAG, "Write error", e);
        }
    }

    public void clearFile() {
        fileUri = null;
    }
}