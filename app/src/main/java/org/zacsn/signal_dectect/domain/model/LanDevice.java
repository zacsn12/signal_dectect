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
    private final String deviceCategory;
    private final String discoveryMethod;
    private final int confidence;
    private final boolean isGateway;
    private final long lastSeen;

    public LanDevice(String ipAddress, String macAddress, String hostname,
                    String manufacturer, boolean isCamera) {
        this(ipAddress, macAddress, hostname, manufacturer, isCamera,
                isCamera ? "摄像头设备" : "网络设备", "网络探测", 50, false,
                System.currentTimeMillis());
    }

    public LanDevice(String ipAddress, String macAddress, String hostname,
                    String manufacturer, boolean isCamera, String deviceCategory,
                    String discoveryMethod, int confidence, boolean isGateway,
                    long lastSeen) {
        this.ipAddress = ipAddress;
        this.macAddress = macAddress;
        this.hostname = hostname;
        this.manufacturer = manufacturer;
        this.isCamera = isCamera;
        this.deviceCategory = deviceCategory;
        this.discoveryMethod = discoveryMethod;
        this.confidence = confidence;
        this.isGateway = isGateway;
        this.lastSeen = lastSeen;
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

    public String getDeviceCategory() {
        return deviceCategory;
    }

    public String getDiscoveryMethod() {
        return discoveryMethod;
    }

    public int getConfidence() {
        return confidence;
    }

    public boolean isGateway() {
        return isGateway;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LanDevice lanDevice = (LanDevice) o;
        return isCamera == lanDevice.isCamera
                && confidence == lanDevice.confidence
                && isGateway == lanDevice.isGateway
                && lastSeen == lanDevice.lastSeen
                && java.util.Objects.equals(ipAddress, lanDevice.ipAddress)
                && java.util.Objects.equals(macAddress, lanDevice.macAddress)
                && java.util.Objects.equals(hostname, lanDevice.hostname)
                && java.util.Objects.equals(manufacturer, lanDevice.manufacturer)
                && java.util.Objects.equals(deviceCategory, lanDevice.deviceCategory)
                && java.util.Objects.equals(discoveryMethod, lanDevice.discoveryMethod);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(ipAddress, macAddress, hostname, manufacturer,
                isCamera, deviceCategory, discoveryMethod, confidence, isGateway, lastSeen);
    }
}
