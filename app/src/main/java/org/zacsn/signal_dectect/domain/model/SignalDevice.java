package org.zacsn.signal_dectect.domain.model;

import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Represents a detected signal device (Bluetooth, WiFi, or Cellular).
 * This is an immutable domain model with all fields final.
 * 
 * Note: Gson can deserialize final fields using sun.misc.Unsafe,
 * so this class remains immutable while being JSON-serializable.
 */
public class SignalDevice {
    @SerializedName("macAddress")
    private final String macAddress;
    
    @SerializedName("deviceName")
    private final String deviceName;
    
    @SerializedName("deviceType")
    private final DeviceType deviceType;
    
    @SerializedName("manufacturer")
    private final String manufacturer;
    
    @SerializedName("signalStrength")
    private final int signalStrength; // in dBm
    
    @SerializedName("frequency")
    private final Integer frequency; // in MHz (for WiFi)
    
    @SerializedName("distance")
    private final double distance; // in meters
    
    @SerializedName("firstSeen")
    private final long firstSeen;
    
    @SerializedName("lastSeen")
    private final long lastSeen;
    
    @SerializedName("isFocused")
    private final boolean isFocused;
    
    @SerializedName("isBlacklisted")
    private final boolean isBlacklisted;
    
    @SerializedName("isWhitelisted")
    private final boolean isWhitelisted;

    /**
     * Private no-arg constructor for Gson deserialization.
     * This constructor should not be used directly.
     */
    @SuppressWarnings("unused")
    private SignalDevice() {
        this.macAddress = "";
        this.deviceName = "";
        this.deviceType = DeviceType.BLUETOOTH_CLASSIC;
        this.manufacturer = "";
        this.signalStrength = 0;
        this.frequency = null;
        this.distance = 0.0;
        this.firstSeen = 0;
        this.lastSeen = 0;
        this.isFocused = false;
        this.isBlacklisted = false;
        this.isWhitelisted = false;
    }

    public SignalDevice(String macAddress, String deviceName, DeviceType deviceType,
                       String manufacturer, int signalStrength, Integer frequency,
                       double distance, long firstSeen, long lastSeen,
                       boolean isFocused, boolean isBlacklisted, boolean isWhitelisted) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.manufacturer = manufacturer;
        this.signalStrength = signalStrength;
        this.frequency = frequency;
        this.distance = distance;
        this.firstSeen = firstSeen;
        this.lastSeen = lastSeen;
        this.isFocused = isFocused;
        this.isBlacklisted = isBlacklisted;
        this.isWhitelisted = isWhitelisted;
    }

    /**
     * Checks if the device signal has timed out.
     *
     * @param timeoutMs Timeout duration in milliseconds (default: 10 seconds)
     * @return true if the device hasn't been seen within the timeout period
     */
    public boolean isSignalTimeout(long timeoutMs) {
        return System.currentTimeMillis() - lastSeen > timeoutMs;
    }

    public boolean isSignalTimeout() {
        return isSignalTimeout(10000);
    }

    // Getters
    public String getMacAddress() {
        return macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public Integer getFrequency() {
        return frequency;
    }

    public double getDistance() {
        return distance;
    }

    public long getFirstSeen() {
        return firstSeen;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public boolean isFocused() {
        return isFocused;
    }

    public boolean isBlacklisted() {
        return isBlacklisted;
    }

    public boolean isWhitelisted() {
        return isWhitelisted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalDevice that = (SignalDevice) o;
        return signalStrength == that.signalStrength &&
                Double.compare(that.distance, distance) == 0 &&
                firstSeen == that.firstSeen &&
                lastSeen == that.lastSeen &&
                isFocused == that.isFocused &&
                isBlacklisted == that.isBlacklisted &&
                isWhitelisted == that.isWhitelisted &&
                Objects.equals(macAddress, that.macAddress) &&
                Objects.equals(deviceName, that.deviceName) &&
                deviceType == that.deviceType &&
                Objects.equals(manufacturer, that.manufacturer) &&
                Objects.equals(frequency, that.frequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(macAddress, deviceName, deviceType, manufacturer, 
                signalStrength, frequency, distance, firstSeen, lastSeen, 
                isFocused, isBlacklisted, isWhitelisted);
    }

    @Override
    public String toString() {
        return "SignalDevice{" +
                "macAddress='" + macAddress + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceType=" + deviceType +
                ", manufacturer='" + manufacturer + '\'' +
                ", signalStrength=" + signalStrength +
                ", frequency=" + frequency +
                ", distance=" + distance +
                ", firstSeen=" + firstSeen +
                ", lastSeen=" + lastSeen +
                ", isFocused=" + isFocused +
                ", isBlacklisted=" + isBlacklisted +
                ", isWhitelisted=" + isWhitelisted +
                '}';
    }

    /**
     * Builder pattern for creating SignalDevice instances.
     */
    public static class Builder {
        private String macAddress;
        private String deviceName;
        private DeviceType deviceType;
        private String manufacturer;
        private int signalStrength;
        private Integer frequency;
        private double distance;
        private long firstSeen;
        private long lastSeen;
        private boolean isFocused = false;
        private boolean isBlacklisted = false;
        private boolean isWhitelisted = false;

        public Builder macAddress(String macAddress) {
            this.macAddress = macAddress;
            return this;
        }

        public Builder deviceName(String deviceName) {
            this.deviceName = deviceName;
            return this;
        }

        public Builder deviceType(DeviceType deviceType) {
            this.deviceType = deviceType;
            return this;
        }

        public Builder manufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
            return this;
        }

        public Builder signalStrength(int signalStrength) {
            this.signalStrength = signalStrength;
            return this;
        }

        public Builder frequency(Integer frequency) {
            this.frequency = frequency;
            return this;
        }

        public Builder distance(double distance) {
            this.distance = distance;
            return this;
        }

        public Builder firstSeen(long firstSeen) {
            this.firstSeen = firstSeen;
            return this;
        }

        public Builder lastSeen(long lastSeen) {
            this.lastSeen = lastSeen;
            return this;
        }

        public Builder isFocused(boolean isFocused) {
            this.isFocused = isFocused;
            return this;
        }

        public Builder isBlacklisted(boolean isBlacklisted) {
            this.isBlacklisted = isBlacklisted;
            return this;
        }

        public Builder isWhitelisted(boolean isWhitelisted) {
            this.isWhitelisted = isWhitelisted;
            return this;
        }

        public SignalDevice build() {
            return new SignalDevice(macAddress, deviceName, deviceType, manufacturer,
                    signalStrength, frequency, distance, firstSeen, lastSeen,
                    isFocused, isBlacklisted, isWhitelisted);
        }
    }
}
