package org.zacsn.signal_dectect.domain.usecase;

import org.zacsn.signal_dectect.data.repository.DeviceRepository;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import javax.inject.Inject;

public class AddToWhitelistUseCase {
    
    private final DeviceRepository deviceRepository;
    
    @Inject
    public AddToWhitelistUseCase(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
    
    public void execute(SignalDevice device) {
        deviceRepository.addToWhitelist(device);
    }
}
