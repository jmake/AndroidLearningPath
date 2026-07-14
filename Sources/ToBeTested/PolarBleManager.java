package spicy.tech;

import android.content.Context;
import android.util.Log;
import com.polar.sdk.api.PolarSDKManager;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarExerciseData;
import com.polar.sdk.api.model.PolarHrData;
import com.polar.sdk.api.model.PolarSensorSetting;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class PolarBleManager {
    private static final String TAG = "PolarBleManager";
    private PolarSDKManager polarSDKManager;
    private String selectedDeviceId;
    private List<PolarManagerListener> listeners = new ArrayList<>();
    private Disposable hrDisposable;
    private Disposable discoveryDisposable;
    private Disposable connectionDisposable;

    public interface PolarManagerListener {
        void onDeviceFound(PolarDeviceInfo device);
        void onDeviceConnected(String deviceId);
        void onDeviceDisconnected(String deviceId);
        void onHeartRateUpdate(int heartRate, long timestamp);
        void onError(String error);
    }

    public PolarBleManager(Context context) {
        this.polarSDKManager = PolarSDKManager.defaultImplementation(context);
    }

    public void addListener(PolarManagerListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PolarManagerListener listener) {
        listeners.remove(listener);
    }

    public void startDeviceDiscovery() {
        discoveryDisposable = polarSDKManager.searchForDevice()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        device -> {
                            Log.d(TAG, "Device found: " + device.getDeviceId());
                            notifyDeviceFound(device);
                        },
                        throwable -> {
                            Log.e(TAG, "Discovery error", throwable);
                            notifyError("Discovery failed: " + throwable.getMessage());
                        }
                );
    }

    public void stopDeviceDiscovery() {
        if (discoveryDisposable != null && !discoveryDisposable.isDisposed()) {
            discoveryDisposable.dispose();
        }
    }

    public void connectToDevice(String deviceId) {
        this.selectedDeviceId = deviceId;
        connectionDisposable = polarSDKManager.connectToDevice(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "Connected to device: " + deviceId);
                            notifyDeviceConnected(deviceId);
                            startHeartRateMonitoring(deviceId);
                        },
                        throwable -> {
                            Log.e(TAG, "Connection error", throwable);
                            notifyError("Connection failed: " + throwable.getMessage());
                        }
                );
    }

    public void disconnectDevice(String deviceId) {
        connectionDisposable = polarSDKManager.disconnectFromDevice(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> {
                            Log.d(TAG, "Disconnected from device: " + deviceId);
                            stopHeartRateMonitoring();
                            notifyDeviceDisconnected(deviceId);
                        },
                        throwable -> {
                            Log.e(TAG, "Disconnection error", throwable);
                            notifyError("Disconnection failed: " + throwable.getMessage());
                        }
                );
    }

    public void startHeartRateMonitoring(String deviceId) {
        hrDisposable = polarSDKManager.startHeartRateStreaming(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        hrData -> {
                            Log.d(TAG, "HR: " + hrData.getHeartRate() + " bpm");
                            notifyHeartRateUpdate(hrData.getHeartRate(), System.currentTimeMillis());
                        },
                        throwable -> {
                            Log.e(TAG, "HR monitoring error", throwable);
                            notifyError("HR monitoring failed: " + throwable.getMessage());
                        }
                );
    }

    public void stopHeartRateMonitoring() {
        if (hrDisposable != null && !hrDisposable.isDisposed()) {
            hrDisposable.dispose();
        }
    }

    public void requestSensorCapabilities(String deviceId) {
        polarSDKManager.getAvailableSensorSettings(deviceId, null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        settings -> Log.d(TAG, "Sensor settings: " + settings),
                        throwable -> {
                            Log.e(TAG, "Capability error", throwable);
                            notifyError("Sensor capability error: " + throwable.getMessage());
                        }
                );
    }

    public void startExerciseRecording(String deviceId) {
        polarSDKManager.startRecordingExercise(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        () -> Log.d(TAG, "Exercise recording started"),
                        throwable -> {
                            Log.e(TAG, "Exercise start error", throwable);
                            notifyError("Exercise start failed: " + throwable.getMessage());
                        }
                );
    }

    public void stopExerciseRecording(String deviceId) {
        polarSDKManager.stopRecordingExercise(deviceId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        exerciseId -> Log.d(TAG, "Exercise recorded: " + exerciseId),
                        throwable -> {
                            Log.e(TAG, "Exercise stop error", throwable);
                            notifyError("Exercise stop failed: " + throwable.getMessage());
                        }
                );
    }

    public void cleanup() {
        stopHeartRateMonitoring();
        stopDeviceDiscovery();
        if (selectedDeviceId != null) {
            disconnectDevice(selectedDeviceId);
        }
    }

    private void notifyDeviceFound(PolarDeviceInfo device) {
        for (PolarManagerListener listener : listeners) {
            listener.onDeviceFound(device);
        }
    }

    private void notifyDeviceConnected(String deviceId) {
        for (PolarManagerListener listener : listeners) {
            listener.onDeviceConnected(deviceId);
        }
    }

    private void notifyDeviceDisconnected(String deviceId) {
        for (PolarManagerListener listener : listeners) {
            listener.onDeviceDisconnected(deviceId);
        }
    }

    private void notifyHeartRateUpdate(int heartRate, long timestamp) {
        for (PolarManagerListener listener : listeners) {
            listener.onHeartRateUpdate(heartRate, timestamp);
        }
    }

    private void notifyError(String error) {
        for (PolarManagerListener listener : listeners) {
            listener.onError(error);
        }
    }
}