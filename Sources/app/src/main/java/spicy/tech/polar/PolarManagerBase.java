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
import com.polar.sdk.api.errors.PolarInvalidArgument;

import java.util.Arrays;
import java.util.HashSet;

public abstract class PolarManagerBase {
    protected static final String TAG = "PolarManager";
    protected final PolarBleApi api;
    protected ConnectionListener listener;
    protected String currentDeviceName = "";
    protected final Context context;
    protected final PolarLogger accLogger = new PolarLogger();
    protected final PolarLogger hrLogger = new PolarLogger();

    protected TextView textView = null;
    protected TextView textViewAcc = null;
    protected TextView textViewHr = null;

    public PolarManagerBase(Context context) {
        this.context = context;

        api = PolarBleApiDefaultImpl.defaultImplementation(
                context,
                new HashSet<>(Arrays.asList(
                        PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                        PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
                ))
        );

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void htsNotificationReceived(@NonNull String s, @NonNull PolarHealthThermometerData data) {}

            @Override
            public void deviceConnecting(@NonNull PolarDeviceInfo deviceInfo) {
                LayoutSetText("[deviceConnecting] " + deviceInfo.getDeviceId());
            }

            @Override
            public void deviceConnected(@NonNull PolarDeviceInfo deviceInfo) {
                currentDeviceName = deviceInfo.getName();
                LayoutSetText("[deviceConnected] " + deviceInfo.getDeviceId());
                
                String safeDeviceName = deviceInfo.getName().replace(" ", "_");
                long currentTimestamp = System.currentTimeMillis();
                String sessionName = safeDeviceName + "_" + currentTimestamp;
                startLogging(sessionName);
                
                java.io.File dir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                if (dir != null) {
                    String basePath = dir.getAbsolutePath() + "/" + sessionName;
                    LayoutSetText("Logging to: " + basePath + "_ACC.txt");
                    LayoutSetText("Logging to: " + basePath + "_HR.txt");
                    Log.d(TAG, "Logging to: " + basePath + "_ACC.txt");
                    Log.d(TAG, "Logging to: " + basePath + "_HR.txt");
                }

                onDeviceConnectedCustom(deviceInfo.getDeviceId(), deviceInfo.getName());
            }

            @Override
            public void bleSdkFeatureReady(@NonNull String identifier, @NonNull PolarBleApi.PolarBleSdkFeature feature) {
                if (feature == PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING) {
                    LayoutSetText("[FeaturesReady] PMD streaming is ready for " + identifier);
                    onStreamingReady(identifier, currentDeviceName);
                }
            }

            @Override
            public void deviceDisconnected(@NonNull PolarDeviceInfo deviceInfo) {
                LayoutSetText("[deviceDisconnected] " + deviceInfo.getDeviceId());
                stopLogging();
                onDeviceDisconnectedCustom();
            }

            @Override
            public void disInformationReceived(@NonNull String identifier, @NonNull DisInfo disInfo) {
                LayoutSetText("[disInformationReceived] " + disInfo);
            }
        });

        LayoutSetText("PolarManager ... ");
    }

    protected abstract void onDeviceConnectedCustom(String deviceId, String deviceName);
    protected abstract void onStreamingReady(String deviceId, String deviceName);
    protected abstract void onDeviceDisconnectedCustom();

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
    }

    public io.reactivex.rxjava3.core.Observable<PolarDeviceInfo> searchForDevice() {
        return kotlinx.coroutines.rx3.RxConvertKt.asObservable(api.searchForDevice(), kotlinx.coroutines.Dispatchers.getIO());
    }

    public void connect(String deviceId, TextView textView, TextView textViewAcc, TextView textViewHr) {
        this.textView = textView;
        this.textViewAcc = textViewAcc;
        this.textViewHr = textViewHr;

        String msg = "[connect] deviceId:'" + deviceId + "'";
        try {
            api.disconnectFromDevice(deviceId);
            api.connectToDevice(deviceId);
            msg += "good!!";
        } catch (PolarInvalidArgument e) {
            msg += "fail!!";
        }
        LayoutSetText(msg);
    }

    public void disconnect(String deviceId) throws PolarInvalidArgument {
        api.disconnectFromDevice(deviceId);
    }

    public void startLogging(String baseFilename) {
        accLogger.open(context, baseFilename + "_ACC");
        hrLogger.open(context, baseFilename + "_HR");
    }

    public void stopLogging() {
        accLogger.close();
        hrLogger.close();
    }

    public void cleanup() {
        stopLogging();
        api.shutDown();
    }

    protected void LayoutSetText(String msg) {
        String msg2 = "-------------\n" + msg + "\n-------------";
        Log.d(TAG, "[" + TAG + "] " + msg);
        if (textView == null) return;
        textView.post(() -> textView.setText(msg2));
    }
}
