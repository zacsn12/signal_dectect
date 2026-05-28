package org.zacsn.signal_dectect.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for watchlist items
 */
@Dao
public interface WatchlistDao {
    
    /**
     * Get all watchlist items ordered by added date descending
     * @return LiveData list of all watchlist items
     */
    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    LiveData<List<WatchlistItemEntity>> getAll();
    
    /**
     * Insert a watchlist item, replacing if it already exists
     * @param item the watchlist item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WatchlistItemEntity item);
    
    /**
     * Delete a watchlist item by MAC address
     * @param macAddress the MAC address of the item to delete
     */
    @Query("DELETE FROM watchlist WHERE macAddress = :macAddress")
    void delete(String macAddress);
    
    /**
     * Check if a device exists in the watchlist
     * @param macAddress the MAC address to check
     * @return true if the device exists in the watchlist
     */
    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE macAddress = :macAddress)")
    boolean exists(String macAddress);
    
    /**
     * Delete all watchlist items
     */
    @Query("DELETE FROM watchlist")
    void deleteAll();
}
