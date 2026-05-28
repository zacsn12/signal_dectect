package org.zacsn.signal_dectect.data.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Room database for Signal Detection application
 * 
 * This database stores:
 * - Scan records: Historical signal inspection sessions
 * - Watchlist: Devices marked for monitoring
 * - Blacklist: Suspicious or unwanted devices
 * - Whitelist: Known safe devices
 */
@Database(
    entities = {
        ScanRecordEntity.class,
        WatchlistItemEntity.class,
        BlacklistItemEntity.class,
        WhitelistItemEntity.class
    },
    version = 2,
    exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    /**
     * Database name constant
     */
    public static final String DATABASE_NAME = "signal_detect_db";
    
    /**
     * Get the DAO for scan record operations
     * @return ScanRecordDao instance
     */
    public abstract ScanRecordDao scanRecordDao();
    
    /**
     * Get the DAO for watchlist operations
     * @return WatchlistDao instance
     */
    public abstract WatchlistDao watchlistDao();
    
    /**
     * Get the DAO for blacklist operations
     * @return BlacklistDao instance
     */
    public abstract BlacklistDao blacklistDao();
    
    /**
     * Get the DAO for whitelist operations
     * @return WhitelistDao instance
     */
    public abstract WhitelistDao whitelistDao();
}
