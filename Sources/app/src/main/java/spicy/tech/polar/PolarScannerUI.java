package spicy.tech.polar;

import android.app.Activity;
import android.graphics.Color;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;

import io.reactivex.rxjava3.disposables.Disposable;

public class PolarScannerUI implements ConnectionListener {

    private final Activity activity;
    private final PolarConnection polarConnection;
    private final Button scanButton;
    private static final String TAG = "PolarScannerUI";

    public PolarScannerUI(Activity activity, PolarConnection polarConnection, Button scanButton) {
        this.activity = activity;
        this.polarConnection = polarConnection;
        this.scanButton = scanButton;

        polarConnection.setConnectionListener(this);
        setButtonState(true, Color.BLUE, "Scan Devices");

        scanButton.setOnClickListener(v -> {
            android.widget.Toast.makeText(activity, "Button Clicked!", android.widget.Toast.LENGTH_SHORT).show();
            
            try {
                setButtonState(false, Color.GRAY, "Scanning...");
                startDeviceScan();
            } catch (Exception e) {
                Log.e(TAG, "Exception during scan start", e);
                android.widget.Toast.makeText(activity, "Error: " + e.getMessage(), android.widget.Toast.LENGTH_LONG).show();
                
                setButtonState(true, Color.BLUE, "Scan Devices");
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
        builder.setTitle("Select Polar Device");

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
            polarConnection.connectToSelectedDevice(selectedId);
        });

        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        Disposable scanDisposable = polarConnection.searchForDevice()
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
    public void onDeviceConnected() {
        setButtonState(true, Color.GREEN, "Scan Again (Connected)");
    }

    @Override
    public void onDeviceDisconnected() {
        setButtonState(true, Color.BLUE, "Scan Devices");
    }
}
