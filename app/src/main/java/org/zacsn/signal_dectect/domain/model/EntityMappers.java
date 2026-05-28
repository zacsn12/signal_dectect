package org.zacsn.signal_dectect.domain.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.zacsn.signal_dectect.data.database.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting between database entities and domain models.
 */
public class EntityMappers {
    
    private static final Gson gson = new Gson();
    
    // ScanRecordEntity to ScanRecord conversion
    public static ScanRecord toDomain(ScanRecordEntity entity) {
        if (entity == null) return null;
        
        ScanType scanType = ScanType.fromInt(entity.getScanType());
        
        List<SignalDevice> devices = new ArrayList<>();
        if (entity.getDevicesJson() != null && !entity.getDevicesJson().isEmpty()) {
            Type listType = new TypeToken<List<SignalDevice>>() {}.getType();
            devices = gson.fromJson(entity.getDevicesJson(), listType);
            if (devices == null) devices = new ArrayList<>();
        }
        
        return new ScanRecord(
            entity.getId(),
            entity.getTimestamp(),
            scanType,
            entity.getDuration(),
            entity.getLatitude(),
            entity.getLongitude(),
            entity.getDeviceCount(),
            devices
        );
    }

    
    // ScanRecord to ScanRecordEntity conversion
    public static ScanRecordEntity toEntity(ScanRecord record) {
        if (record == null) return null;
        
        String devicesJson = "";
        if (record.getDevices() != null && !record.getDevices().isEmpty()) {
            devicesJson = gson.toJson(record.getDevices());
        }
        
        ScanRecordEntity entity = new ScanRecordEntity(
            record.getTimestamp(),
            record.getScanType().toInt(),
            record.getDuration(),
            record.getLatitude(),
            record.getLongitude(),
            record.getDeviceCount(),
            devicesJson
        );
        entity.setId(record.getId());
        return entity;
    }

    
    // WatchlistItemEntity to SignalDevice conversion
    public static SignalDevice watchlistToDomain(WatchlistItemEntity entity) {
        if (entity == null) return null;
        
        DeviceType deviceType = parseDeviceType(entity.getDeviceType());
        
        return new SignalDevice(
            entity.getMacAddress(),
            entity.getDeviceName(),
            deviceType,
            entity.getManufacturer(),
            0, // signal strength not stored in watchlist
            null, // frequency not stored
            0.0, // distance not stored
            entity.getAddedAt(),
            entity.getAddedAt(),
            false,
            false,
            false
        );
    }
    
    // SignalDevice to WatchlistItemEntity conversion
    public static WatchlistItemEntity toWatchlistEntity(SignalDevice device) {
        if (device == null) return null;
        
        return new WatchlistItemEntity(
            device.getMacAddress(),
            device.getDeviceName(),
            device.getDeviceType().name().toLowerCase(),
            device.getManufacturer(),
            System.currentTimeMillis()
        );
    }

    
    // SignalDevice to BlacklistItemEntity conversion
    public static BlacklistItemEntity toBlacklistEntity(SignalDevice device, String reason) {
        if (device == null) return null;
        
        return new BlacklistItemEntity(
            device.getMacAddress(),
            device.getDeviceName(),
            device.getDeviceType().name().toLowerCase(),
            device.getManufacturer(),
            System.currentTimeMillis(),
            reason
        );
    }
    
    // SignalDevice to WhitelistItemEntity conversion
    public static WhitelistItemEntity toWhitelistEntity(SignalDevice device) {
        if (device == null) return null;
        
        return new WhitelistItemEntity(
            device.getMacAddress(),
            device.getDeviceName(),
            device.getDeviceType().name().toLowerCase(),
            device.getManufacturer(),
            System.currentTimeMillis()
        );
    }

    
    // Helper method to parse device type string
    private static DeviceType parseDeviceType(String typeString) {
        if (typeString == null) return DeviceType.BLUETOOTH_CLASSIC;
        
        switch (typeString.toLowerCase()) {
            case "bluetooth_classic":
            case "bluetooth":
                return DeviceType.BLUETOOTH_CLASSIC;
            case "bluetooth_le":
            case "ble":
                return DeviceType.BLUETOOTH_LE;
            case "wifi":
                return DeviceType.WIFI;
            case "cellular":
                return DeviceType.CELLULAR;
            default:
                return DeviceType.BLUETOOTH_CLASSIC;
        }
    }
    
    // Batch conversion methods
    public static List<ScanRecord> toDomainList(List<ScanRecordEntity> entities) {
        List<ScanRecord> records = new ArrayList<>();
        if (entities != null) {
            for (ScanRecordEntity entity : entities) {
                records.add(toDomain(entity));
            }
        }
        return records;
    }
}
