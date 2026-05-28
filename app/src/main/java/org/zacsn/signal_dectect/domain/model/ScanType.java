package org.zacsn.signal_dectect.domain.model;

/**
 * Represents the type of scan to perform using bitmask.
 * Bitmask values: 1=Bluetooth, 2=WiFi, 4=Cellular
 */
public class ScanType {
    private final boolean bluetooth;
    private final boolean wifi;
    private final boolean cellular;

    public ScanType(boolean bluetooth, boolean wifi, boolean cellular) {
        this.bluetooth = bluetooth;
        this.wifi = wifi;
        this.cellular = cellular;
    }

    /**
     * Convert scan type to integer bitmask.
     * @return Integer bitmask (1=BT, 2=WiFi, 4=Cellular)
     */
    public int toInt() {
        int result = 0;
        if (bluetooth) result |= 1;
        if (wifi) result |= 2;
        if (cellular) result |= 4;
        return result;
    }

    /**
     * Create ScanType from integer bitmask.
     * @param value Integer bitmask
     * @return ScanType instance
     */
    public static ScanType fromInt(int value) {
        return new ScanType(
            (value & 1) != 0,
            (value & 2) != 0,
            (value & 4) != 0
        );
    }

    // Predefined scan types
    public static final ScanType ALL = new ScanType(true, true, true);
    public static final ScanType BLUETOOTH_ONLY = new ScanType(true, false, false);
    public static final ScanType WIFI_ONLY = new ScanType(false, true, false);
    public static final ScanType CELLULAR_ONLY = new ScanType(false, false, true);

    // Getters
    public boolean isBluetooth() {
        return bluetooth;
    }

    public boolean isWifi() {
        return wifi;
    }

    public boolean isCellular() {
        return cellular;
    }
}
