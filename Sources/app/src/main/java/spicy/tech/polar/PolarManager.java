package spicy.tech.polar;

import android.content.Context;
import android.util.Log;

import com.polar.sdk.api.model.PolarAccelerometerData;
import com.polar.sdk.api.model.PolarHrData;
import com.polar.sdk.api.model.PolarPpiData;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.rx3.RxConvertKt;

public class PolarManager extends PolarManagerBase {

    private Disposable hrDisposable;
    private Disposable ppiDisposable;
    private Disposable accDisposable;

    public final spicy.tech.plotter.DataBuffer accBuffer = new spicy.tech.plotter.DataBuffer("ACC", 200f, 1800, 0f, 3000f, android.graphics.Color.BLUE);
    public final spicy.tech.plotter.DataBuffer hrBuffer = new spicy.tech.plotter.DataBuffer("HR", 1f, 1800, 40f, 200f, android.graphics.Color.RED);
    
    public interface DrawListener {
        void requestDraw();
    }
    private DrawListener drawListener;
    public void setDrawListener(DrawListener listener) { this.drawListener = listener; }

    public PolarManager(Context context) {
        super(context);
    }

    private void LayoutSetTextAcc(String msg) {
        if (textViewAcc != null) {
            textViewAcc.post(() -> textViewAcc.setText("ACC: " + msg));
        }
    }

    private void LayoutSetTextHr(String msg) {
        if (textViewHr != null) {
            textViewHr.post(() -> textViewHr.setText("HR: " + msg));
        }
    }

    @Override
    public void cleanup() {
        stopHrStreaming();
        stopPpiStreaming();
        stopAccStreaming();
        super.cleanup();
    }

    @Override
    protected void onDeviceConnectedCustom(String deviceId, String deviceName) {
        startHrStreaming(deviceId);
        startPpiStreaming(deviceId);
        if (listener != null) listener.onDeviceConnected(deviceName);
    }

    @Override
    protected void onStreamingReady(String deviceId, String deviceName) {
        startAccStreaming(deviceId, deviceName);
    }

    @Override
    protected void onDeviceDisconnectedCustom() {
        firstAccTimestampNs = null;
        phoneTimeAtFirstAccMs = null;
        
        stopHrStreaming();
        stopPpiStreaming();
        stopAccStreaming();
        if (listener != null) listener.onDeviceDisconnected();
    }

    private void startHrStreaming(String deviceId) {
        LayoutSetText("[startHrStreaming] ...");

        Observable<PolarHrData> hrObservable =
                RxConvertKt.asObservable(api.startHrStreaming(deviceId), Dispatchers.getIO());

        hrDisposable = hrObservable.subscribe(
                (PolarHrData data) -> {
                    List<PolarHrData.PolarHrSample> samples = data.getSamples();
                    if (samples.isEmpty()) return;

                    for (PolarHrData.PolarHrSample sample : samples) {
                        double[] sampleData = getHeartRateDataSample(sample);
                        hrLogger.writeArray(sampleData);

                        float time = (float) sampleData[0];
                        float hr = (float) sampleData[1];
                        hrBuffer.addData(time, hr);
                        
                        LayoutSetTextHr(java.util.Arrays.toString(sampleData));
                    }
                    hrLogger.flush();
                    accLogger.flush();
                    if (drawListener != null) drawListener.requestDraw();
                },
                throwable -> {
                    if (!throwable.toString().contains("BleDisconnected") && !throwable.toString().contains("CancellationException")) {
                        Log.e(TAG, "[" + TAG + "] HR stream error", throwable);
                    }
                }
        );
    }

    private void stopHrStreaming() {
        if (hrDisposable != null && !hrDisposable.isDisposed()) {
            hrDisposable.dispose();
        }
    }

