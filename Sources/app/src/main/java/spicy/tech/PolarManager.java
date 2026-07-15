package spicy.tech;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
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

import java.util.Arrays;
import java.util.EnumSet;
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
    private Disposable hrDisposable;
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
                LayoutSetText( "[deviceConnected] " + deviceInfo.getDeviceId() );
                //Toast.makeText(context, "Connected", Toast.LENGTH_SHORT).show();
                startHrStreaming( deviceInfo.getDeviceId() );
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo deviceInfo)
            {
                LayoutSetText( "[deviceDisconnected] " + deviceInfo.getDeviceId() );
                stopHrStreaming();
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
        //Log.d(TAG,"[startHrStreaming] ...");
        LayoutSetText("[startHrStreaming] ...");

        Observable<PolarHrData> hrObservable =
                RxConvertKt.asObservable(api.startHrStreaming(deviceId), Dispatchers.getIO());

        hrDisposable = hrObservable.subscribe(
                (PolarHrData data) -> {
                    if (!data.getSamples().isEmpty())
                    {
                        int hr = data.getSamples().get(0).getHr();
                        LayoutSetText( "HR: " + hr );
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

    public void connect(String deviceId, TextView textView)
    {
        this.textView = textView;

        String msg = "";
        msg += "[connect] deviceId:'" + deviceId + "'" ;
        try
        {
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
        api.shutDown();
    }


} // PolarManager