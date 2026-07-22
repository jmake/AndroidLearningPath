package spicy.tech.polar;

public interface ConnectionListener {
    void onDeviceConnected(String deviceName);
    void onDeviceDisconnected();
}
