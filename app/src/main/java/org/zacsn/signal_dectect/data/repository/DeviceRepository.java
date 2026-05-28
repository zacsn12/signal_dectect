package org.zacsn.signal_dectect.data.repository;

import androidx.lifecycle.LiveData;
import org.zacsn.signal_dectect.data.database.BlacklistItemEntity;
import org.zacsn.signal_dectect.data.database.WatchlistItemEntity;
import org.zacsn.signal_dectect.data.database.WhitelistItemEntity;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.List;

/**
 * Repository for device list management (watchlist, blacklist, whitelist).
 */
public interface DeviceRepository {
    
    // Watchlist operations
    LiveData<List<WatchlistItemEntity>> getWatchlist();
    void addToWatchlist(SignalDevice device);
    void removeFromWatchlist(String macAddress);
    boolean isInWatchlist(String macAddress);
    
    // Blacklist operations
    LiveData<List<BlacklistItemEntity>> getBlacklist();
    void addToBlacklist(SignalDevice device, String reason);
    void removeFromBlacklist(String macAddress);
    boolean isInBlacklist(String macAddress);
    
    // Whitelist operations
    LiveData<List<WhitelistItemEntity>> getWhitelist();
    void addToWhitelist(SignalDevice device);
    void removeFromWhitelist(String macAddress);
    boolean isInWhitelist(String macAddress);
}
