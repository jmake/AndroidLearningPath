package spicy.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.polar.sdk.api.errors.PolarInvalidArgument;

public class MainActivity extends Activity
{
    private FileLogger fileLogger = null;
    private PolarConnection polarConnection = null;
    private static final int CREATE_FILE_REQUEST = 100;

    private int row = 0;
    private long time0 = 0;
    private Thread loggingThread = null;
    private volatile boolean logging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main );

        String msg = "New text";
        TextView textView = findViewById( R.id.textView );
        textView.setText( msg );

        polarConnection = new PolarConnection(this);
        polarConnection.onCreate();

        fileLogger = new FileLogger(this);
        startActivityForResult(fileLogger.createFilePickerIntent(), CREATE_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (fileLogger != null)
        {
            if (requestCode == CREATE_FILE_REQUEST && resultCode == RESULT_OK && data != null)
            {
                time0 = System.currentTimeMillis();
                fileLogger.setFileUri(data.getData());
                fileLogger.append("time;value1;value2;value3");
                startLogging();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    )
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (polarConnection != null) polarConnection.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startLogging()
    {
        if (fileLogger == null) return ;

        logging = true;
        loggingThread = new Thread(() ->
        {
            while (logging) {
                long time = System.currentTimeMillis();
                long value1 = time - time0;
                int value2 = row;
                double value3 = Math.sin(2.0 * Math.PI * value1 / 1000.0);

                fileLogger.append(time + ";" + value1 + ";" + value2 + ";" + value3);
                row++;
                /*
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
                */
            }
        });
        loggingThread.start();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        logging = false;
        if (loggingThread != null) loggingThread.interrupt();

        try
        {
            if (polarConnection != null) polarConnection.onDestroy();
        }
        catch (PolarInvalidArgument e)
        {
            throw new RuntimeException(e);
        }

    }
}