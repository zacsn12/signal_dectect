package org.zacsn.signal_dectect.data.scanner;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.CellSignalStrength;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Controller for cellular network signal monitoring.
 */
@Singleton
public class CellularScanController {
    
    private final Context context;
    private final TelephonyManager telephonyManager;
    
    private PhoneStateListener phoneStateListener;
    private boolean isScanning = false;
    private ScanListener scanListener;
    
    public interface ScanListener {
        void onSignalUpdate(SignalDevice device);
        void onScanError(String error);
    }
    
    @Inject
    public CellularScanController(@ApplicationContext Context context) {
        this.context = context;
        this.telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }
    
    public void setScanListener(ScanListener listener) {
        this.scanListener = listener;
    }

    
    /**
     * Start cellular signal monitoring.
     */
    public void startScan() {
        if (telephonyManager == null) {
            if (scanListener != null) {
                scanListener.onScanError("Telephony service not available");
            }
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
            if (scanListener != null) {
                scanListener.onScanError("Phone state permission not granted");
            }
            return;
        }
        
        isScanning = true;
        setupPhoneStateListener();
    }
    
    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                super.onSignalStrengthsChanged(signalStrength);
                
                int dbm = getSignalStrengthDbm(signalStrength);
                String operatorName = telephonyManager.getNetworkOperatorName();
                String networkOperator = telephonyManager.getNetworkOperator();
                
                SignalDevice device = new SignalDevice(
                    "CELLULAR_" + networkOperator,
                    operatorName,
                    DeviceType.CELLULAR,
                    operatorName,
                    dbm,
                    null,
                    0.0, // Distance not applicable for cellular
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    false,
                    false,
                    false
                );
                
                if (scanListener != null) {
                    scanListener.onSignalUpdate(device);
                }
            }
        };
        
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    
    /**
     * Stop cellular signal monitoring.
     */
    public void stopScan() {
        if (!isScanning) return;
        
        isScanning = false;
        
        if (phoneStateListener != null && telephonyManager != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
            phoneStateListener = null;
        }
    }
    
    /**
     * Get signal strength in dBm from SignalStrength object.
     */
    private int getSignalStrengthDbm(SignalStrength signalStrength) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            java.util.List<CellSignalStrength> cellSignalStrengths = 
                signalStrength.getCellSignalStrengths();
            if (!cellSignalStrengths.isEmpty()) {
                return cellSignalStrengths.get(0).getDbm();
            }
        }
        
        // Fallback for older versions
        // Approximate conversion: level 0-4 maps to -113 to -51 dBm
        int level = signalStrength.getLevel();
        return -113 + (level * 28);
    }
    
    public boolean isScanning() {
        return isScanning;
    }
}
