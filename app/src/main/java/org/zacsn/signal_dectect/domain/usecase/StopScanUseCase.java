package org.zacsn.signal_dectect.domain.usecase;

import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.domain.model.ScanRecord;

import javax.inject.Inject;

public class StopScanUseCase {
    
    private final ScanRepository scanRepository;
    
    @Inject
    public StopScanUseCase(ScanRepository scanRepository) {
        this.scanRepository = scanRepository;
    }
    
    public ScanRecord execute() {
        return scanRepository.stopScan();
    }
}
