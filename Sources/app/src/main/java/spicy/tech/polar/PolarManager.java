package spicy.tech.polar;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.androidcommunications.api.ble.model.DisInfo;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHealthThermometerData;
import com.polar.sdk.api.model.PolarHrData;
import com.polar.sdk.api.errors.PolarInvalidArgument;
import com.polar.sdk.api.model.PolarPpiData;
import com.polar.sdk.api.model.PolarAccelerometerData;

import java.util.Arrays;
import java.util.HashSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.rx3.RxConvertKt;

public class PolarManager
{
    private TextView textView = null;
    private static final String TAG = "PolarManager";

    private final PolarBleApi api;

    private ConnectionListener listener;
    private Disposable hrDisposable;
    private Disposable ppiDisposable;
    private Disposable accDisposable;
    private String currentDeviceName = "";
    //private final Context context;

    private void LayoutSetText(String msg)
    {
        String msg2 = "";
        msg2 += "-------------\n";
        msg2 += msg;
        msg2 += "\n-------------";

        Log.d(TAG, "[" + TAG + "] " + msg);

        if (textView == null) return ;

        //textView.setText( msg2 );
        String finalMsg = msg2;
        textView.post(() -> textView.setText(finalMsg));

    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public io.reactivex.rxjava3.core.Observable<PolarDeviceInfo> searchForDevice() {
        return kotlinx.coroutines.rx3.RxConvertKt.asObservable(api.searchForDevice(), kotlinx.coroutines.Dispatchers.getIO());
    }

    public void connect(String deviceId, TextView textView)
    {
        this.textView = textView;

        String msg = "";
        msg += "[connect] deviceId:'" + deviceId + "'" ;
        try
        {
            api.disconnectFromDevice(deviceId);
            api.connectToDevice(deviceId);
            msg += "good!!";
        }
        catch (PolarInvalidArgument e)
        {
            msg += "fail!!";
        }

        LayoutSetText( msg );
    }

    public void disconnect(String deviceId) throws PolarInvalidArgument
    {
        api.disconnectFromDevice(deviceId);
    }

    public void cleanup()
    {
        stopHrStreaming();
        stopPpiStreaming();
        stopAccStreaming();
        api.shutDown();
    }

    public PolarManager(Context context)
    {
        //this.context = context;
        //Toast.makeText(context, "PolarManager", Toast.LENGTH_LONG).show();
        /*
        api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                EnumSet.allOf(PolarBleApi.PolarBleSdkFeature.class)
        );
        */

        api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                new HashSet<>(Arrays.asList(
                        PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
                ))
        );
        api.setApiLogger(this::LayoutSetText) ;

        api.setApiCallback(new PolarBleApiCallback()
        {
            @Override
            public void htsNotificationReceived(@NonNull String s, @NonNull PolarHealthThermometerData polarHealthThermometerData)
            {

            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo deviceInfo)
            {
                LayoutSetText( "[deviceConnecting] " + deviceInfo.getDeviceId() );
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo deviceInfo)
            {
                currentDeviceName = deviceInfo.getName();
                LayoutSetText( "[deviceConnected] " + deviceInfo.getDeviceId() );
                startHrStreaming( deviceInfo.getDeviceId() );
                startPpiStreaming( deviceInfo.getDeviceId() );
                if (listener != null) listener.onDeviceConnected(deviceInfo.getName());
            }

            @Override
            public void bleSdkFeatureReady(@NonNull String identifier, @NonNull com.polar.sdk.api.PolarBleApi.PolarBleSdkFeature feature) {
                if (feature == com.polar.sdk.api.PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING) {
                    LayoutSetText("[FeaturesReady] PMD streaming is ready for " + identifier);
                    startAccStreaming(identifier, currentDeviceName);
                }
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo deviceInfo)
            {
                LayoutSetText( "[deviceDisconnected] " + deviceInfo.getDeviceId() );
                stopHrStreaming();
                stopPpiStreaming();
                stopAccStreaming();
                if (listener != null) listener.onDeviceDisconnected();
            }

            @Override
            public void disInformationReceived(@NonNull String identifier, @NonNull DisInfo disInfo)
            {
                LayoutSetText( "[deviceDisconnected] " + disInfo );
            }
        });

        LayoutSetText( "PolarManager ... " );
    }

