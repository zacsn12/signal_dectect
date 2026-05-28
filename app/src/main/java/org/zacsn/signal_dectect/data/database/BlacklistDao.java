package org.zacsn.signal_dectect.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for blacklist items
 */
@Dao
public interface BlacklistDao {
    
    /**
     * Get all blacklist items ordered by added date descending
     * @return LiveData list of all blacklist items
     */
    @Query("SELECT * FROM blacklist ORDER BY addedAt DESC")
    LiveData<List<BlacklistItemEntity>> getAll();
    
    /**
     * Insert a blacklist item, replacing if it already exists
     * @param item the blacklist item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlacklistItemEntity item);
    
    /**
     * Delete a blacklist item by MAC address
     * @param macAddress the MAC address of the item to delete
     */
    @Query("DELETE FROM blacklist WHERE macAddress = :macAddress")
    void delete(String macAddress);
    
    /**
     * Check if a device exists in the blacklist
     * @param macAddress the MAC address to check
     * @return true if the device exists in the blacklist
     */
    @Query("SELECT EXISTS(SELECT 1 FROM blacklist WHERE macAddress = :macAddress)")
    boolean exists(String macAddress);
}
