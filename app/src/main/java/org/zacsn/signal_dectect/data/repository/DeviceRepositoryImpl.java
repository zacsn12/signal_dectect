package org.zacsn.signal_dectect.data.repository;

import androidx.lifecycle.LiveData;
import org.zacsn.signal_dectect.data.database.*;
import org.zacsn.signal_dectect.domain.model.EntityMappers;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Implementation of DeviceRepository.
 */
@Singleton
public class DeviceRepositoryImpl implements DeviceRepository {
    
    private final WatchlistDao watchlistDao;
    private final BlacklistDao blacklistDao;
    private final WhitelistDao whitelistDao;
    private final Executor executor;
    
    @Inject
    public DeviceRepositoryImpl(WatchlistDao watchlistDao, BlacklistDao blacklistDao, 
                               WhitelistDao whitelistDao) {
        this.watchlistDao = watchlistDao;
        this.blacklistDao = blacklistDao;
        this.whitelistDao = whitelistDao;
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Watchlist operations
    @Override
    public LiveData<List<WatchlistItemEntity>> getWatchlist() {
        return watchlistDao.getAll();
    }
    
    @Override
    public void addToWatchlist(SignalDevice device) {
        executor.execute(() -> {
            WatchlistItemEntity entity = EntityMappers.toWatchlistEntity(device);
            watchlistDao.insert(entity);
        });
    }
    
    @Override
    public void removeFromWatchlist(String macAddress) {
        executor.execute(() -> watchlistDao.delete(macAddress));
    }
    
    @Override
    public boolean isInWatchlist(String macAddress) {
        return watchlistDao.exists(macAddress);
    }
    
    // Blacklist operations
    @Override
    public LiveData<List<BlacklistItemEntity>> getBlacklist() {
        return blacklistDao.getAll();
    }
    
    @Override
    public void addToBlacklist(SignalDevice device, String reason) {
        executor.execute(() -> {
            BlacklistItemEntity entity = EntityMappers.toBlacklistEntity(device, reason);
            blacklistDao.insert(entity);
        });
    }
    
    @Override
    public void removeFromBlacklist(String macAddress) {
        executor.execute(() -> blacklistDao.delete(macAddress));
    }
    
    @Override
    public boolean isInBlacklist(String macAddress) {
        return blacklistDao.exists(macAddress);
    }
    
    // Whitelist operations
    @Override
    public LiveData<List<WhitelistItemEntity>> getWhitelist() {
        return whitelistDao.getAll();
    }
    
    @Override
    public void addToWhitelist(SignalDevice device) {
        executor.execute(() -> {
            WhitelistItemEntity entity = EntityMappers.toWhitelistEntity(device);
            whitelistDao.insert(entity);
        });
    }
    
    @Override
    public void removeFromWhitelist(String macAddress) {
        executor.execute(() -> whitelistDao.delete(macAddress));
    }
    
    @Override
    public boolean isInWhitelist(String macAddress) {
        return whitelistDao.exists(macAddress);
    }
}
