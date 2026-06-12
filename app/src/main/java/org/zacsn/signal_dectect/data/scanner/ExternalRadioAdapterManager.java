package org.zacsn.signal_dectect.data.scanner;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import java.util.HashMap;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class ExternalRadioAdapterManager {
    private final Context context;

    @Inject
    public ExternalRadioAdapterManager(@ApplicationContext Context context) {
        this.context = context;
    }

    ScanSourceSelection selectBluetoothSource(boolean androidFrameworkAvailable) {
        boolean externalDetected = hasUsbBluetoothLikeAdapter();
        if (hasUsableExternalBluetoothBackend()) {
            return new ScanSourceSelection(
                    ScanSourceSelection.Source.EXTERNAL_ADAPTER,
                    true,
                    "已选择外接蓝牙适配器"
            );
        }

        if (androidFrameworkAvailable) {
            return new ScanSourceSelection(
                    ScanSourceSelection.Source.ANDROID_FRAMEWORK,
                    externalDetected,
                    externalDetected
                            ? "检测到外接蓝牙设备，但 Android 未开放通用 USB 蓝牙扫描接口，使用系统当前蓝牙适配器"
                            : "未检测到可用外接蓝牙设备，使用本机蓝牙适配器"
            );
        }

        return new ScanSourceSelection(
                ScanSourceSelection.Source.UNAVAILABLE,
                externalDetected,
                externalDetected
                        ? "检测到外接蓝牙设备，但系统蓝牙不可用且未接入外接蓝牙扫描后端"
                        : "蓝牙不可用或未开启"
        );
    }

    ScanSourceSelection selectWifiSource(boolean androidFrameworkAvailable) {
        boolean externalDetected = hasUsbWifiLikeAdapter();
        if (hasUsableExternalWifiBackend()) {
            return new ScanSourceSelection(
                    ScanSourceSelection.Source.EXTERNAL_ADAPTER,
                    true,
                    "已选择外接 WiFi 适配器"
            );
        }

        if (androidFrameworkAvailable) {
            return new ScanSourceSelection(
                    ScanSourceSelection.Source.ANDROID_FRAMEWORK,
                    externalDetected,
                    externalDetected
                            ? "检测到外接 WiFi 设备，但 Android 未开放通用 USB WiFi 扫描接口，使用系统当前 WiFi 适配器"
                            : "未检测到可用外接 WiFi 设备，使用本机 WiFi 适配器"
            );
        }

        return new ScanSourceSelection(
                ScanSourceSelection.Source.UNAVAILABLE,
                externalDetected,
                externalDetected
                        ? "检测到外接 WiFi 设备，但系统 WiFi 不可用且未接入外接 WiFi 扫描后端"
                        : "WiFi 不可用"
        );
    }

    private boolean hasUsableExternalBluetoothBackend() {
        return false;
    }

    private boolean hasUsableExternalWifiBackend() {
        return false;
    }

    private boolean hasUsbBluetoothLikeAdapter() {
        for (UsbDevice device : getUsbDevices().values()) {
            if (isBluetoothClass(device.getDeviceClass())
                    || hasInterfaceClass(device, UsbConstants.USB_CLASS_WIRELESS_CONTROLLER)) {
                return true;
            }

            String name = (device.getProductName() + " " + device.getManufacturerName())
                    .toLowerCase(Locale.US);
            if (name.contains("bluetooth") || name.contains("bt")) {
                return true;
            }
        }
        return false;
    }

    private boolean hasUsbWifiLikeAdapter() {
        for (UsbDevice device : getUsbDevices().values()) {
            if (hasInterfaceClass(device, UsbConstants.USB_CLASS_WIRELESS_CONTROLLER)
                    || hasInterfaceClass(device, UsbConstants.USB_CLASS_COMM)
                    || hasInterfaceClass(device, UsbConstants.USB_CLASS_VENDOR_SPEC)) {
                String name = (device.getProductName() + " " + device.getManufacturerName())
                        .toLowerCase(Locale.US);
                if (name.contains("wifi")
                        || name.contains("wi-fi")
                        || name.contains("wireless")
                        || name.contains("802.11")
                        || name.contains("wlan")) {
                    return true;
                }
            }
        }
        return false;
    }

    private HashMap<String, UsbDevice> getUsbDevices() {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return new HashMap<>();
        }
        return usbManager.getDeviceList();
    }

    private boolean hasInterfaceClass(UsbDevice device, int usbClass) {
        if (device == null) {
            return false;
        }
        for (int i = 0; i < device.getInterfaceCount(); i++) {
            UsbInterface usbInterface = device.getInterface(i);
            if (usbInterface != null && usbInterface.getInterfaceClass() == usbClass) {
                return true;
            }
        }
        return false;
    }

    private boolean isBluetoothClass(int usbClass) {
        return usbClass == UsbConstants.USB_CLASS_WIRELESS_CONTROLLER;
    }
}
