package spicy.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.polar.sdk.api.errors.PolarInvalidArgument;

public class MainActivity extends Activity
{
    private static final String TAG = "MainActivity";
    private static final int CREATE_FILE_REQUEST = 100;

    private TextView textViewBody = null;
    private TextView textViewFooter = null;

    private FileLogger fileLogger = null;
    private PolarConnection polarConnection = null;

    private int row = 0;
    private long time0 = 0;
    private Thread loggingThread = null;
    private volatile boolean logging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LayoutOnCreate();
        LayoutSetText("onCreate");

        //LoggingThreadOnCreate();
        PolarConnectionOnCreate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST && resultCode == RESULT_OK && data != null)
        {
            LoggingThreadOnActivityResult( data );
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

        PolarConnectionOnRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        LoggingThreadOnDestroy();
        PolarConnectionOnDestroy();
    }

    // Layout
    private void LayoutOnCreate()
    {
        setContentView( R.layout.activity_main );

        textViewBody = findViewById( R.id.body_id );
        //String msg = "Ready ...";
        //textViewBody.setText(msg);

        textViewFooter = findViewById( R.id.footer_id );
    }

    private void LayoutSetText(String msg)
    {
        Log.d(TAG, "[" + TAG + "] " + msg);

        if (textViewFooter == null) return ;
        textViewFooter.setText( msg );
    }

    // PolarConnection
    private void PolarConnectionOnCreate()
    {
        polarConnection = new PolarConnection(this);
        polarConnection.onCreate(textViewBody);
    }

    private void PolarConnectionOnRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    )
    {
        if (polarConnection == null) return ;

        polarConnection.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void PolarConnectionOnDestroy()
    {
        if (polarConnection == null) return ;

        try
        {
            polarConnection.onDestroy();
        }
        catch (PolarInvalidArgument e)
        {
            throw new RuntimeException(e);
        }
    }

    // LoggingThread
    private void LoggingThreadOnCreate()
    {
        fileLogger = new FileLogger(this);
        startActivityForResult(fileLogger.createFilePickerIntent(), CREATE_FILE_REQUEST);
    }

    private void LoggingThreadOnActivityResult(Intent data)
    {
        if (fileLogger == null) return ;

        time0 = System.currentTimeMillis();
        fileLogger.setFileUri( data.getData() );
        fileLogger.append("time;value1;value2;value3");
        LoggingThreadStart();
    }

    private void LoggingThreadStart()
    {
        if (fileLogger == null) return ;

        logging = true;
        loggingThread = new Thread(() ->
        {
            String msg = "";
            while (logging)
            {
                long time = System.currentTimeMillis();
                long value1 = time - time0;
                int value2 = row;
                double value3 = Math.sin(2.0 * Math.PI * value1 / 1000.0);

                msg = time + ";" + value1 + ";" + value2 + ";" + value3;
                //textView.setText( msg );
                fileLogger.append( msg );
                row++;

                try { Thread.sleep(10); }
                catch (InterruptedException e) {return;}
            }
        });
        loggingThread.start();
    }

    protected void LoggingThreadOnDestroy()
    {
        if (loggingThread == null) return ;

        logging = false;
        loggingThread.interrupt();
    }

} // MainActivity