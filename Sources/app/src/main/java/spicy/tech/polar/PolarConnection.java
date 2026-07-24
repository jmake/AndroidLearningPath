package spicy.tech.polar;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import spicy.tech.ble.DeviceConnection;
import spicy.tech.ble.ConnectionListener;
import spicy.tech.ble.DeviceInfo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.polar.sdk.api.errors.PolarInvalidArgument;

public class PolarConnection implements DeviceConnection
{
    private static final String TAG = "PolarConnection";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView textView = null;
    private TextView textViewAcc = null;
    private TextView textViewHr = null;
    private String deviceId;

    private PolarManager polarManager;
    private final Context context;
    private ConnectionListener listener;

    private void LayoutSetText(String msg)
    {
        Log.d(TAG, "[" + TAG + "] " + msg);

        if (textView == null) return ;
        textView.setText( msg );
    }

    public PolarConnection(Context context)
    {
        this.context = context;
    }

    public void setConnectionListener(ConnectionListener listener) {
        this.listener = listener;
        if (polarManager != null) {
            polarManager.setConnectionListener(listener);
        }
    }

    public PolarManager getPolarManager() {
        return polarManager;
    }

    public void onCreate(TextView textView, TextView textViewAcc, TextView textViewHr)
    {
        this.textView = textView ;
        this.textViewAcc = textViewAcc;
        this.textViewHr = textViewHr;

        boolean requiredPermissions = hasRequiredPermissions();
        LayoutSetText("requiredPermissions: '" + requiredPermissions + "' ");

        if (requiredPermissions) { initPolarManager(); }
        else { requestRequiredPermissions(); }
    }

    private boolean hasRequiredPermissions()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            return ContextCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this.context, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return ContextCompat.checkSelfPermission(this.context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestRequiredPermissions()
    {
        if (!(this.context instanceof Activity)) {
            Log.e(TAG, "Context is not an Activity; cannot request permissions");
            return;
        }

        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            };
        }
        else
        {
            permissions = new String[]{ Manifest.permission.ACCESS_FINE_LOCATION };
        }
        ActivityCompat.requestPermissions((Activity) this.context, permissions, PERMISSION_REQUEST_CODE);
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        if (requestCode == PERMISSION_REQUEST_CODE)
        {
            if (hasRequiredPermissions())
            {
                initPolarManager();
            }
            else
            {
                Log.e(TAG, "Required Bluetooth/location permissions were denied");
                Toast.makeText(this.context, "Bluetooth permissions are required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initPolarManager()
    {
        if (polarManager == null) {
            polarManager = new PolarManager(this.context);
            if (listener != null) {
                polarManager.setConnectionListener(listener);
            }
        }
    }

    @Override
    public io.reactivex.rxjava3.core.Observable<DeviceInfo> searchForDevice() {
        initPolarManager();
        return polarManager.searchForDevice()
            .map(device -> new DeviceInfo(device.getDeviceId(), device.getName()));
    }

    public void connectToSelectedDevice(String selectedDeviceId) {
        this.deviceId = selectedDeviceId;
        initPolarManager();
        LayoutSetText("connectToSelectedDevice :'" + deviceId + "' ");
        polarManager.connect(deviceId, textView, textViewAcc, textViewHr);
    }

    public void disconnect() {
        if (polarManager != null && deviceId != null) {
            try {
                polarManager.disconnect(deviceId);
            } catch (PolarInvalidArgument e) {
                Log.e(TAG, "Disconnect error", e);
            }
        }
    }

    public void onDestroy() throws PolarInvalidArgument
    {
        if (polarManager == null) return ;

        if (deviceId != null) {
            polarManager.disconnect(deviceId);
        }
        polarManager.cleanup();
        LayoutSetText("onDestroy :'" + deviceId + "' "); 
    }
}