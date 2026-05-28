package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.data.repository.DeviceRepository;
import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.domain.usecase.AddToBlacklistUseCase;
import org.zacsn.signal_dectect.domain.usecase.AddToWatchlistUseCase;
import org.zacsn.signal_dectect.domain.usecase.AddToWhitelistUseCase;
import org.zacsn.signal_dectect.domain.usecase.StartScanUseCase;
import org.zacsn.signal_dectect.domain.usecase.StopScanUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for Signal Inspection screen.
 */
@HiltViewModel
public class SignalInspectViewModel extends ViewModel {
    
    private final StartScanUseCase startScanUseCase;
    private final StopScanUseCase stopScanUseCase;
    private final AddToWatchlistUseCase addToWatchlistUseCase;
    private final AddToBlacklistUseCase addToBlacklistUseCase;
    private final AddToWhitelistUseCase addToWhitelistUseCase;
    private final DeviceRepository deviceRepository;
    
    private final MutableLiveData<ScanState> scanState = new MutableLiveData<>(ScanState.IDLE);
    private final MutableLiveData<List<SignalDevice>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Long> scanDuration = new MutableLiveData<>(0L);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    private long scanStartTime = 0;
    private DeviceType filterType = null;
    private Double maxDistance = null;

    
    @Inject
    public SignalInspectViewModel(
        StartScanUseCase startScanUseCase,
        StopScanUseCase stopScanUseCase,
        AddToWatchlistUseCase addToWatchlistUseCase,
        AddToBlacklistUseCase addToBlacklistUseCase,
        AddToWhitelistUseCase addToWhitelistUseCase,
        DeviceRepository deviceRepository
    ) {
        this.startScanUseCase = startScanUseCase;
        this.stopScanUseCase = stopScanUseCase;
        this.addToWatchlistUseCase = addToWatchlistUseCase;
        this.addToBlacklistUseCase = addToBlacklistUseCase;
        this.addToWhitelistUseCase = addToWhitelistUseCase;
        this.deviceRepository = deviceRepository;
    }
    
    public void startScan(ScanType scanType) {
        // Clear device list before starting new scan
        devices.setValue(new ArrayList<>());
        
        boolean success = startScanUseCase.execute(scanType, new ScanRepository.ScanCallback() {
            @Override
            public void onDeviceFound(SignalDevice device) {
                // Device found callback
            }
            
            @Override
            public void onDeviceListUpdated(List<SignalDevice> deviceList) {
                List<SignalDevice> filtered = applyFilters(deviceList);
                devices.postValue(filtered);
            }
        });
        
        if (success) {
            scanState.setValue(ScanState.SCANNING);
            scanStartTime = System.currentTimeMillis();
            startDurationTimer();
        } else {
            scanState.setValue(ScanState.ERROR);
            errorMessage.setValue("权限未授予");
        }
    }
    
    public void stopScan() {
        stopScanUseCase.execute();
        scanState.setValue(ScanState.IDLE);
        scanDuration.setValue(0L);
        // Don't clear device list when stopping scan - keep the results visible
    }
    
    private void startDurationTimer() {
        // Update duration every second
        new Thread(() -> {
            while (scanState.getValue() == ScanState.SCANNING) {
                long duration = System.currentTimeMillis() - scanStartTime;
                scanDuration.postValue(duration / 1000);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    
    public void filterByType(DeviceType type) {
        this.filterType = type;
        List<SignalDevice> currentDevices = devices.getValue();
        if (currentDevices != null) {
            devices.setValue(applyFilters(currentDevices));
        }
    }
    
    public void filterByRange(double maxDistance) {
        this.maxDistance = maxDistance;
        List<SignalDevice> currentDevices = devices.getValue();
        if (currentDevices != null) {
            devices.setValue(applyFilters(currentDevices));
        }
    }
    
    private List<SignalDevice> applyFilters(List<SignalDevice> deviceList) {
        List<SignalDevice> filtered = new ArrayList<>(deviceList);
        
        if (filterType != null) {
            filtered = filtered.stream()
                .filter(d -> d.getDeviceType() == filterType)
                .collect(Collectors.toList());
        }
        
        if (maxDistance != null) {
            filtered = filtered.stream()
                .filter(d -> d.getDistance() <= maxDistance)
                .collect(Collectors.toList());
        }
        
        return filtered;
    }
    
    public void addToWatchlist(SignalDevice device) {
        addToWatchlistUseCase.execute(device);
    }
    
    public void addToBlacklist(SignalDevice device) {
        addToBlacklistUseCase.execute(device, "用户标记");
    }
    
    public void addToWhitelist(SignalDevice device) {
        addToWhitelistUseCase.execute(device);
    }
    
    // Getters
    public LiveData<ScanState> getScanState() {
        return scanState;
    }
    
    public LiveData<List<SignalDevice>> getDevices() {
        return devices;
    }
    
    public LiveData<Long> getScanDuration() {
        return scanDuration;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public enum ScanState {
        IDLE, SCANNING, ERROR
    }
}
