package org.zacsn.signal_dectect.util;

import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for Bluetooth manufacturer identification.
 * Maps Bluetooth Company Identifiers to manufacturer names.
 * 
 * Company IDs are assigned by Bluetooth SIG.
 * Reference: https://www.bluetooth.com/specifications/assigned-numbers/
 */
public class BluetoothManufacturerUtils {
    
    private static final String TAG = "BtManufacturerUtils";
    private static final Map<Integer, String> manufacturerMap = new HashMap<>();
    private static final Map<Integer, String> usbVendorMap = new HashMap<>();
    private static final Set<Integer> ambiguousCompanyIds = new HashSet<>();
    
    static {
        // Major smartphone manufacturers
        manufacturerMap.put(0x004C, "Apple, Inc.");
        manufacturerMap.put(0x0075, "Samsung Electronics Co. Ltd.");
        manufacturerMap.put(0x0157, "Xiaomi Inc.");
        manufacturerMap.put(0x0272, "Xiaomi Inc.");
        manufacturerMap.put(0x038F, "Xiaomi Inc.");
        manufacturerMap.put(0x0589, "Huawei Technologies Co., Ltd.");
        manufacturerMap.put(0x027D, "Huawei Technologies Co., Ltd.");
        manufacturerMap.put(0x0471, "OPPO Mobile Telecommunications Corp., Ltd.");
        manufacturerMap.put(0x04E1, "vivo Mobile Communication Co., Ltd.");
        manufacturerMap.put(0x0459, "OnePlus Electronics (Shenzhen) Co., Ltd.");
        
        // Computer manufacturers
        manufacturerMap.put(0x0006, "Microsoft");
        manufacturerMap.put(0x000F, "Intel Corp.");
        manufacturerMap.put(0x000A, "Qualcomm");
        manufacturerMap.put(0x00E0, "Google");
        manufacturerMap.put(0x0087, "Garmin International, Inc.");
        manufacturerMap.put(0x0131, "Lenovo (Singapore) Pte Ltd.");
        manufacturerMap.put(0x0259, "HP Inc.");
        manufacturerMap.put(0x0419, "ASUS Global Pte Ltd");
        
        // Audio device manufacturers
        manufacturerMap.put(0x009E, "Sony Corporation");
        manufacturerMap.put(0x0109, "Bose Corporation");
        manufacturerMap.put(0x00A7, "Harman International Industries, Inc.");
        manufacturerMap.put(0x0141, "Sennheiser electronic GmbH & Co. KG");
        manufacturerMap.put(0x0357, "JBL");
        
        // Wearable manufacturers
        manufacturerMap.put(0x0090, "Fitbit, Inc.");
        manufacturerMap.put(0x0171, "Amazfit");
        manufacturerMap.put(0x0275, "Huami Information Technology Co., Ltd.");
        
        // IoT and Smart Home manufacturers
        manufacturerMap.put(0x0099, "Texas Instruments Inc.");
        manufacturerMap.put(0x0059, "Nordic Semiconductor ASA");
        manufacturerMap.put(0x0171, "Espressif Inc.");
        manufacturerMap.put(0x02E5, "Espressif Inc.");
        manufacturerMap.put(0x0499, "Ruuvi Innovations Ltd.");
        manufacturerMap.put(0x0349, "Shenzhen Jingxun Software Telecommunication Technology Co., Ltd");
        
        // Automotive manufacturers
        manufacturerMap.put(0x0143, "Tesla Motors");
        manufacturerMap.put(0x0189, "Volkswagen AG");
        manufacturerMap.put(0x0164, "BMW AG");
        manufacturerMap.put(0x0118, "Mercedes-Benz");
        manufacturerMap.put(0x0183, "Toyota Motor Corporation");
        
        // Gaming console manufacturers
        manufacturerMap.put(0x0009, "Nintendo Co., Ltd.");
        manufacturerMap.put(0x0054, "Sony Interactive Entertainment Inc.");
        
        // Network equipment manufacturers
        manufacturerMap.put(0x0159, "Cisco Systems, Inc");
        manufacturerMap.put(0x0286, "TP-Link Corporation Limited");
        manufacturerMap.put(0x0489, "Xiaomi Communications Co Ltd");
        
        // Other notable manufacturers
        manufacturerMap.put(0x0057, "LG Electronics");
        manufacturerMap.put(0x0117, "Motorola Solutions");
        manufacturerMap.put(0x0168, "HTC Corporation");
        manufacturerMap.put(0x0224, "Amazon.com Services LLC");
        manufacturerMap.put(0x0499, "Tile, Inc.");
        manufacturerMap.put(0x0050, "Plantronics, Inc.");
        manufacturerMap.put(0x0077, "Logitech International SA");
        manufacturerMap.put(0x0195, "GoPro, Inc.");
        manufacturerMap.put(0x0247, "DJI");
        manufacturerMap.put(0x0343, "Anker Innovations Limited");
        
        // Chinese manufacturers
        manufacturerMap.put(0x0157, "Xiaomi Inc.");
        manufacturerMap.put(0x0349, "Shenzhen Jingxun Software");
        manufacturerMap.put(0x0471, "OPPO Mobile");
        manufacturerMap.put(0x04E1, "vivo Mobile");
        manufacturerMap.put(0x0459, "OnePlus Electronics");
        manufacturerMap.put(0x0589, "Huawei Technologies");
        manufacturerMap.put(0x05AC, "Realme Chongqing Mobile");
        manufacturerMap.put(0x0275, "Huami (Amazfit)");
        manufacturerMap.put(0x0286, "TP-Link");
        manufacturerMap.put(0x02E5, "Espressif (ESP32)");

        // Duplicated or historically overloaded entries in the local table are
        // treated as unknown to avoid turning a weak BLE hint into a false vendor.
        ambiguousCompanyIds.add(0x0171);
        ambiguousCompanyIds.add(0x0499);

        usbVendorMap.put(0x05AC, "Apple, Inc.");
        usbVendorMap.put(0x04E8, "Samsung Electronics Co. Ltd.");
        usbVendorMap.put(0x2717, "Xiaomi Inc.");
        usbVendorMap.put(0x12D1, "Huawei Technologies Co., Ltd.");
        usbVendorMap.put(0x18D1, "Google");
        usbVendorMap.put(0x045E, "Microsoft");
        usbVendorMap.put(0x8086, "Intel Corp.");
        usbVendorMap.put(0x8087, "Intel Corp.");
        usbVendorMap.put(0x05C6, "Qualcomm");
        usbVendorMap.put(0x054C, "Sony Corporation");
        usbVendorMap.put(0x17EF, "Lenovo (Singapore) Pte Ltd.");
        usbVendorMap.put(0x03F0, "HP Inc.");
        usbVendorMap.put(0x0B05, "ASUS Global Pte Ltd");
        usbVendorMap.put(0x22D9, "OPPO Mobile Telecommunications Corp., Ltd.");
        usbVendorMap.put(0x2D95, "vivo Mobile Communication Co., Ltd.");
        usbVendorMap.put(0x2A70, "OnePlus Electronics (Shenzhen) Co., Ltd.");
    }
    
