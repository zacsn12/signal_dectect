package org.zacsn.signal_dectect.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for scan records
 */
@Dao
public interface ScanRecordDao {
    
    /**
     * Get all scan records ordered by timestamp descending
     * @return LiveData list of all scan records
     */
    @Query("SELECT * FROM scan_records ORDER BY timestamp DESC")
    LiveData<List<ScanRecordEntity>> getAllRecords();
    
    /**
     * Get a specific scan record by ID
     * @param recordId the record ID
     * @return the scan record entity or null
     */
    @Query("SELECT * FROM scan_records WHERE id = :recordId")
    ScanRecordEntity getRecordById(long recordId);
    
    /**
     * Insert a new scan record
     * @param record the scan record to insert
     * @return the row ID of the inserted record
     */
    @Insert
    long insertRecord(ScanRecordEntity record);
    
    /**
     * Delete a scan record
     * @param record the scan record to delete
     */
    @Delete
    void deleteRecord(ScanRecordEntity record);
    
    /**
     * Delete a scan record by ID
     * @param recordId the record ID to delete
     */
    @Query("DELETE FROM scan_records WHERE id = :recordId")
    void deleteById(long recordId);
    
    /**
     * Delete all scan records
     */
    @Query("DELETE FROM scan_records")
    void deleteAll();
    
    /**
     * Update record name
     * @param recordId the record ID
     * @param name the new name
     */
    @Query("UPDATE scan_records SET name = :name WHERE id = :recordId")
    void updateRecordName(long recordId, String name);
    
    /**
     * Delete multiple records by IDs
     * @param recordIds list of record IDs to delete
     */
    @Query("DELETE FROM scan_records WHERE id IN (:recordIds)")
    void deleteByIds(java.util.List<Long> recordIds);
}
