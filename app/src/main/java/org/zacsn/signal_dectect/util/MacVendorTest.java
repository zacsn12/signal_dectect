package org.zacsn.signal_dectect.util;

import android.content.Context;
import android.util.Log;

/**
 * Simple test class to verify MAC vendor lookup and Bluetooth manufacturer identification.
 * Call testVendorLookup() after MacVendorUtils.initialize() to verify it's working.
 */
public class MacVendorTest {
    
    private static final String TAG = "MacVendorTest";
    
    public static void testVendorLookup(Context context) {
        Log.i(TAG, "=== Starting MAC Vendor Lookup Test ===");
        
        // Ensure initialized
        if (!MacVendorUtils.isInitialized()) {
            Log.w(TAG, "MacVendorUtils not initialized, initializing now...");
            MacVendorUtils.initialize(context);
        }
        
        Log.i(TAG, "Vendor database size: " + MacVendorUtils.getVendorCount());
        
        // Test some known MAC addresses from the CSV file
        String[] testMacs = {
            "28:6F:B9:AA:BB:CC",  // Nokia Shanghai Bell Co., Ltd.
            "08:EA:44:11:22:33",  // Extreme Networks Headquarters
            "F4:EA:B5:44:55:66",  // Extreme Networks Headquarters
            "E0:06:30:77:88:99",  // HUAWEI TECHNOLOGIES CO.,LTD
            "CC:EB:5E:AA:BB:CC",  // Xiaomi Communications Co Ltd
            "F0:EE:7A:11:22:33",  // Apple, Inc.
            "64:0D:E6:11:22:33",  // Petra Systems
            "47:70:6A:BF:4E:74",  // Randomized MAC (should show "随机地址")
            "7F:14:1E:52:78:00",  // Randomized MAC (should show "随机地址")
            "11:A9:3D:00:F7:EB",  // Randomized MAC (should show "随机地址")
            "00:00:00:00:00:00"   // Should return Unknown
        };
        
        for (String mac : testMacs) {
            String vendor = MacVendorUtils.getVendor(mac);
            Log.i(TAG, "MAC: " + mac + " -> Vendor: " + vendor);
        }
        
        Log.i(TAG, "=== MAC Vendor Lookup Test Complete ===");
        
        // Test Bluetooth manufacturer identification
        Log.i(TAG, "=== Starting Bluetooth Manufacturer Test ===");
        Log.i(TAG, "Bluetooth manufacturer database size: " + BluetoothManufacturerUtils.getManufacturerCount());
        
        int[] testCompanyIds = {
            0x004C,  // Apple
            0x0075,  // Samsung
            0x0157,  // Xiaomi
            0x0589,  // Huawei
            0x0471,  // OPPO
            0x04E1,  // vivo
            0x0459,  // OnePlus
            0x0006,  // Microsoft
            0x00E0,  // Google
            0x0099,  // Texas Instruments
            0x9999   // Unknown
        };
        
        for (int companyId : testCompanyIds) {
            String manufacturer = BluetoothManufacturerUtils.getManufacturer(companyId);
            Log.i(TAG, "Company ID: 0x" + Integer.toHexString(companyId).toUpperCase() 
                    + " -> Manufacturer: " + (manufacturer != null ? manufacturer : "Unknown"));
        }
        
        Log.i(TAG, "=== Bluetooth Manufacturer Test Complete ===");
    }
}
