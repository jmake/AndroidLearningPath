package spicy.tech.ble;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;

import io.reactivex.rxjava3.disposables.Disposable;

public class DeviceScannerUI implements ConnectionListener {

    private final Activity activity;
    private final DeviceConnection deviceConnection;
    private final Button scanButton;
    private static final String TAG = "DeviceScannerUI";
    private boolean isConnected = false;
    private String connectedDeviceName = "";

    public DeviceScannerUI(Activity activity, DeviceConnection deviceConnection, Button scanButton) {
        this.activity = activity;
        this.deviceConnection = deviceConnection;
        this.scanButton = scanButton;

        deviceConnection.setConnectionListener(this);
        setButtonState(true, Color.BLUE, "Scan Devices");

        scanButton.setOnClickListener(v -> {
            try {
                if (isConnected) {
                    setButtonState(false, Color.GRAY, "Disconnecting...");
                    deviceConnection.disconnect();
                } else {
                    setButtonState(false, Color.GRAY, "Scanning...");
                    startDeviceScan();
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during scan/disconnect", e);
                android.widget.Toast.makeText(activity, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                setButtonState(true, isConnected ? Color.RED : Color.BLUE, isConnected ? "Disconnect (" + connectedDeviceName + ")" : "Scan Devices");
            }
        });
    }

    private void setButtonState(boolean enabled, int color, String text) {
        activity.runOnUiThread(() -> {
            scanButton.setEnabled(enabled);
            scanButton.setBackgroundColor(color);
            scanButton.setText(text);
        });
    }

    private final java.util.Map<String, String> deviceMap = new java.util.HashMap<>();

    private void startDeviceScan() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(activity);
        builder.setTitle("Select Device");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity, android.R.layout.simple_list_item_1) {
            @Override
            public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getView(position, convertView, parent);
                if (position % 2 == 1) {
                    view.setBackgroundColor(Color.LTGRAY);
                } else {
                    view.setBackgroundColor(Color.WHITE);
                }
                return view;
            }
        };
        builder.setAdapter(adapter, (dialog, which) -> {
            String displayString = adapter.getItem(which);
            String selectedId = deviceMap.get(displayString);
            deviceConnection.connectToSelectedDevice(selectedId);
        });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        Disposable scanDisposable = deviceConnection.searchForDevice()
                .subscribe(
                        deviceInfo -> activity.runOnUiThread(() -> {
                            String id = deviceInfo.getDeviceId();
                            String name = deviceInfo.getName();
                            String display = name + " - " + id;

                            if (adapter.getPosition(display) < 0) {
                                deviceMap.put(display, id);
                                adapter.add(display);
                            }
                        }),
                        error -> Log.e(TAG, "Scan error", error)
                );

        dialog.setOnDismissListener(d -> {
            scanDisposable.dispose();
            setButtonState(true, Color.BLUE, "Scan Devices");
        });
    }

    @Override
    public void onDeviceConnected(String deviceName) {
        isConnected = true;
        connectedDeviceName = deviceName;
        setButtonState(true, Color.RED, "Disconnect (" + deviceName + ")");
    }

    @Override
    public void onDeviceDisconnected() {
        isConnected = false;
        setButtonState(true, Color.BLUE, "Scan Devices");
    }
}
