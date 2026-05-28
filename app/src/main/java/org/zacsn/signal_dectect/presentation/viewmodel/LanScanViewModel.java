package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import org.zacsn.signal_dectect.domain.model.LanDevice;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LanScanViewModel extends ViewModel {
    
    private final MutableLiveData<LanScanState> scanState = new MutableLiveData<>(LanScanState.IDLE);
    private final MutableLiveData<List<LanDevice>> devices = new MutableLiveData<>(new ArrayList<>());
    
    @Inject
    public LanScanViewModel() {}
    
    public void startScan() {
        scanState.setValue(LanScanState.SCANNING);
        // LAN scanning logic would go here
    }
    
    public void stopScan() {
        scanState.setValue(LanScanState.IDLE);
    }
    
    public LiveData<LanScanState> getScanState() {
        return scanState;
    }
    
    public LiveData<List<LanDevice>> getDevices() {
        return devices;
    }
    
    public enum LanScanState {
        IDLE, SCANNING, COMPLETE, ERROR
    }
}
