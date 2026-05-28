package org.zacsn.signal_dectect.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import org.zacsn.signal_dectect.domain.model.ScanType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Manages runtime permissions for signal scanning operations.
 * Handles different permission requirements across Android versions.
 */
@Singleton
public class PermissionManager {
    
    private final Context context;
    
    @Inject
    public PermissionManager(@ApplicationContext Context context) {
        this.context = context;
    }

    
    /**
     * Get required permissions for a specific scan type based on Android version.
     * 
     * @param scanType The type of scan to perform
     * @return Array of required permission strings
     */
    public String[] getRequiredPermissions(ScanType scanType) {
        Set<String> permissions = new HashSet<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            if (scanType.isBluetooth()) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            if (scanType.isWifi()) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
                permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            }
            if (scanType.isCellular()) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
        } else {
            // Android 8-11 (API 26-30)
            if (scanType.isBluetooth()) {
                permissions.add(Manifest.permission.BLUETOOTH);
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (scanType.isWifi()) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                permissions.add(Manifest.permission.CHANGE_WIFI_STATE);
                permissions.add(Manifest.permission.ACCESS_WIFI_STATE);
            }
            if (scanType.isCellular()) {
                permissions.add(Manifest.permission.READ_PHONE_STATE);
            }
        }
        
        return permissions.toArray(new String[0]);
    }

    
    /**
     * Check if all required permissions are granted for a scan type.
     * 
     * @param scanType The type of scan to check permissions for
     * @return true if all required permissions are granted
     */
    public boolean hasRequiredPermissions(ScanType scanType) {
        String[] required = getRequiredPermissions(scanType);
        for (String permission : required) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get list of missing permissions for a scan type.
     * 
     * @param scanType The type of scan to check
     * @return Array of missing permission strings
     */
    public String[] getMissingPermissions(ScanType scanType) {
        List<String> missing = new ArrayList<>();
        String[] required = getRequiredPermissions(scanType);
        
        for (String permission : required) {
            if (ContextCompat.checkSelfPermission(context, permission) 
                    != PackageManager.PERMISSION_GRANTED) {
                missing.add(permission);
            }
        }
        
        return missing.toArray(new String[0]);
    }
}
