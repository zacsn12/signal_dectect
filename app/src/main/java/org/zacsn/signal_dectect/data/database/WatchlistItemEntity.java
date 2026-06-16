package org.zacsn.signal_dectect.data.database;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

/**
 * Room entity for storing watchlist items
 */
@Entity(tableName = "watchlist")
public class WatchlistItemEntity {
    
    @PrimaryKey
    @NonNull
    private String macAddress;
    
    private String deviceName;
    
    private String deviceType; // "bluetooth", "wifi", "cellular"
    
    private String manufacturer;
    
    private long addedAt;

    private String matchType;

    private String matchValue;

    private String displayName;
    
    // Constructor
    @Ignore
    public WatchlistItemEntity(@NonNull String macAddress, String deviceName, 
                              String deviceType, String manufacturer, long addedAt) {
        this(macAddress, deviceName, deviceType, manufacturer, addedAt, null, null, null);
    }

    public WatchlistItemEntity(@NonNull String macAddress, String deviceName,
                              String deviceType, String manufacturer, long addedAt,
                              String matchType, String matchValue, String displayName) {
        this.macAddress = macAddress;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.manufacturer = manufacturer;
        this.addedAt = addedAt;
        this.matchType = matchType;
        this.matchValue = matchValue;
        this.displayName = displayName;
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

    public String getMatchType() {
        return matchType;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchValue() {
        return matchValue;
    }

    public void setMatchValue(String matchValue) {
        this.matchValue = matchValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