    private void startPpiStreaming(String deviceId) {
        LayoutSetText("[startPpiStreaming] ...");

        Observable<PolarPpiData> ppiObservable =
                RxConvertKt.asObservable(api.startPpiStreaming(deviceId), Dispatchers.getIO());

        ppiDisposable = ppiObservable.subscribe(
                (PolarPpiData data) -> {
                    if (!data.getSamples().isEmpty()) {
                        Log.d(TAG, "[" + TAG + "] " + data.getSamples());
                    }
                },
                throwable -> System.out.println("PPI stream error: " + throwable.getMessage())
        );
    }

    private void stopPpiStreaming() {
        if (ppiDisposable != null && !ppiDisposable.isDisposed()) {
            ppiDisposable.dispose();
        }
    }

    private void startAccStreaming(String deviceId, String deviceName) {
        if (deviceName != null && deviceName.contains("Sense")) {
            startAccStreamingSense(deviceId);
        } else {
            startAccStreamingH10(deviceId);
        }
    }

    private void startStreamWithSettingsH10(String deviceId, com.polar.sdk.api.model.PolarSensorSetting settings) {
        if (accDisposable != null && !accDisposable.isDisposed()) return;

        if (settings != null && settings.getSettings() != null) {
            Log.d(TAG, "[startStreamWithSettingsH10] Settings: " + settings.getSettings());
        }

        accDisposable = kotlinx.coroutines.rx3.RxConvertKt.asObservable(
                        api.startAccStreaming(deviceId, settings), 
                        kotlinx.coroutines.Dispatchers.getIO()
                ).subscribe(
                        (com.polar.sdk.api.model.PolarAccelerometerData data) -> {
                            List<PolarAccelerometerData.PolarAccelerometerDataSample> samples;
                            samples = data.getSamples();

                            if (!samples.isEmpty()) {
                                double[] lastSampleData = null;
                                for (PolarAccelerometerData.PolarAccelerometerDataSample sample : samples) {
                                    double[] sampleData = getAccelerometerDataSample(sample);
                                    accLogger.writeArray(sampleData);
                                    lastSampleData = sampleData;
                                    
                                    float time = (float) sampleData[0];
                                    float magnitude = (float) Math.sqrt(sampleData[1]*sampleData[1] + sampleData[2]*sampleData[2] + sampleData[3]*sampleData[3]);
                                    accBuffer.addData(time, magnitude);
                                }
                                if (lastSampleData != null) {
                                    LayoutSetTextAcc(java.util.Arrays.toString(lastSampleData));
                                    if (drawListener != null) drawListener.requestDraw();
                                }
                                accLogger.flush();
                            }
                        },
                        throwable -> {
                            if (!throwable.toString().contains("BleDisconnected") && !throwable.toString().contains("CancellationException")) {
                                Log.e(TAG, "[" + TAG + "] ACC H10 stream error", throwable);
                            }
                        }
                );
    }

    private Long firstAccTimestampNs = null;
    private Long phoneTimeAtFirstAccMs = null; 

    @androidx.annotation.NonNull
    private double[] getAccelerometerDataSample(PolarAccelerometerData.PolarAccelerometerDataSample sample) {
        long timeStampNs = sample.getTimeStamp(); 
        
        if (firstAccTimestampNs == null) {
            firstAccTimestampNs = timeStampNs; 
            phoneTimeAtFirstAccMs = System.currentTimeMillis(); 
        }

        double relativeTimeSeconds = (timeStampNs - firstAccTimestampNs) / 1_000_000_000.0;

        return new double[]{ relativeTimeSeconds, sample.getX(), sample.getY(), sample.getZ() };
    }

    @androidx.annotation.NonNull
    private double[] getHeartRateDataSample(PolarHrData.PolarHrSample sample) {
        long currentPhoneTimeMs = System.currentTimeMillis(); 
        long relativeTimeMs = 0;

        if (phoneTimeAtFirstAccMs != null) {
            relativeTimeMs = currentPhoneTimeMs - phoneTimeAtFirstAccMs;
        }

        double relativeTimeSeconds = relativeTimeMs / 1000.0;

        return new double[]{ relativeTimeSeconds, sample.getHr() };
    }

