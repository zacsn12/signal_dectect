package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.domain.model.ScanRecord;
import java.util.List;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class RecordViewModel extends ViewModel {
    
    private final ScanRepository scanRepository;
    private final LiveData<List<ScanRecord>> scanHistory;
    
    @Inject
    public RecordViewModel(ScanRepository scanRepository) {
        this.scanRepository = scanRepository;
        this.scanHistory = scanRepository.getScanHistory();
    }
    
    public LiveData<List<ScanRecord>> getScanHistory() {
        return scanHistory;
    }
    
    public void deleteRecord(long recordId) {
        scanRepository.deleteScanRecord(recordId);
    }
}
