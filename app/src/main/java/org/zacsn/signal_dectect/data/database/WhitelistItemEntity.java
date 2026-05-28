package org.zacsn.signal_dectect.data.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room entity for storing whitelist items
 */
@Entity(tableName = "whitelist")
public class WhitelistItemEntity {
    
    @PrimaryKey
    @NonNull
    private String macAddress;
    
    private String deviceName;
    
    private String deviceType; // "bluetooth", "wifi", "cellular"
    
    private String manufacturer;
    
    private long addedAt;
    
    // Constructor
    public WhitelistItemEntity(@NonNull String macAddress, String deviceName, 
                              String deviceType, String manufacturer, long addedAt) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.manufacturer = manufacturer;
        this.addedAt = addedAt;
    }
    
    // Getters and Setters
    @NonNull
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(@NonNull String macAddress) {
        this.macAddress = macAddress;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getDeviceType() {
        return deviceType;
    }
    
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    public String getManufacturer() {
        return manufacturer;
    }
    
    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }
    
    public long getAddedAt() {
        return addedAt;
    }
    
    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }
}
