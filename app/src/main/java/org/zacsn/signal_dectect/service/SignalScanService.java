package org.zacsn.signal_dectect.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

import org.zacsn.signal_dectect.MainActivity;
import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.scanner.BluetoothScanController;
import org.zacsn.signal_dectect.data.scanner.CellularScanController;
import org.zacsn.signal_dectect.data.scanner.WiFiScanController;
import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Foreground service for continuous signal scanning.
 */
@AndroidEntryPoint
public class SignalScanService extends Service {
    
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "signal_scan_channel";
    
    @Inject
    BluetoothScanController bluetoothController;
    
    @Inject
    WiFiScanController wifiController;
    
    @Inject
    CellularScanController cellularController;
    
    private final IBinder binder = new LocalBinder();
    private final List<SignalDevice> allDevices = new CopyOnWriteArrayList<>();
    private ScanType currentScanType;
    private ServiceCallback callback;
    
    public interface ServiceCallback {
        void onDeviceFound(SignalDevice device);
        void onDeviceListUpdated(List<SignalDevice> devices);
    }
    
    public class LocalBinder extends Binder {
        public SignalScanService getService() {
            return SignalScanService.this;
        }
    }

    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        setupScanListeners();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(0));
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    public void setCallback(ServiceCallback callback) {
        this.callback = callback;
    }
    
    /**
     * Start scanning with specified scan type.
     */
    public void startScan(ScanType scanType) {
        this.currentScanType = scanType;
        
        // Stop all scanners first
        bluetoothController.stopScan();
        wifiController.stopScan();
        cellularController.stopScan();
        
        // Clear device list
        allDevices.clear();
        
        // Start only the requested scanners
        if (scanType.isBluetooth()) {
            bluetoothController.startScan();
        }
        if (scanType.isWifi()) {
            wifiController.startScan();
        }
        if (scanType.isCellular()) {
            cellularController.startScan();
        }
    }
    
    /**
     * Stop all scanning.
     */
    public void stopScan() {
        bluetoothController.stopScan();
        wifiController.stopScan();
        cellularController.stopScan();
    }
    
    public List<SignalDevice> getAllDevices() {
        return new ArrayList<>(allDevices);
    }

    
    private void setupScanListeners() {
        bluetoothController.setScanListener(new BluetoothScanController.ScanListener() {
            @Override
            public void onDeviceFound(SignalDevice device) {
                handleDeviceFound(device);
            }
            
            @Override
            public void onScanError(String error) {
                // Handle error
            }
        });
        
        wifiController.setScanListener(new WiFiScanController.ScanListener() {
            @Override
            public void onDeviceFound(SignalDevice device) {
                handleDeviceFound(device);
            }
            
            @Override
            public void onScanError(String error) {
                // Handle error
            }
        });
        
        cellularController.setScanListener(new CellularScanController.ScanListener() {
            @Override
            public void onSignalUpdate(SignalDevice device) {
                handleDeviceFound(device);
            }
            
            @Override
            public void onScanError(String error) {
                // Handle error
            }
        });
    }
    
    private void handleDeviceFound(SignalDevice device) {
        // Filter device based on current scan type
        if (currentScanType != null && !isDeviceMatchingScanType(device)) {
            return; // Skip devices that don't match current scan type
        }
        
        // Update or add device
        boolean found = false;
        for (int i = 0; i < allDevices.size(); i++) {
            if (allDevices.get(i).getMacAddress().equals(device.getMacAddress())) {
                allDevices.set(i, device);
                found = true;
                break;
            }
        }
        
        if (!found) {
            allDevices.add(device);
        }
        
        updateNotification(allDevices.size());
        
        if (callback != null) {
            callback.onDeviceFound(device);
            callback.onDeviceListUpdated(new ArrayList<>(allDevices));
        }
    }
    
    /**
     * Check if device matches the current scan type.
     */
    private boolean isDeviceMatchingScanType(SignalDevice device) {
        if (currentScanType == null) {
            return true; // No filter, accept all
        }
        
        DeviceType deviceType = device.getDeviceType();
        
        // Check if device type matches scan type
        if (currentScanType.isBluetooth() && 
            (deviceType == DeviceType.BLUETOOTH_LE || deviceType == DeviceType.BLUETOOTH_CLASSIC)) {
            return true;
        }
        
        if (currentScanType.isWifi() && deviceType == DeviceType.WIFI) {
            return true;
        }
        
        if (currentScanType.isCellular() && deviceType == DeviceType.CELLULAR) {
            return true;
        }
        
        return false;
    }

    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "信号扫描服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("持续扫描蓝牙、WiFi和蜂窝信号");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(int deviceCount) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        );
        
        String contentText = deviceCount > 0 
            ? "已发现 " + deviceCount + " 个设备" 
            : "正在扫描...";
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("信号检测运行中")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }
    
    private void updateNotification(int deviceCount) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(deviceCount));
        }
    }
    
    @Override
    public void onDestroy() {
        stopScan();
        super.onDestroy();
    }
}
