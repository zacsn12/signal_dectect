package org.zacsn.signal_dectect.domain.model;

/**
 * Represents a device discovered on the local area network.
 */
public class LanDevice {
    private final String ipAddress;
    private final String macAddress;
    private final String hostname;
    private final String manufacturer;
    private final boolean isCamera;

    public LanDevice(String ipAddress, String macAddress, String hostname,
                    String manufacturer, boolean isCamera) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.hostname = hostname;
        this.manufacturer = manufacturer;
        this.isCamera = isCamera;
    }

    // Getters
    public String getIpAddress() {
        return ipAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public String getHostname() {
        return hostname;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public boolean isCamera() {
        return isCamera;
    }
}
