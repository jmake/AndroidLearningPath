package spicy.tech.ble;

public class DeviceInfo {
    private final String id;
    private final String name;

    public DeviceInfo(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getDeviceId() { return id; }
    public String getName() { return name; }
}
