package org.zacsn.signal_dectect.domain.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.zacsn.signal_dectect.data.database.*;
import org.zacsn.signal_dectect.util.GsonFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for converting between database entities and domain models.
 */
public class EntityMappers {
    
    private static final Gson gson = GsonFactory.getGson();
    
    // ScanRecordEntity to ScanRecord conversion
    public static ScanRecord toDomain(ScanRecordEntity entity) {
        if (entity == null) return null;
        
        ScanType scanType = ScanType.fromInt(entity.getScanType());
        
        List<SignalDevice> devices = parseDevicesJson(entity.getDevicesJson(), entity.getTimestamp());
        
        return new ScanRecord(
            entity.getId(),
            entity.getTimestamp(),
            scanType,
            entity.getDuration(),
            entity.getLatitude(),
            entity.getLongitude(),
            Math.max(entity.getDeviceCount(), devices.size()),
            devices
        );
    }

    public static List<SignalDevice> parseDevicesJson(String devicesJson, long fallbackTimestamp) {
        List<SignalDevice> devices = new ArrayList<>();
        if (devicesJson == null || devicesJson.trim().isEmpty()) {
            return devices;
        }

        String json = devicesJson.trim();
        try {
            Type listType = new TypeToken<List<SignalDevice>>() {}.getType();
            devices = gson.fromJson(json, listType);
        } catch (Throwable ignored) {
        }

        if (devices == null || devices.isEmpty()) {
            try {
                com.google.gson.JsonElement element = new com.google.gson.JsonParser().parse(json);
                if (element != null && element.isJsonArray()) {
                    com.google.gson.JsonArray array = element.getAsJsonArray();
                    devices = new ArrayList<>();
                    for (com.google.gson.JsonElement item : array) {
                        if (item.isJsonObject()) {
                            try {
                                SignalDevice d = gson.fromJson(item, SignalDevice.class);
                                if (d != null) devices.add(d);
                            } catch (Throwable ignored) {}
                        } else if (item.isJsonPrimitive()) {
                            String str = item.getAsString();
                            if (str != null && !str.trim().isEmpty()) {
                                devices.add(new SignalDevice(
                                        str.trim(),
                                        str.trim(),
                                        DeviceType.BLUETOOTH_CLASSIC,
                                        "未知厂商",
                                        0,
                                        null,
                                        0.0,
                                        fallbackTimestamp > 0 ? fallbackTimestamp : System.currentTimeMillis(),
                                        fallbackTimestamp > 0 ? fallbackTimestamp : System.currentTimeMillis(),
                                        false,
                                        false,
                                        false
                                ));
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                android.util.Log.e("EntityMappers", "Failed to parse devices JSON", e);
            }
        }

        return devices != null ? devices : new ArrayList<>();
    }

    
    // ScanRecord to ScanRecordEntity conversion
    public static ScanRecordEntity toEntity(ScanRecord record) {
        if (record == null) return null;
        
        String devicesJson = "";
        if (record.getDevices() != null && !record.getDevices().isEmpty()) {
            try {
                devicesJson = gson.toJson(record.getDevices());
            } catch (Throwable e) {
                android.util.Log.e("EntityMappers", "Failed to serialize devices to JSON", e);
                devicesJson = "";
            }
        }
        
        int scanTypeInt = record.getScanType() != null ? record.getScanType().toInt() : 0;
        ScanRecordEntity entity = new ScanRecordEntity(
            record.getTimestamp(),
            scanTypeInt,
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
            System.currentTimeMillis(),
            "MAC",
            device.getMacAddress(),
            device.getDeviceName()
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