    /**
     * Get manufacturer name from Bluetooth Company Identifier.
     * 
     * @param companyId Bluetooth Company Identifier (16-bit)
     * @return Manufacturer name or null if not found
     */
    public static String getManufacturer(int companyId) {
        if (ambiguousCompanyIds.contains(companyId)) {
            Log.d(TAG, "Ambiguous company ID ignored: " + getCompanyIdHex(companyId));
            return null;
        }

        String manufacturer = manufacturerMap.get(companyId);
        if (manufacturer != null) {
            Log.d(TAG, "Found manufacturer: " + manufacturer + " for company ID: 0x" 
                    + Integer.toHexString(companyId).toUpperCase());
        } else {
            Log.d(TAG, "Manufacturer not found for company ID: 0x" 
                    + Integer.toHexString(companyId).toUpperCase());
        }
        return manufacturer;
    }

    public static String getPnpManufacturer(int vendorIdSource, int vendorId) {
        if (vendorIdSource == 1) {
            return getManufacturer(vendorId);
        }
        if (vendorIdSource == 2) {
            return usbVendorMap.get(vendorId);
        }
        return null;
    }

    public static String getPnpVendorSourceLabel(int vendorIdSource) {
        switch (vendorIdSource) {
            case 1:
                return "Bluetooth SIG";
            case 2:
                return "USB-IF";
            default:
                return "未知VID来源";
        }
    }

    public static String getCompanyIdHex(int companyId) {
        return "0x" + String.format("%04X", companyId & 0xFFFF);
    }

