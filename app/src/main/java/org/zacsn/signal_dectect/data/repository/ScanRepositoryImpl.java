package org.zacsn.signal_dectect.data.repository;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
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
    private ScanCallback currentCallback;
    private long scanStartTime;
    private ScanType currentScanType;

    
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
                    if (currentCallback != null) {
                        currentCallback.onDeviceListUpdated(devices);
                    }
                }
            });
            
            if (currentScanType != null) {
                scanService.startScan(currentScanType);
            }
        }
        
        @Override
        public void onServiceDisconnected(ComponentName name) {
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
        context.startService(intent);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    public ScanRecord stopScan() {
        List<SignalDevice> devices = getCurrentDevices();
        long duration = System.currentTimeMillis() - scanStartTime;
        
        ScanRecord record = new ScanRecord(
            0,
            scanStartTime,
            currentScanType,
            duration,
            null, // latitude
            null, // longitude
            devices.size(),
            devices
        );
        
        // Don't save to database automatically - let the caller decide
        // The caller (SignalInspectActivity) will ask user if they want to save
        
        // Stop service
        if (serviceBound && scanService != null) {
            scanService.stopScan();
            context.unbindService(serviceConnection);
            serviceBound = false;
        }
        
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
                    records.add(EntityMappers.toDomain(entity));
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
        return new ArrayList<>();
    }
}
