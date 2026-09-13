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
import org.zacsn.signal_dectect.data.scan.ScanEngine;
import org.zacsn.signal_dectect.data.scan.ScanObserver;
import org.zacsn.signal_dectect.data.scan.ScanSource;
import org.zacsn.signal_dectect.data.scan.BluetoothScanSource;
import org.zacsn.signal_dectect.data.scan.WiFiScanSource;
import org.zacsn.signal_dectect.data.scan.CellularScanSource;
import org.zacsn.signal_dectect.data.scanner.BluetoothScanController;
import org.zacsn.signal_dectect.data.scanner.CellularScanController;
import org.zacsn.signal_dectect.data.scanner.WiFiScanController;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Foreground service for continuous signal scanning.
 *
 * 设备聚合、去重、在线状态与过期清理已全部下沉到单线程的 {@link ScanEngine}。
 * 本 Service 只负责：前台通知与生命周期、把三路采集器的观测投递给引擎、
 * 把引擎的列表更新回调转发给上层（Repository / ViewModel）。
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
    private final ScanEngine engine = new ScanEngine();
    private List<ScanSource> scanSources;
    private ScanType currentScanType;
    private ServiceCallback callback;
    private boolean isScanning = false;
    /** 通知里只显示设备数量，数量没变就没必要重发（系统对高频通知会限流丢帧）。 */
    private int lastNotifiedCount = -1;

    public interface ServiceCallback {
        void onDeviceFound(SignalDevice device);
        void onDeviceListUpdated(List<SignalDevice> devices);
        void onScanError(String error);
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
        // 引擎在其后台线程上发出列表快照；此处只做通知刷新与向上转发。
        engine.setListener(devices -> {
            updateNotification(devices.size());
            if (callback != null) {
                callback.onDeviceListUpdated(new ArrayList<>(devices));
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification(0));
        lastNotifiedCount = 0;
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
        this.isScanning = true;

        // Stop all sources first
        for (ScanSource source : scanSources) {
            source.stop();
        }

        // 重置聚合状态；引擎按扫描类型过滤观测。
        engine.start(scanType);
        if (callback != null) {
            callback.onDeviceListUpdated(new ArrayList<>());
        }

        // Start only the sources that apply to the requested scan type
        for (ScanSource source : scanSources) {
            if (source.appliesTo(scanType)) {
                source.start();
            }
        }
    }

    /**
     * Stop all scanning.
     */
    public void stopScan() {
        isScanning = false;
        engine.stop();
        for (ScanSource source : scanSources) {
            source.stop();
        }
    }

    public List<SignalDevice> getAllDevices() {
        return new ArrayList<>(engine.allDevices());
    }

    public List<SignalDevice> getSessionDevices() {
        return new ArrayList<>(engine.sessionDevices());
    }

    private void setupScanListeners() {
        scanSources = java.util.Arrays.asList(
            new BluetoothScanSource(bluetoothController),
            new WiFiScanSource(wifiController)
        );
        for (ScanSource source : scanSources) {
            source.setObserver(new ScanObserver() {
                @Override
                public void onDevice(SignalDevice device) {
                    handleDeviceFound(device);
                }

                @Override
                public void onError(String message) {
                    handleScanError(source.label() + ": " + message);
                }
            });
        }
    }

    private void handleScanError(String error) {
        if (callback != null) {
            callback.onScanError(error);
        }
    }

    void handleDeviceFound(SignalDevice device) {
        engine.submit(device);
        if (callback != null) {
            callback.onDeviceFound(device);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "信号扫描服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("持续扫描蓝牙和WiFi信号");

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
        if (deviceCount == lastNotifiedCount) {
            return;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createNotification(deviceCount));
            lastNotifiedCount = deviceCount;
        }
    }

    @Override
    public void onDestroy() {
        stopScan();
        engine.shutdown();
        super.onDestroy();
    }
}
