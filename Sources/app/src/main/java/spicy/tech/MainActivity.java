package spicy.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.polar.sdk.api.errors.PolarInvalidArgument;

public class MainActivity extends Activity
{
    private FileLogger logger;
    private PolarConnection polarConnection;
    private static final int CREATE_FILE_REQUEST = 100;

    private int row = 0;
    private long time0;
    private Thread loggingThread;
    private volatile boolean logging = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logger = new FileLogger(this);
        polarConnection = new PolarConnection(this);
        polarConnection.onCreate();

        startActivityForResult(
                logger.createFilePickerIntent(),
                CREATE_FILE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            logger.setFileUri(data.getData());
            time0 = System.currentTimeMillis();
            logger.append("time;value1;value2;value3");
            startLogging();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        polarConnection.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void startLogging()
    {
        logging = true;
        loggingThread = new Thread(() -> {
            while (logging) {
                long time = System.currentTimeMillis();
                long value1 = time - time0;
                int value2 = row;
                double value3 = Math.sin(2.0 * Math.PI * value1 / 1000.0);

                logger.append(time + ";" + value1 + ";" + value2 + ";" + value3);
                row++;

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        loggingThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logging = false;
        if (loggingThread != null) {
            loggingThread.interrupt();
        }
        try {
            polarConnection.onDestroy();
        } catch (PolarInvalidArgument e) {
            throw new RuntimeException(e);
        }
    }
}