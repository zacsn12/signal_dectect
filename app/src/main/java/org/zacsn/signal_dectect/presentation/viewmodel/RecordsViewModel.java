package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import javax.inject.Inject;
import dagger.hilt.android.lifecycle.HiltViewModel;
import java.util.List;

@HiltViewModel
public class RecordsViewModel extends ViewModel {
    
    private final ScanRecordDao scanRecordDao;
    private final LiveData<List<ScanRecordEntity>> allRecords;
    
    @Inject
    public RecordsViewModel(ScanRecordDao scanRecordDao) {
        this.scanRecordDao = scanRecordDao;
        this.allRecords = scanRecordDao.getAllRecords();
    }
    
    public LiveData<List<ScanRecordEntity>> getAllRecords() {
        return allRecords;
    }
    
    public void deleteRecord(ScanRecordEntity record) {
        new Thread(() -> scanRecordDao.deleteRecord(record)).start();
    }
    
    public void deleteRecords(java.util.List<Long> recordIds) {
        new Thread(() -> scanRecordDao.deleteByIds(recordIds)).start();
    }
    
    public void updateRecordName(long recordId, String name) {
        new Thread(() -> scanRecordDao.updateRecordName(recordId, name)).start();
    }
}
