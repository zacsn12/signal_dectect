package org.zacsn.signal_dectect.domain.model;

import java.util.List;

/**
 * Domain model for scan record.
 * This is the business logic representation, separate from database entity.
 */
public class ScanRecord {
    
    private long id;
    private long timestamp;
    private ScanType scanType;
    private long duration; // milliseconds
    private Double latitude;
    private Double longitude;
    private int deviceCount;
    private List<SignalDevice> devices;
    
    // Constructor
    public ScanRecord(long id, long timestamp, ScanType scanType, long duration,
                     Double latitude, Double longitude, int deviceCount, 
                     List<SignalDevice> devices) {
        this.id = id;
        this.timestamp = timestamp;
        this.scanType = scanType;
        this.duration = duration;
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceCount = deviceCount;
        this.devices = devices;
    }
    
    // Getters and Setters
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public ScanType getScanType() {
        return scanType;
    }

    
    public void setScanType(ScanType scanType) {
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
    
    public List<SignalDevice> getDevices() {
        return devices;
    }
    
    public void setDevices(List<SignalDevice> devices) {
        this.devices = devices;
    }
}
