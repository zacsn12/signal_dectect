package org.zacsn.signal_dectect.data.repository;

import androidx.lifecycle.LiveData;
import org.zacsn.signal_dectect.domain.model.ScanRecord;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.List;

/**
 * Repository interface for scan operations.
 */
public interface ScanRepository {
    
    /**
     * Start a new scan session.
     */
    void startScan(ScanType scanType, ScanCallback callback);
    
    /**
     * Stop current scan and save record.
     */
    ScanRecord stopScan();
    
    /**
     * Get scan history from database.
     */
    LiveData<List<ScanRecord>> getScanHistory();
    
    /**
     * Delete a scan record.
     */
    void deleteScanRecord(long recordId);
    
    /**
     * Get current scanning devices.
     */
    List<SignalDevice> getCurrentDevices();
    
    interface ScanCallback {
        void onDeviceFound(SignalDevice device);
        void onDeviceListUpdated(List<SignalDevice> devices);
    }
}
