package spicy.tech.ble;

public interface ConnectionListener {
    void onDeviceConnected(String deviceName);
    void onDeviceDisconnected();
}
