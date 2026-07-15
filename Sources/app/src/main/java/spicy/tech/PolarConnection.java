package spicy.tech;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.polar.sdk.api.errors.PolarInvalidArgument;

public class PolarConnection
{
    private static final String TAG = "PolarConnection";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private TextView textView = null;

    /*
    python -m polar_python scan --json
    {
        "name": "Polar Sense 065AFD32",
        "address": "24:AC:AC:06:5A:FD"
    }
    */
    private static final String DEVICE_ID = "065AFD32";

    private PolarManager polarManager;
    private final Context context;

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

    protected void onCreate(TextView textView)
    {
        this.textView = textView ;
        //textView = textView.findViewById(R.id.textView);

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
        polarManager = new PolarManager(this.context);
        polarManager.connect(DEVICE_ID);
        LayoutSetText("initPolarManager :'" + DEVICE_ID + "' ");
    }

    protected void onDestroy() throws PolarInvalidArgument
    {
        if (polarManager == null) return ;

        polarManager.disconnect(DEVICE_ID);
        polarManager.cleanup();
        //Log.d(TAG,"[onDestroy] DEVICE_ID:" + DEVICE_ID);
        LayoutSetText("onDestroy :'" + DEVICE_ID + "' "); 
    }
}