    private void startHrStreaming(String deviceId)
    {
        LayoutSetText("[startHrStreaming] ...");

        Observable<PolarHrData> hrObservable =
                RxConvertKt.asObservable(api.startHrStreaming(deviceId), Dispatchers.getIO());

        hrDisposable = hrObservable.subscribe(
                (PolarHrData data) -> {
                    if (!data.getSamples().isEmpty())
                    {
                        Log.d(TAG, "[" + TAG + "] " + data.getSamples() );
                        LayoutSetText( data.getSamples().toString() );

                        int hr = data.getSamples().get(0).getHr();
                        //LayoutSetText( "HR: " + hr );
                    }
                },
                throwable -> System.out.println("HR stream error: " + throwable.getMessage())
        );
    }

    private void stopHrStreaming()
    {
        if (hrDisposable != null && !hrDisposable.isDisposed())
        {
            hrDisposable.dispose();
        }
    }

    private void startPpiStreaming(String deviceId)
    {
        LayoutSetText("[startPpiStreaming] ...");

        Observable<PolarPpiData> ppiObservable =
                RxConvertKt.asObservable(api.startPpiStreaming(deviceId), Dispatchers.getIO());

        ppiDisposable = ppiObservable.subscribe(
                (PolarPpiData data) -> {
                    if (!data.getSamples().isEmpty())
                    {
                        Log.d(TAG, "[" + TAG + "] " + data.getSamples());
                    }
                },
                throwable -> System.out.println("PPI stream error: " + throwable.getMessage())
        );
    }

    private void stopPpiStreaming()
    {
        if (ppiDisposable != null && !ppiDisposable.isDisposed())
        {
            ppiDisposable.dispose();
        }
    }

    private void startAccStreaming(String deviceId, String deviceName)
    {
        if (deviceName != null && deviceName.contains("Sense")) {
            startAccStreamingSense(deviceId);
        } else {
            startAccStreamingH10(deviceId);
        }
    }

    private void startStreamWithSettingsH10(String deviceId, com.polar.sdk.api.model.PolarSensorSetting settings) {
        accDisposable = kotlinx.coroutines.rx3.RxConvertKt.asObservable(
                        api.startAccStreaming(deviceId, settings), 
                        kotlinx.coroutines.Dispatchers.getIO()
                ).subscribe(
                        (com.polar.sdk.api.model.PolarAccelerometerData data) -> {
                            if (!data.getSamples().isEmpty()) Log.d(TAG, "[" + TAG + "] ACC H10: " + data.getSamples());
                        },
                        throwable -> Log.e(TAG, "[" + TAG + "] ACC H10 stream error", throwable)
                );
    }

    private void startAccStreamingH10(String deviceId)
    {
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

            // If it returns synchronously
            if (res instanceof com.polar.sdk.api.model.PolarSensorSetting) {
                startStreamWithSettingsH10(deviceId, (com.polar.sdk.api.model.PolarSensorSetting) res);
            }
        } catch (Exception e) { Log.e(TAG, "Failed request: " + e.getMessage()); }
    }

    private void startAccStreamingSense(String deviceId)
    {
        if (accDisposable != null && !accDisposable.isDisposed()) return;
        LayoutSetText("[startAccStreamingSense] ...");

        try {
            // Sense: RESTORED to the working map!
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
                                if (!data.getSamples().isEmpty()) Log.d(TAG, "[" + TAG + "] ACC Sense: " + data.getSamples());
                            },
                            throwable -> Log.e(TAG, "[" + TAG + "] ACC Sense stream error", throwable)
                    );
        } catch (Exception e) { Log.e(TAG, "Failed Sense: " + e.getMessage()); }
    }

    private void stopAccStreaming()
    {
        if (accDisposable != null && !accDisposable.isDisposed())
        {
            accDisposable.dispose();
        }
    }


} // PolarManager