package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for MAC address vendor lookup.
 * Loads OUI (Organizationally Unique Identifier) data from CSV file.
 */
public class MacVendorUtils {
    
    private static final String TAG = "MacVendorUtils";
    private static Map<String, String> vendorMap = null;
    private static boolean isInitialized = false;
    
    /**
     * Initialize the vendor database from CSV file.
     * This should be called once during application startup.
     * 
     * @param context Application context
     */
    public static synchronized void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "Already initialized with " + (vendorMap != null ? vendorMap.size() : 0) + " vendors");
            return;
        }
        
        vendorMap = new HashMap<>();
        
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("oui.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            
            String line;
            int lineCount = 0;
            int successCount = 0;
            
            while ((line = reader.readLine()) != null) {
                lineCount++;
                
                // Parse CSV line: MAC-PREFIX,"Vendor Name"
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // Split by comma, but handle quoted vendor names
                int commaIndex = line.indexOf(',');
                if (commaIndex == -1) {
                    continue;
                }
                
                String macPrefix = line.substring(0, commaIndex).trim();
                String vendorName = line.substring(commaIndex + 1).trim();
                
                // Remove quotes from vendor name
                if (vendorName.startsWith("\"") && vendorName.endsWith("\"")) {
                    vendorName = vendorName.substring(1, vendorName.length() - 1);
                }
                
                // Convert MAC prefix from XX-XX-XX format to XX:XX:XX format
                macPrefix = macPrefix.replace('-', ':').toUpperCase();
                
                vendorMap.put(macPrefix, vendorName);
                successCount++;
            }
            
            reader.close();
            inputStream.close();
            
            isInitialized = true;
            Log.i(TAG, "Successfully loaded " + successCount + " vendors from " + lineCount + " lines");
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to load vendor database", e);
            e.printStackTrace();
            // If loading fails, keep empty map
            isInitialized = true;
        }
    }
    
    /**
     * Get vendor name from MAC address.
     * 
     * @param macAddress MAC address in format "XX:XX:XX:XX:XX:XX"
     * @return Vendor name or "Unknown" if not found
     */
    public static String getVendor(String macAddress) {
        if (!isInitialized || vendorMap == null) {
            Log.w(TAG, "getVendor called but not initialized");
            return "未知";
        }
        
        if (macAddress == null || macAddress.length() < 8) {
            Log.w(TAG, "Invalid MAC address: " + macAddress);
            return "未知";
        }
        
        // Check if this is a randomized MAC address
        if (isRandomizedMacAddress(macAddress)) {
            Log.d(TAG, "Randomized MAC address detected: " + macAddress);
            return "随机地址";
        }
        
        // Extract first 8 characters (XX:XX:XX) and convert to uppercase
        String prefix = macAddress.substring(0, 8).toUpperCase();
        
        // Look up vendor in map
        String vendor = vendorMap.get(prefix);
        
        if (vendor == null) {
            Log.d(TAG, "Vendor not found for MAC prefix: " + prefix + " (from " + macAddress + ")");
            return "未知厂商";
        } else {
            Log.d(TAG, "Found vendor: " + vendor + " for MAC prefix: " + prefix);
            return vendor;
        }
    }
    
    /**
     * Check if a MAC address is randomized (locally administered).
     * A MAC address is randomized if the second bit of the first byte is set to 1.
     * 
     * @param macAddress MAC address in format "XX:XX:XX:XX:XX:XX"
     * @return true if randomized, false otherwise
     */
    private static boolean isRandomizedMacAddress(String macAddress) {
        if (macAddress == null || macAddress.length() < 2) {
            return false;
        }
        
        try {
            // Get the first byte (first two hex characters)
            String firstByte = macAddress.substring(0, 2);
            int byteValue = Integer.parseInt(firstByte, 16);
            
            // Check if the second bit (locally administered bit) is set
            // Bit mask: 0x02 = 0000 0010
            return (byteValue & 0x02) != 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Check if the vendor database is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Get the number of vendors in the database.
     * 
     * @return Number of vendor entries
     */
    public static int getVendorCount() {
        return vendorMap != null ? vendorMap.size() : 0;
    }
}
