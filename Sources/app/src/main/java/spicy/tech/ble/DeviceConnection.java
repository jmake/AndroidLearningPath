package spicy.tech.ble;

import io.reactivex.rxjava3.core.Observable;

public interface DeviceConnection {
    void setConnectionListener(ConnectionListener listener);
    Observable<DeviceInfo> searchForDevice();
    void connectToSelectedDevice(String selectedDeviceId);
    void disconnect();
}
