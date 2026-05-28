package org.zacsn.signal_dectect.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for whitelist items
 */
@Dao
public interface WhitelistDao {
    
    /**
     * Get all whitelist items ordered by added date descending
     * @return LiveData list of all whitelist items
     */
    @Query("SELECT * FROM whitelist ORDER BY addedAt DESC")
    LiveData<List<WhitelistItemEntity>> getAll();
    
    /**
     * Insert a whitelist item, replacing if it already exists
     * @param item the whitelist item to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WhitelistItemEntity item);
    
    /**
     * Delete a whitelist item by MAC address
     * @param macAddress the MAC address of the item to delete
     */
    @Query("DELETE FROM whitelist WHERE macAddress = :macAddress")
    void delete(String macAddress);
    
    /**
     * Check if a device exists in the whitelist
     * @param macAddress the MAC address to check
     * @return true if the device exists in the whitelist
     */
    @Query("SELECT EXISTS(SELECT 1 FROM whitelist WHERE macAddress = :macAddress)")
    boolean exists(String macAddress);
}
