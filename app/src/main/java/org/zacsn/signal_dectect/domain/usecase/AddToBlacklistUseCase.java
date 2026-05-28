package org.zacsn.signal_dectect.domain.usecase;

import org.zacsn.signal_dectect.data.repository.DeviceRepository;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import javax.inject.Inject;

public class AddToBlacklistUseCase {
    
    private final DeviceRepository deviceRepository;
    
    @Inject
    public AddToBlacklistUseCase(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    
    public void execute(SignalDevice device, String reason) {
        deviceRepository.addToBlacklist(device, reason);
    }
}