    private void startAccStreamingH10(String deviceId) {
        if (accDisposable != null && !accDisposable.isDisposed()) return;
        LayoutSetText("[startAccStreamingH10] Requesting...");

        try {
            Object res = api.requestStreamSettings(deviceId, com.polar.sdk.api.PolarBleApi.PolarDeviceDataType.ACC, new kotlin.coroutines.Continuation<com.polar.sdk.api.model.PolarSensorSetting>() {
                @androidx.annotation.NonNull
                @Override
                public kotlin.coroutines.CoroutineContext getContext() {
                    return kotlin.coroutines.EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@androidx.annotation.NonNull Object result) {
                    if (result instanceof com.polar.sdk.api.model.PolarSensorSetting) {
                        startStreamWithSettingsH10(deviceId, (com.polar.sdk.api.model.PolarSensorSetting) result);
                    }
                }
            });

            if (res instanceof com.polar.sdk.api.model.PolarSensorSetting) {
                startStreamWithSettingsH10(deviceId, (com.polar.sdk.api.model.PolarSensorSetting) res);
            }
        } catch (Exception e) { Log.e(TAG, "Failed request: " + e.getMessage()); }
    }

    private void startAccStreamingSense(String deviceId) {
        if (accDisposable != null && !accDisposable.isDisposed()) return;
        LayoutSetText("[startAccStreamingSense] ...");

        try {
            java.util.Map<com.polar.sdk.api.model.PolarSensorSetting.SettingType, Integer> settingsMap = new java.util.HashMap<>();
            settingsMap.put(com.polar.sdk.api.model.PolarSensorSetting.SettingType.SAMPLE_RATE, 52);
            settingsMap.put(com.polar.sdk.api.model.PolarSensorSetting.SettingType.RESOLUTION, 16);
            settingsMap.put(com.polar.sdk.api.model.PolarSensorSetting.SettingType.RANGE, 8);
            settingsMap.put(com.polar.sdk.api.model.PolarSensorSetting.SettingType.CHANNELS, 3);

            com.polar.sdk.api.model.PolarSensorSetting manualSettings = 
                    new com.polar.sdk.api.model.PolarSensorSetting(settingsMap);

            accDisposable = kotlinx.coroutines.rx3.RxConvertKt.asObservable(
                            api.startAccStreaming(deviceId, manualSettings), 
                            kotlinx.coroutines.Dispatchers.getIO()
                    ).subscribe(
                            (com.polar.sdk.api.model.PolarAccelerometerData data) -> {
                                List<PolarAccelerometerData.PolarAccelerometerDataSample> samples = data.getSamples();
                                if (!samples.isEmpty()) {
                                    double[] lastSampleData = null;
                                    for (PolarAccelerometerData.PolarAccelerometerDataSample sample : samples) {
                                        double[] sampleData = getAccelerometerDataSample(sample);
                                        accLogger.writeArray(sampleData);
                                        lastSampleData = sampleData;
                                        
                                        float time = (float) sampleData[0];
                                        float magnitude = (float) Math.sqrt(sampleData[1]*sampleData[1] + sampleData[2]*sampleData[2] + sampleData[3]*sampleData[3]);
                                        accBuffer.addData(time, magnitude);
                                    }
                                    if (lastSampleData != null) {
                                        LayoutSetTextAcc(java.util.Arrays.toString(lastSampleData));
                                        if (drawListener != null) drawListener.requestDraw();
                                    }
                                    accLogger.flush();
                                }
                            },
                            throwable -> {
                                if (!throwable.toString().contains("BleDisconnected") && !throwable.toString().contains("CancellationException")) {
                                    Log.e(TAG, "[" + TAG + "] ACC Sense stream error", throwable);
                                }
                            }
                    );
        } catch (Exception e) { Log.e(TAG, "Failed Sense: " + e.getMessage()); }
    }

    private void stopAccStreaming() {
        if (accDisposable != null && !accDisposable.isDisposed()) {
            accDisposable.dispose();
        }
    }
}