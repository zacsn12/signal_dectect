package org.zacsn.signal_dectect.data.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import org.zacsn.signal_dectect.domain.model.EntityMappers;
import org.zacsn.signal_dectect.domain.model.ScanRecord;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.service.SignalScanService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Implementation of ScanRepository.
 */
@Singleton
public class ScanRepositoryImpl implements ScanRepository {
    
    private final Context context;
    private final ScanRecordDao scanRecordDao;
    private final Executor executor;
    
    private SignalScanService scanService;
    private boolean serviceBound = false;
    /**
     * 已调用 bindService 且尚未 unbindService。
     *
     * 与 serviceBound 区分开：bindService 是异步的，连接回调到达前若再 bind 一次，
     * 绑定计数就会加到 2，而 stopScan() 只 unbind 一次，多出来的那次永远泄漏。
     * 服务进程崩溃时 onServiceDisconnected 会清掉 serviceBound，但绑定关系仍在，
     * 此时也不能重复 bind，所以这个标志只由 startScan / stopScan 翻转。
     */
    private boolean bindRequested = false;
    private ScanCallback currentCallback;
    private long scanStartTime;
    private ScanType currentScanType;
    private List<SignalDevice> lastSessionDevices = new ArrayList<>();

    
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            SignalScanService.LocalBinder binder = (SignalScanService.LocalBinder) service;
            scanService = binder.getService();
            serviceBound = true;

            scanService.setCallback(new SignalScanService.ServiceCallback() {
                @Override
                public void onDeviceFound(SignalDevice device) {
                    if (currentCallback != null) {
                        currentCallback.onDeviceFound(device);
                    }
                }
                
                @Override
                public void onDeviceListUpdated(List<SignalDevice> devices) {
                    if (devices != null) {
                        lastSessionDevices = new ArrayList<>(devices);
                    }
                    if (currentCallback != null) {
                        currentCallback.onDeviceListUpdated(devices);
                    }
                }

                @Override
                public void onScanError(String error) {
                    if (currentCallback != null) {
                        currentCallback.onScanError(error);
                    }
                }
            });
            
            if (currentScanType != null) {
                scanService.startScan(currentScanType);
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // 服务进程意外结束；绑定关系仍由系统保留，重连后会再次回调 onServiceConnected，
            // 因此这里不动 bindRequested，避免误判成“未绑定”而重复 bind。
            serviceBound = false;
            scanService = null;
        }
    };
    
    @Inject
    public ScanRepositoryImpl(@ApplicationContext Context context, ScanRecordDao scanRecordDao) {
        this.context = context;
        this.scanRecordDao = scanRecordDao;
        this.executor = Executors.newSingleThreadExecutor();
    }

    
    @Override
    public void startScan(ScanType scanType, ScanCallback callback) {
        this.currentScanType = scanType;
        this.currentCallback = callback;
        this.scanStartTime = System.currentTimeMillis();
        
        Intent intent = new Intent(context, SignalScanService.class);
        ContextCompat.startForegroundService(context, intent);

        if (serviceBound && scanService != null) {
            // 已连接：直接换一轮扫描，不再 bind（否则绑定计数只增不减）。
            scanService.startScan(scanType);
            return;
        }
        if (!bindRequested) {
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            bindRequested = true;
        }
        // 绑定中：currentScanType 已记录，onServiceConnected 会用它启动扫描。
    }
    
    @Override
    public ScanRecord stopScan() {
        List<SignalDevice> devices;
        if (serviceBound && scanService != null) {
            devices = scanService.getSessionDevices();
        } else if (!lastSessionDevices.isEmpty()) {
            devices = new ArrayList<>(lastSessionDevices);
        } else {
            devices = getCurrentDevices();
        }
        if (devices != null && !devices.isEmpty()) {
            lastSessionDevices = new ArrayList<>(devices);
        }
        long duration = System.currentTimeMillis() - scanStartTime;
        
        ScanRecord record = new ScanRecord(
            0,
            scanStartTime,
            currentScanType,
            duration,
            null, // latitude
            null, // longitude
            devices != null ? devices.size() : 0,
            devices != null ? devices : new ArrayList<>()
        );
        
        // Stop service
        if (serviceBound && scanService != null) {
            scanService.stopScan();
        }
        if (bindRequested) {
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                // 已经解绑过（例如服务被系统回收），忽略即可。
            }
            bindRequested = false;
        }
        serviceBound = false;
        scanService = null;


        Intent intent = new Intent(context, SignalScanService.class);
        context.stopService(intent);
        
        return record;
    }
    
    @Override
    public LiveData<List<ScanRecord>> getScanHistory() {
        return Transformations.map(scanRecordDao.getAllRecords(), entities -> {
            List<ScanRecord> records = new ArrayList<>();
            if (entities != null) {
                for (ScanRecordEntity entity : entities) {
                    try {
                        ScanRecord record = EntityMappers.toDomain(entity);
                        if (record != null) {
                            records.add(record);
                        }
                    } catch (Throwable e) {
                        android.util.Log.e("ScanRepositoryImpl", "Error mapping ScanRecordEntity to domain", e);
                    }
                }
            }
            return records;
        });
    }
    
    @Override
    public void deleteScanRecord(long recordId) {
        executor.execute(() -> scanRecordDao.deleteById(recordId));
    }
    
    @Override
    public List<SignalDevice> getCurrentDevices() {
        if (serviceBound && scanService != null) {
            return scanService.getAllDevices();
        }
        return new ArrayList<>(lastSessionDevices);
    }

    @Override
    public List<SignalDevice> getSessionDevices() {
        if (serviceBound && scanService != null) {
            List<SignalDevice> list = scanService.getSessionDevices();
            if (list != null && !list.isEmpty()) {
                lastSessionDevices = new ArrayList<>(list);
                return list;
            }
        }
        return new ArrayList<>(lastSessionDevices);
    }
}
