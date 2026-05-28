package org.zacsn.signal_dectect.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for storing scan records
 */
@Entity(tableName = "scan_records")
public class ScanRecordEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    private String name; // Record name (optional, user-defined)
    
    private long timestamp;
    
    private int scanType; // Bitmask: 1=BT, 2=WiFi, 4=Cellular
    
    private long duration; // milliseconds
    
    private Double latitude;
    
    private Double longitude;
    
    private int deviceCount;
    
    private String devicesJson; // JSON serialized List<SignalDevice>
    
    // Constructor
    public ScanRecordEntity(long timestamp, int scanType, long duration, Double latitude, 
                           Double longitude, int deviceCount, String devicesJson) {
        this.timestamp = timestamp;
        this.scanType = scanType;
        this.duration = duration;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceCount = deviceCount;
        this.devicesJson = devicesJson;
        this.name = null; // Default to null, will be auto-generated if needed
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getScanType() {
        return scanType;
    }
    
    public void setScanType(int scanType) {
        this.scanType = scanType;
    }
    
    public long getDuration() {
        return duration;
    }
    
    public void setDuration(long duration) {
        this.duration = duration;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public int getDeviceCount() {
        return deviceCount;
    }
    
    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }
    
    public String getDevicesJson() {
        return devicesJson;
    }
    
    public void setDevicesJson(String devicesJson) {
        this.devicesJson = devicesJson;
    }
}
