package org.zacsn.signal_dectect;

import android.app.Application;
import android.util.Log;
import dagger.hilt.android.HiltAndroidApp;
import org.zacsn.signal_dectect.util.MacVendorUtils;

/**
 * Application class for Signal Detection App.
 * Annotated with @HiltAndroidApp to enable Hilt dependency injection.
 */
@HiltAndroidApp
public class SignalDetectApplication extends Application {
    
    private static final String TAG = "SignalDetectApp";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.i(TAG, "Application onCreate - Initializing MAC vendor database...");
        
        // Initialize MAC vendor database
        MacVendorUtils.initialize(this);
        
        Log.i(TAG, "MAC vendor database initialized: " + MacVendorUtils.isInitialized()
                + ", vendor count: " + MacVendorUtils.getVendorCount());
    }
}
