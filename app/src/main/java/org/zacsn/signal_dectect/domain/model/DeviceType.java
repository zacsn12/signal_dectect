package org.zacsn.signal_dectect.domain.model;

import com.google.gson.annotations.SerializedName;

/**
 * Enumeration of device types that can be detected by the signal scanner.
 */
public enum DeviceType {
    /**
     * Classic Bluetooth device (BR/EDR)
     */
    @SerializedName("BLUETOOTH_CLASSIC")
    BLUETOOTH_CLASSIC,

    /**
     * Bluetooth Low Energy (BLE) device
     */
    @SerializedName("BLUETOOTH_LE")
    BLUETOOTH_LE,

    /**
     * WiFi network or access point
     */
    @SerializedName("WIFI")
    WIFI,

    /**
     * Cellular network signal
     */
    @SerializedName("CELLULAR")
    CELLULAR
}
