package org.zacsn.signal_dectect.domain.model;

/**
 * Enumeration of device types that can be detected by the signal scanner.
 */
public enum DeviceType {
    /**
     * Classic Bluetooth device (BR/EDR)
     */
    BLUETOOTH_CLASSIC,

    /**
     * Bluetooth Low Energy (BLE) device
     */
    BLUETOOTH_LE,

    /**
     * WiFi network or access point
     */
    WIFI,

    /**
     * Cellular network signal
     */
    CELLULAR
}
