package spicy.tech;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    private static final int CREATE_FILE_REQUEST = 100;
    private FileLogger logger;

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

            logger.append("Logger started");
            logger.append("Value: 123");
        }
    }
}