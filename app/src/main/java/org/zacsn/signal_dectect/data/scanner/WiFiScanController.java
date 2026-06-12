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
import android.util.Log;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.DistanceUtils;
import org.zacsn.signal_dectect.util.BluetoothManufacturerUtils;
import org.zacsn.signal_dectect.util.MacVendorUtils;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for WiFi network scanning.
 */
@Singleton
public class WiFiScanController {
    private static final String TAG = "WiFiScanController";
    private static final long WIFI_SCAN_INTERVAL_MS = 10_000L;
    
    private final Context context;
    private final WifiManager wifiManager;
    private final ExternalRadioAdapterManager externalAdapterManager;
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
    public WiFiScanController(
            @ApplicationContext Context context,
            ExternalRadioAdapterManager externalAdapterManager
    ) {
        this.context = context;
        this.externalAdapterManager = externalAdapterManager;
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
        boolean frameworkAvailable = wifiManager != null;
        ScanSourceSelection scanSource = externalAdapterManager.selectWifiSource(frameworkAvailable);
        Log.i(TAG, scanSource.getMessage());

        if (!scanSource.canUseAndroidFramework()) {
            if (scanListener != null) {
                scanListener.onScanError(scanSource.getMessage());
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
            if (bssid == null || bssid.isEmpty()) {
                continue;
            }
            String deviceKey = bssid.toUpperCase(Locale.US);
            String ssid = result.SSID;
            if (ssid == null || ssid.isEmpty()) {
                ssid = "隐藏WiFi网络";
            }

            SignalDevice previous = deviceMap.get(deviceKey);
            int signalStrength = SignalDeviceStabilizer.smoothSignalStrength(previous, result.level);
            ManufacturerEvidence manufacturerEvidence = evaluateWifiManufacturer(deviceKey, ssid);
            long now = System.currentTimeMillis();
            
            SignalDevice device = new SignalDevice(
                deviceKey,
                ssid,
                DeviceType.WIFI,
                "未确认",
                manufacturerEvidence.manufacturer,
                manufacturerEvidence.source,
                manufacturerEvidence.confidence,
                manufacturerEvidence.verdict,
                manufacturerEvidence.evidence,
                signalStrength,
                result.frequency,
                calculateDistance(signalStrength, result.frequency),
                now,
                now,
                false,
                false,
                false
            );
            device = SignalDeviceStabilizer.merge(previous, device);
            
            deviceMap.put(deviceKey, device);
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
                    boolean scanStarted = wifiManager.startScan();
                    if (!scanStarted) {
                        processScanResults();
                    }
                    handler.postDelayed(this, WIFI_SCAN_INTERVAL_MS);
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
        if (rssi >= 0 || rssi < -120 || frequency <= 0) {
            return DistanceUtils.UNKNOWN_DISTANCE;
        }
        double exp = (27.55 - (20 * Math.log10(frequency)) + Math.abs(rssi)) / 20.0;
        return DistanceUtils.sanitize(Math.pow(10.0, exp));
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

    private boolean isKnownManufacturer(String manufacturer) {
        return manufacturer != null
            && !manufacturer.equals("未知")
            && !manufacturer.equals("未知厂商")
            && !manufacturer.equals("随机地址");
    }

    private ManufacturerEvidence evaluateWifiManufacturer(String bssid, String ssid) {
        ManufacturerEvidence best = ManufacturerEvidence.unknown();
        boolean randomizedBssid = isLocallyAdministeredAddress(bssid);

        String ouiManufacturer = randomizedBssid ? null : MacVendorUtils.getVendor(bssid);
        if (isKnownManufacturer(ouiManufacturer)) {
            best = best.better(ManufacturerEvidence.likely(
                    ouiManufacturer,
                    "wifi_bssid_oui",
                    80,
                    "WiFi BSSID OUI命中: " + ouiManufacturer
            ));
        } else if (randomizedBssid) {
            best = best.better(ManufacturerEvidence.unknown(
                    "wifi_randomized_bssid",
                    "BSSID疑似随机/本地管理地址，未采用OUI判断"
            ));
        }

        String ssidManufacturer = inferManufacturerFromSsid(ssid);
        if (isKnownManufacturer(ssidManufacturer)) {
            int confidence = randomizedBssid ? 70 : 75;
            String source = "wifi_ssid_name";
            String evidence = "SSID品牌线索: " + ssid;
            if (isKnownManufacturer(ouiManufacturer)
                    && BluetoothManufacturerUtils.isSameManufacturer(ouiManufacturer, ssidManufacturer)) {
                best = best.better(ManufacturerEvidence.confirmed(
                        ssidManufacturer,
                        "wifi_bssid_ssid_match",
                        95,
                        "WiFi BSSID OUI与SSID品牌一致: " + ouiManufacturer + " / " + ssid
                ));
                return best;
            }
            best = best.better(ManufacturerEvidence.likely(
                    ssidManufacturer,
                    source,
                    confidence,
                    evidence
            ));
        }

        return best;
    }

    private boolean isLocallyAdministeredAddress(String macAddress) {
        if (macAddress == null || macAddress.length() < 2) {
            return true;
        }
        try {
            int firstOctet = Integer.parseInt(macAddress.substring(0, 2), 16);
            return (firstOctet & 0x02) != 0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private String inferManufacturerFromSsid(String ssid) {
        if (ssid == null) {
            return null;
        }

        String normalized = ssid.toLowerCase(Locale.US);
        if (normalized.contains("iphone") || normalized.contains("ipad")
                || normalized.contains("apple") || normalized.contains("airdrop")) {
            return "Apple, Inc.";
        }
        if (normalized.contains("galaxy") || normalized.contains("samsung")) {
            return "Samsung Electronics Co. Ltd.";
        }
        if (normalized.contains("xiaomi") || normalized.contains("redmi")
                || normalized.contains("mi ") || normalized.startsWith("mi_")
                || normalized.startsWith("mi-")) {
            return "Xiaomi Inc.";
        }
        if (normalized.contains("huawei") || normalized.contains("honor")) {
            return "Huawei Technologies Co., Ltd.";
        }
        if (normalized.contains("oppo")) {
            return "OPPO Mobile Telecommunications Corp., Ltd.";
        }
        if (normalized.contains("vivo") || normalized.contains("iqoo")) {
            return "vivo Mobile Communication Co., Ltd.";
        }
        if (normalized.contains("oneplus")) {
            return "OnePlus Electronics (Shenzhen) Co., Ltd.";
        }
        if (normalized.contains("pixel") || normalized.contains("google")) {
            return "Google";
        }
        if (normalized.contains("realme")) {
            return "Realme Chongqing Mobile";
        }
        return null;
    }
    
    public ConcurrentHashMap<String, SignalDevice> getDeviceMap() {
        return deviceMap;
    }

    private static final class ManufacturerEvidence {
        private final String manufacturer;
        private final String source;
        private final int confidence;
        private final ManufacturerVerdict verdict;
        private final String evidence;

        private ManufacturerEvidence(
                String manufacturer,
                String source,
                int confidence,
                ManufacturerVerdict verdict,
                String evidence
        ) {
            this.manufacturer = manufacturer;
            this.source = source;
            this.confidence = confidence;
            this.verdict = verdict;
            this.evidence = evidence;
        }

        private static ManufacturerEvidence unknown() {
            return new ManufacturerEvidence("未确认", "unknown", 0, ManufacturerVerdict.UNKNOWN, "");
        }

        private static ManufacturerEvidence unknown(String source, String evidence) {
            return new ManufacturerEvidence("未确认", source, 0, ManufacturerVerdict.UNKNOWN, evidence);
        }

        private static ManufacturerEvidence possible(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.POSSIBLE, evidence);
        }

        private static ManufacturerEvidence likely(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.LIKELY, evidence);
        }

        private static ManufacturerEvidence confirmed(
                String manufacturer,
                String source,
                int confidence,
                String evidence
        ) {
            return new ManufacturerEvidence(manufacturer, source, confidence, ManufacturerVerdict.CONFIRMED, evidence);
        }

        private ManufacturerEvidence better(ManufacturerEvidence other) {
            if (other == null) {
                return this;
            }
            if (other.confidence > confidence) {
                return other;
            }
            if (other.confidence == confidence
                    && (evidence == null || evidence.isEmpty())
                    && other.evidence != null
                    && !other.evidence.isEmpty()) {
                return other;
            }
            return this;
        }
    }
}
