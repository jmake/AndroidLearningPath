package spicy.tech;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.polar.sdk.api.PolarBleApi;
import com.polar.sdk.api.PolarBleApiCallback;
import com.polar.sdk.api.PolarBleApiDefaultImpl;
import com.polar.androidcommunications.api.ble.model.DisInfo;
import com.polar.sdk.api.model.PolarDeviceInfo;
import com.polar.sdk.api.model.PolarHealthThermometerData;
import com.polar.sdk.api.model.PolarHrData;
import com.polar.sdk.api.errors.PolarInvalidArgument;

import java.util.EnumSet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.rx3.RxConvertKt;

public class PolarManager {

    private final PolarBleApi api;
    private Disposable hrDisposable;

    public PolarManager(Context context) {
        api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                EnumSet.allOf(PolarBleApi.PolarBleSdkFeature.class)
        );

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void htsNotificationReceived(@NonNull String s, @NonNull PolarHealthThermometerData polarHealthThermometerData) {

            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo deviceInfo) {
                System.out.println("Connecting: " + deviceInfo.getDeviceId());
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo deviceInfo) {
                System.out.println("Connected: " + deviceInfo.getDeviceId());
                startHrStreaming(deviceInfo.getDeviceId());
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo deviceInfo) {
                System.out.println("Disconnected");
                stopHrStreaming();
            }

            @Override
            public void disInformationReceived(@NonNull String identifier, @NonNull DisInfo disInfo) {
                System.out.println("Device info received: " + disInfo);
            }
        });
    }

    private void startHrStreaming(String deviceId) {
        Observable<PolarHrData> hrObservable =
                RxConvertKt.asObservable(api.startHrStreaming(deviceId), Dispatchers.getIO());

        hrDisposable = hrObservable.subscribe(
                (PolarHrData data) -> {
                    if (!data.getSamples().isEmpty()) {
                        int hr = data.getSamples().get(0).getHr();
                        System.out.println("HR: " + hr);
                    }
                },
                throwable -> System.out.println("HR stream error: " + throwable.getMessage())
        );
    }

    private void stopHrStreaming() {
        if (hrDisposable != null && !hrDisposable.isDisposed()) {
            hrDisposable.dispose();
        }
    }

    public void connect(String deviceId) {
        try {
            api.connectToDevice(deviceId);
        } catch (PolarInvalidArgument e) {
            Log.e("PolarManager", "Invalid device ID: " + deviceId, e);
        }
    }

    public void disconnect(String deviceId) throws PolarInvalidArgument {
        api.disconnectFromDevice(deviceId);
    }

    public void cleanup() {
        stopHrStreaming();
        api.shutDown();
    }
}