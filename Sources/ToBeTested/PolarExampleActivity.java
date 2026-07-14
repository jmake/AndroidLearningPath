package spicy.tech;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.polar.sdk.api.model.PolarDeviceInfo;

public class PolarExampleActivity extends AppCompatActivity implements PolarBleManager.PolarManagerListener {
    private PolarBleManager polarBleManager;
    private TextView statusText;
    private TextView hrText;
    private Button btnDiscover;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnStartExercise;
    private Button btnStopExercise;
    private String connectedDeviceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polar_example);

        initializeViews();
        initializePolarManager();
        setupButtonListeners();
    }

    private void initializeViews() {
        statusText = findViewById(R.id.status_text);
        hrText = findViewById(R.id.hr_text);
        btnDiscover = findViewById(R.id.btn_discover);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnStartExercise = findViewById(R.id.btn_start_exercise);
        btnStopExercise = findViewById(R.id.btn_stop_exercise);
    }

    private void initializePolarManager() {
        polarBleManager = new PolarBleManager(this);
        polarBleManager.addListener(this);
    }

    private void setupButtonListeners() {
        btnDiscover.setOnClickListener(v -> {
            statusText.setText("Discovering devices...");
            polarBleManager.startDeviceDiscovery();
        });

        btnConnect.setOnClickListener(v -> {
            if (connectedDeviceId != null) {
                polarBleManager.connectToDevice(connectedDeviceId);
            } else {
                Toast.makeText(this, "Select a device first", Toast.LENGTH_SHORT).show();
            }
        });

        btnDisconnect.setOnClickListener(v -> {
            if (connectedDeviceId != null) {
                polarBleManager.disconnectDevice(connectedDeviceId);
            }
        });

        btnStartExercise.setOnClickListener(v -> {
            if (connectedDeviceId != null) {
                polarBleManager.startExerciseRecording(connectedDeviceId);
            }
        });

        btnStopExercise.setOnClickListener(v -> {
            if (connectedDeviceId != null) {
                polarBleManager.stopExerciseRecording(connectedDeviceId);
            }
        });
    }

    @Override
    public void onDeviceFound(PolarDeviceInfo device) {
        statusText.setText("Found: " + device.getDeviceName() + " (" + device.getDeviceId() + ")");
        connectedDeviceId = device.getDeviceId();
    }

    @Override
    public void onDeviceConnected(String deviceId) {
        statusText.setText("Connected to: " + deviceId);
        btnConnect.setEnabled(false);
        btnDisconnect.setEnabled(true);
        btnStartExercise.setEnabled(true);
        polarBleManager.requestSensorCapabilities(deviceId);
    }

    @Override
    public void onDeviceDisconnected(String deviceId) {
        statusText.setText("Disconnected from: " + deviceId);
        hrText.setText("HR: -- bpm");
        btnConnect.setEnabled(true);
        btnDisconnect.setEnabled(false);
        btnStartExercise.setEnabled(false);
        btnStopExercise.setEnabled(false);
    }

    @Override
    public void onHeartRateUpdate(int heartRate, long timestamp) {
        hrText.setText("HR: " + heartRate + " bpm");
    }

    @Override
    public void onError(String error) {
        statusText.setText("Error: " + error);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        polarBleManager.cleanup();
    }
}