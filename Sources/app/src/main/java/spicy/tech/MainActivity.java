package spicy.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final int CREATE_FILE_REQUEST = 100;
    private FileLogger logger;

    private long time0;
    private int row = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        logger = new FileLogger(this);

        startActivityForResult(
                logger.createFilePickerIntent(),
                CREATE_FILE_REQUEST
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CREATE_FILE_REQUEST && resultCode == RESULT_OK) {
            logger.setFileUri(data.getData());

            time0 = System.currentTimeMillis();

            logger.append("time;value1;value2;value3");

            startLogging();
        }
    }

    private void startLogging() {
        new Thread(() -> {
            while (true) {
                long time = System.currentTimeMillis();
                long value1 = time - time0;

                int value2 = row;

                double value3 = Math.sin(2.0 * Math.PI * value1 / 1000.0);

                logger.append(
                        time + ";" +
                                value1 + ";" +
                                value2 + ";" +
                                value3
                );

                row++;

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }).start();
    }
}