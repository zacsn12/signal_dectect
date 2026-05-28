package org.zacsn.signal_dectect.di;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import org.zacsn.signal_dectect.data.repository.DeviceRepository;
import org.zacsn.signal_dectect.data.repository.DeviceRepositoryImpl;
import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.data.repository.ScanRepositoryImpl;

import javax.inject.Singleton;

/**
 * Hilt module for providing repository implementations.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {
    
    @Binds
    @Singleton
    public abstract ScanRepository bindScanRepository(ScanRepositoryImpl impl);
    
    @Binds
    @Singleton
    public abstract DeviceRepository bindDeviceRepository(DeviceRepositoryImpl impl);
}
