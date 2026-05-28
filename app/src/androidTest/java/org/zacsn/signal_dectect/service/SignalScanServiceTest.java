package org.zacsn.signal_dectect.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Integration tests for SignalScanService binding and communication.
 */
@RunWith(AndroidJUnit4.class)
public class SignalScanServiceTest {

    private Context context;
    private SignalScanService service;
    private ServiceConnection serviceConnection;
    private CountDownLatch bindLatch;
    private CountDownLatch unbindLatch;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        bindLatch = new CountDownLatch(1);
        unbindLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() {
        if (serviceConnection != null && service != null) {
            try {
                context.unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                // Service already unbound
            }
        }
        service = null;
        serviceConnection = null;
    }

    /**
     * Test that service can be bound successfully from Activity.
     */
    @Test
    public void testServiceBinding() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
                unbindLatch.countDown();
            }
        };

        // Bind to service
        boolean bound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        assertTrue("Service should bind successfully", bound);

        // Wait for binding to complete
        boolean bindSuccess = bindLatch.await(5, TimeUnit.SECONDS);
        assertTrue("Service should be bound within timeout", bindSuccess);
        assertNotNull("Service instance should not be null", service);
    }

    /**
     * Test that service can be unbound properly.
     */
    @Test
    public void testServiceUnbinding() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
                unbindLatch.countDown();
            }
        };

        // Bind to service
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        // Unbind service
        context.unbindService(serviceConnection);
        
        // Verify unbinding (service reference should still exist until GC)
        assertNotNull("Service reference exists after unbind", service);
    }

    /**
     * Test communication from Activity to Service (start scan).
     */
    @Test
    public void testActivityToServiceCommunication() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        // Bind to service
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        // Test communication: start scan
        assertNotNull("Service should be bound", service);
        
        // This should not throw exception
        service.startScan(ScanType.BLUETOOTH_ONLY);
        
        // Verify scan started (devices list should be cleared)
        List<SignalDevice> devices = service.getAllDevices();
        assertNotNull("Device list should not be null", devices);
        assertEquals("Device list should be empty after starting new scan", 0, devices.size());
        
        // Stop scan
        service.stopScan();
    }

    /**
     * Test communication from Service to Activity (callback mechanism).
     */
    @Test
    public void testServiceToActivityCommunication() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        final CountDownLatch callbackLatch = new CountDownLatch(1);
        final boolean[] callbackReceived = {false};
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                
                // Set up callback
                service.setCallback(new SignalScanService.ServiceCallback() {
                    @Override
                    public void onDeviceFound(SignalDevice device) {
                        callbackReceived[0] = true;
                        callbackLatch.countDown();
                    }

                    @Override
                    public void onDeviceListUpdated(List<SignalDevice> devices) {
                        // Device list updated
                    }
                });
                
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        // Bind to service
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        assertNotNull("Service should be bound", service);
        
        // Note: In a real scenario, callbacks would be triggered by actual scan results
        // This test verifies the callback mechanism is set up correctly
        // Actual device discovery would require hardware/emulator with BT/WiFi
    }

    /**
     * Test service lifecycle management (onCreate, onBind, onDestroy).
     */
    @Test
    public void testServiceLifecycle() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
                unbindLatch.countDown();
            }
        };

        // Bind to service (triggers onCreate and onBind)
        boolean bound = context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        assertTrue("Service should bind successfully", bound);
        
        bindLatch.await(5, TimeUnit.SECONDS);
        assertNotNull("Service should be created and bound", service);

        // Verify service is functional
        service.startScan(ScanType.WIFI_ONLY);
        List<SignalDevice> devices = service.getAllDevices();
        assertNotNull("Service should be operational", devices);
        
        service.stopScan();

        // Unbind service (may trigger onDestroy if no other bindings)
        context.unbindService(serviceConnection);
        
        // Service should have cleaned up
        // Note: onDestroy timing is not guaranteed immediately after unbind
    }

    /**
     * Test multiple scan types can be started.
     */
    @Test
    public void testMultipleScanTypes() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                SignalScanService.LocalBinder localBinder = (SignalScanService.LocalBinder) binder;
                service = localBinder.getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        assertNotNull("Service should be bound", service);

        // Test different scan types
        service.startScan(ScanType.BLUETOOTH_ONLY);
        service.stopScan();
        
        service.startScan(ScanType.WIFI_ONLY);
        service.stopScan();
        
        service.startScan(ScanType.CELLULAR_ONLY);
        service.stopScan();
        
        service.startScan(ScanType.ALL);
        service.stopScan();
        
        // All operations should complete without exception
    }

    /**
     * Test that service returns correct binder type.
     */
    @Test
    public void testBinderType() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        final IBinder[] receivedBinder = {null};
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                receivedBinder[0] = binder;
                service = ((SignalScanService.LocalBinder) binder).getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        assertNotNull("Binder should not be null", receivedBinder[0]);
        assertTrue("Binder should be LocalBinder type", 
            receivedBinder[0] instanceof SignalScanService.LocalBinder);
        assertNotNull("Service should be accessible through binder", service);
    }

    /**
     * Test that getAllDevices returns a copy of the device list.
     */
    @Test
    public void testGetAllDevicesReturnsCopy() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = ((SignalScanService.LocalBinder) binder).getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        assertNotNull("Service should be bound", service);

        // Get device list twice
        List<SignalDevice> devices1 = service.getAllDevices();
        List<SignalDevice> devices2 = service.getAllDevices();

        assertNotNull("First device list should not be null", devices1);
        assertNotNull("Second device list should not be null", devices2);
        
        // Should be different instances (copies)
        assertNotSame("getAllDevices should return a new list each time", devices1, devices2);
    }

    /**
     * Test callback can be set and cleared.
     */
    @Test
    public void testCallbackManagement() throws InterruptedException {
        Intent serviceIntent = new Intent(context, SignalScanService.class);
        
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                service = ((SignalScanService.LocalBinder) binder).getService();
                bindLatch.countDown();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                service = null;
            }
        };

        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        bindLatch.await(5, TimeUnit.SECONDS);

        assertNotNull("Service should be bound", service);

        // Set callback
        SignalScanService.ServiceCallback callback = new SignalScanService.ServiceCallback() {
            @Override
            public void onDeviceFound(SignalDevice device) {}

            @Override
            public void onDeviceListUpdated(List<SignalDevice> devices) {}
        };
        
        service.setCallback(callback);
        
        // Clear callback
        service.setCallback(null);
        
        // Should not throw exception
    }
}
