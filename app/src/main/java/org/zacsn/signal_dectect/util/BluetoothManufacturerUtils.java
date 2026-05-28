package org.zacsn.signal_dectect.util;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

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
    }
    
    /**
     * Get manufacturer name from Bluetooth Company Identifier.
     * 
     * @param companyId Bluetooth Company Identifier (16-bit)
     * @return Manufacturer name or null if not found
     */
    public static String getManufacturer(int companyId) {
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
