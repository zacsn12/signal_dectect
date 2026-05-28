package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.MacVendorUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for WiFi network scanning.
 */
@Singleton
public class WiFiScanController {
    
    private final Context context;
    private final WifiManager wifiManager;
    private final ConcurrentHashMap<String, SignalDevice> deviceMap;
    private final Handler handler;
    
    private BroadcastReceiver scanReceiver;
    private boolean isScanning = false;
    private ScanListener scanListener;
    private Runnable periodicScanRunnable;
    
    public interface ScanListener {
        void onDeviceFound(SignalDevice device);
        void onScanError(String error);
    }

    
    @Inject
    public WiFiScanController(@ApplicationContext Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext()
            .getSystemService(Context.WIFI_SERVICE);
        this.deviceMap = new ConcurrentHashMap<>();
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }
    
    /**
     * Start WiFi scanning with periodic updates.
     */
    public void startScan() {
        if (wifiManager == null) {
            if (scanListener != null) {
                scanListener.onScanError("WiFi is not available");
            }
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            if (scanListener != null) {
                scanListener.onScanError("Location permission not granted");
            }
            return;
        }
        
        isScanning = true;
        deviceMap.clear();
        
        setupScanReceiver();
        startPeriodicScan();
    }

    
    private void setupScanReceiver() {
        scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                    processScanResults();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        context.registerReceiver(scanReceiver, filter);
    }
    
    private void processScanResults() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        List<ScanResult> results = wifiManager.getScanResults();
        
        for (ScanResult result : results) {
            String bssid = result.BSSID;
            String ssid = result.SSID;
            if (ssid == null || ssid.isEmpty()) {
                // For hidden networks, try to use vendor name
                String vendor = MacVendorUtils.getVendor(bssid);
                if (vendor.equals("未知") || vendor.equals("未知厂商")) {
                    ssid = "Hidden Network";
                } else if (vendor.equals("随机地址")) {
                    ssid = "Hidden Network (随机)";
                } else {
                    ssid = vendor + " (Hidden)";
                }
            }
            
            SignalDevice device = new SignalDevice(
                bssid,
                ssid,
                DeviceType.WIFI,
                MacVendorUtils.getVendor(bssid),
                result.level,
                result.frequency,
                calculateDistance(result.level, result.frequency),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                false,
                false,
                false
            );
            
            deviceMap.put(bssid, device);
            if (scanListener != null) {
                scanListener.onDeviceFound(device);
            }
        }
    }

    
    private void startPeriodicScan() {
        periodicScanRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScanning) {
                    wifiManager.startScan();
                    handler.postDelayed(this, 2000); // Scan every 2 seconds
                }
            }
        };
        handler.post(periodicScanRunnable);
    }
    
    /**
     * Stop WiFi scanning.
     */
    public void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        
        if (periodicScanRunnable != null) {
            handler.removeCallbacks(periodicScanRunnable);
        }
        
        if (scanReceiver != null) {
            try {
                context.unregisterReceiver(scanReceiver);
            } catch (Exception e) {
                // Ignore if not registered
            }
            scanReceiver = null;
        }
    }
    
    /**
     * Calculate distance from RSSI and frequency.
     */
    private double calculateDistance(int rssi, int frequency) {
        double exp = (27.55 - (20 * Math.log10(frequency)) + Math.abs(rssi)) / 20.0;
        return Math.pow(10.0, exp);
    }
    
    /**
     * Get WiFi band from frequency.
     */
    public String getWifiBand(int frequency) {
        if (frequency >= 2400 && frequency <= 2500) {
            return "2.4G";
        } else if (frequency >= 4900 && frequency <= 5900) {
            return "5G";
        } else if (frequency >= 5925 && frequency <= 7125) {
            return "6G";
        }
        return "Unknown";
    }
    
    public boolean isScanning() {
        return isScanning;
    }
    
    public ConcurrentHashMap<String, SignalDevice> getDeviceMap() {
        return deviceMap;
    }
}
