package spicy.tech;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

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

public class PolarManager
{
    private static final String TAG = "PolarManager";

    private final PolarBleApi api;
    private Disposable hrDisposable;
    //private final Context context;

    public PolarManager(Context context)
    {
        //this.context = context;
        //Toast.makeText(context, "PolarManager", Toast.LENGTH_LONG).show();

        api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                EnumSet.allOf(PolarBleApi.PolarBleSdkFeature.class)
        );

        //api.setApiLogger { str: String -> Log.d("SDK", str) }

        api.setApiCallback(new PolarBleApiCallback()
        {

            @Override
            public void htsNotificationReceived(@NonNull String s, @NonNull PolarHealthThermometerData polarHealthThermometerData) {

            }

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo deviceInfo)
            {
                Log.d(TAG, "[deviceConnecting] " + deviceInfo.getDeviceId());
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo deviceInfo)
            {
                Log.d(TAG, "[deviceConnected] " + deviceInfo.getDeviceId());
                Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                startHrStreaming( deviceInfo.getDeviceId() );
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo deviceInfo) {
                System.out.println("Disconnected");
                stopHrStreaming();
            }

            @Override
            public void disInformationReceived(@NonNull String identifier, @NonNull DisInfo disInfo)
            {
                System.out.println("Device info received: " + disInfo);
            }
        });

        Log.d(TAG,"[PolarManager] ...");
        Toast.makeText(context, "PolarManager...", Toast.LENGTH_LONG).show();
    }

    private void startHrStreaming(String deviceId)
    {
        Log.d(TAG,"[startHrStreaming] ...");

        Observable<PolarHrData> hrObservable =
                RxConvertKt.asObservable(api.startHrStreaming(deviceId), Dispatchers.getIO());

        hrDisposable = hrObservable.subscribe(
                (PolarHrData data) -> {
                    if (!data.getSamples().isEmpty()) {
                        int hr = data.getSamples().get(0).getHr();
                        System.out.println("HR: " + hr);
                        Log.d(TAG, "HR: " + hr);
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
            Log.e(TAG, "Invalid device ID: " + deviceId, e);
        }
    }

    public void disconnect(String deviceId) throws PolarInvalidArgument {
        api.disconnectFromDevice(deviceId);
    }

    public void cleanup() {
        stopHrStreaming();
        api.shutDown();
    }


} // PolarManager