    /**
     * Returns a manufacturer only when the advertisement data is reliable enough for
     * device alerting. Some BLE formats, especially iBeacon, use Apple's company ID
     * even when the beacon hardware is made by a third party.
     */
    public static String getReliableManufacturer(int companyId, byte[] manufacturerData) {
        if (companyId == 0x004C && isAppleIBeaconPayload(manufacturerData)) {
            Log.d(TAG, "Ignoring Apple company ID from iBeacon payload to avoid false device alerts");
            return null;
        }

        return getManufacturer(companyId);
    }

    public static String inferManufacturerFromName(String deviceName) {
        if (deviceName == null) {
            return null;
        }

        String normalized = deviceName.toLowerCase();
        if (normalized.contains("airpods") || normalized.contains("beats") || normalized.contains("apple")) {
            return "Apple, Inc.";
        }
        if (normalized.contains("galaxy") || normalized.contains("samsung")) {
            return "Samsung Electronics Co. Ltd.";
        }
        if (normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("mi band")) {
            return "Xiaomi Inc.";
        }
        if (normalized.contains("huawei") || normalized.contains("honor")) {
            return "Huawei Technologies Co., Ltd.";
        }
        if (normalized.contains("oppo")) {
            return "OPPO Mobile Telecommunications Corp., Ltd.";
        }
        if (normalized.contains("vivo")) {
            return "vivo Mobile Communication Co., Ltd.";
        }
        if (normalized.contains("oneplus")) {
            return "OnePlus Electronics (Shenzhen) Co., Ltd.";
        }
        if (normalized.contains("pixel") || normalized.contains("google")) {
            return "Google";
        }
        return null;
    }

    public static boolean isSameManufacturer(String first, String second) {
        String firstBrand = normalizeBrand(first);
        String secondBrand = normalizeBrand(second);
        return firstBrand != null && firstBrand.equals(secondBrand);
    }

    public static String normalizeBrand(String manufacturer) {
        if (manufacturer == null) {
            return null;
        }

        String normalized = manufacturer.toLowerCase()
                .replace(",", " ")
                .replace(".", " ")
                .replace("-", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replace("&", " ")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }

        if (normalized.contains("apple") || normalized.contains("beats")) {
            return "apple";
        }
        if (normalized.contains("samsung") || normalized.contains("galaxy")) {
            return "samsung";
        }
        if (normalized.contains("xiaomi") || normalized.contains("redmi") || normalized.contains("huami")
                || normalized.contains("amazfit")) {
            return "xiaomi";
        }
        if (normalized.contains("huawei") || normalized.contains("honor")) {
            return "huawei";
        }
        if (normalized.contains("oppo")) {
            return "oppo";
        }
        if (normalized.contains("vivo")) {
            return "vivo";
        }
        if (normalized.contains("oneplus")) {
            return "oneplus";
        }
        if (normalized.contains("google") || normalized.contains("pixel")) {
            return "google";
        }
        if (normalized.contains("microsoft")) {
            return "microsoft";
        }
        if (normalized.contains("intel")) {
            return "intel";
        }
        if (normalized.contains("qualcomm")) {
            return "qualcomm";
        }
        if (normalized.contains("sony")) {
            return "sony";
        }
        if (normalized.contains("lenovo")) {
            return "lenovo";
        }
        if (normalized.contains("hp ")) {
            return "hp";
        }
        if (normalized.contains("asus")) {
            return "asus";
        }
        return normalized;
    }

    private static boolean isAppleIBeaconPayload(byte[] manufacturerData) {
        return manufacturerData != null
                && manufacturerData.length >= 23
                && (manufacturerData[0] & 0xFF) == 0x02
                && (manufacturerData[1] & 0xFF) == 0x15;
    }
    
    /**
     * Parse manufacturer data from byte array.
     * The first 2 bytes contain the company ID in little-endian format.
     * 
     * @param manufacturerData Raw manufacturer data bytes
     * @return Manufacturer name or null if cannot parse
     */
    public static String parseManufacturerData(byte[] manufacturerData) {
        if (manufacturerData == null || manufacturerData.length < 2) {
            return null;
        }
        
        // Company ID is in little-endian format (first 2 bytes)
        int companyId = (manufacturerData[1] & 0xFF) << 8 | (manufacturerData[0] & 0xFF);
        
        return getManufacturer(companyId);
    }
    
    /**
     * Get the number of known manufacturers.
     * 
     * @return Number of manufacturer entries
     */
    public static int getManufacturerCount() {
        return manufacturerMap.size();
    }
}
