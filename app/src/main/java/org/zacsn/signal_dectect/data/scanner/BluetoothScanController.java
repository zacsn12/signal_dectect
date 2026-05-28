package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanRecord;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.SparseArray;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.MacVendorUtils;
import org.zacsn.signal_dectect.util.BluetoothManufacturerUtils;

import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for Bluetooth (Classic and BLE) device scanning.
 */
@Singleton
public class BluetoothScanController {
    
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final ConcurrentHashMap<String, SignalDevice> deviceMap;
    
    private BluetoothLeScanner bleScanner;
    private ScanCallback bleScanCallback;
    private BroadcastReceiver classicReceiver;
    private boolean isScanning = false;
    private ScanListener scanListener;
    
    public interface ScanListener {
        void onDeviceFound(SignalDevice device);
        void onScanError(String error);
    }

    
    @Inject
    public BluetoothScanController(@ApplicationContext Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.deviceMap = new ConcurrentHashMap<>();
    }
    
    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }
    
    /**
     * Start Bluetooth scanning (both BLE and Classic).
     */
    public void startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            if (scanListener != null) {
                scanListener.onScanError("Bluetooth is not available or disabled");
            }
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
            if (scanListener != null) {
                scanListener.onScanError("Bluetooth scan permission not granted");
            }
            return;
        }
        
        isScanning = true;
        deviceMap.clear();
        
        startBleScan();
        startClassicScan();
    }

    
    /**
     * Start BLE scanning.
     */
    private void startBleScan() {
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) return;
        
        bleScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                BluetoothDevice device = result.getDevice();
                String macAddress = device.getAddress();
                int rssi = result.getRssi();
                
                // Try to get manufacturer from MAC address first
                String manufacturer = MacVendorUtils.getVendor(macAddress);
                
                // If MAC-based lookup failed or returned randomized address,
                // try to get manufacturer from BLE advertisement data
                if (manufacturer.equals("随机地址") || manufacturer.equals("未知厂商") || manufacturer.equals("未知")) {
                    ScanRecord scanRecord = result.getScanRecord();
                    if (scanRecord != null) {
                        SparseArray<byte[]> manufacturerData = scanRecord.getManufacturerSpecificData();
                        if (manufacturerData != null && manufacturerData.size() > 0) {
                            // Get the first manufacturer data entry
                            int companyId = manufacturerData.keyAt(0);
                            String btManufacturer = BluetoothManufacturerUtils.getManufacturer(companyId);
                            if (btManufacturer != null) {
                                manufacturer = btManufacturer;
                            }
                        }
                    }
                }
                
                // Get device name, fallback to vendor/manufacturer name if null
                String deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    // Use manufacturer name unless it's unknown or randomized
                    if (manufacturer.equals("未知") || manufacturer.equals("未知厂商")) {
                        deviceName = "Unknown Device";
                    } else if (manufacturer.equals("随机地址")) {
                        deviceName = "随机地址";
                    } else {
                        deviceName = manufacturer;
                    }
                }
                
                SignalDevice signalDevice = new SignalDevice(
                    macAddress,
                    deviceName,
                    DeviceType.BLUETOOTH_LE,
                    manufacturer,
                    rssi,
                    null,
                    calculateDistance(rssi, -59),
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    false,
                    false,
                    false
                );
                
                deviceMap.put(macAddress, signalDevice);
                if (scanListener != null) {
                    scanListener.onDeviceFound(signalDevice);
                }
            }
            
            @Override
            public void onScanFailed(int errorCode) {
                if (scanListener != null) {
                    scanListener.onScanError("BLE scan failed with error code: " + errorCode);
                }
            }
        };
        
        try {
            bleScanner.startScan(bleScanCallback);
        } catch (SecurityException e) {
            if (scanListener != null) {
                scanListener.onScanError("BLE scan permission error: " + e.getMessage());
            }
        }
    }

    
    /**
     * Start Classic Bluetooth discovery.
     */
    private void startClassicScan() {
        classicReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    if (ActivityCompat.checkSelfPermission(context, 
                            Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    short rssiShort = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                    int rssi = (int) rssiShort;
                    
                    if (device != null) {
                        String macAddress = device.getAddress();
                        
                        // Try to get manufacturer from MAC address
                        String manufacturer = MacVendorUtils.getVendor(macAddress);
                        
                        // Get device name, fallback to manufacturer name if null
                        String deviceName = device.getName();
                        if (deviceName == null || deviceName.isEmpty()) {
                            // Use manufacturer name unless it's unknown or randomized
                            if (manufacturer.equals("未知") || manufacturer.equals("未知厂商")) {
                                deviceName = "Unknown Device";
                            } else if (manufacturer.equals("随机地址")) {
                                deviceName = "随机地址";
                            } else {
                                deviceName = manufacturer;
                            }
                        }
                        
                        SignalDevice signalDevice = new SignalDevice(
                            macAddress,
                            deviceName,
                            DeviceType.BLUETOOTH_CLASSIC,
                            manufacturer,
                            rssi,
                            null,
                            calculateDistance(rssi, -59),
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            false,
                            false,
                            false
                        );
                        
                        deviceMap.put(macAddress, signalDevice);
                        if (scanListener != null) {
                            scanListener.onDeviceFound(signalDevice);
                        }
                    }
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(classicReceiver, filter);
        
        try {
            bluetoothAdapter.startDiscovery();
        } catch (SecurityException e) {
            if (scanListener != null) {
                scanListener.onScanError("Classic Bluetooth scan permission error: " + e.getMessage());
            }
        }
    }

    
    /**
     * Stop Bluetooth scanning.
     */
    public void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        
        // Stop BLE scan
        if (bleScanner != null && bleScanCallback != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bleScanner.stopScan(bleScanCallback);
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Stop Classic Bluetooth discovery
        if (bluetoothAdapter != null) {
            try {
                if (ActivityCompat.checkSelfPermission(context, 
                        Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter.cancelDiscovery();
                }
            } catch (Exception e) {
                // Ignore
            }
        }
        
        // Unregister receiver
        if (classicReceiver != null) {
            try {
                context.unregisterReceiver(classicReceiver);
            } catch (Exception e) {
                // Ignore if not registered
            }
            classicReceiver = null;
        }
    }
    
    /**
     * Calculate distance from RSSI value.
     * 
     * @param rssi Signal strength in dBm
     * @param txPower Transmission power at 1 meter (default: -59 dBm)
     * @return Estimated distance in meters
     */
    private double calculateDistance(int rssi, int txPower) {
        if (rssi == 0) {
            return -1.0;
        }
        
        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10.0);
        } else {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;
        }
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    public ConcurrentHashMap<String, SignalDevice> getDeviceMap() {
        return deviceMap;
    }
}
