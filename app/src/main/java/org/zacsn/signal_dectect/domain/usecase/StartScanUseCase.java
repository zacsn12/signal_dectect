package org.zacsn.signal_dectect.domain.usecase;

import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.util.PermissionManager;

import javax.inject.Inject;

public class StartScanUseCase {
    
    private final ScanRepository scanRepository;
    private final PermissionManager permissionManager;
    
    @Inject
    public StartScanUseCase(ScanRepository scanRepository, PermissionManager permissionManager) {
        this.scanRepository = scanRepository;
        this.permissionManager = permissionManager;
    }
    
    public boolean execute(ScanType scanType, ScanRepository.ScanCallback callback) {
        if (!permissionManager.hasRequiredPermissions(scanType)) {
            return false;
        }
        
        scanRepository.startScan(scanType, callback);
        return true;
    }
}
