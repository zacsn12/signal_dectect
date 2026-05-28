package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import org.zacsn.signal_dectect.databinding.ActivityDeviceDetailBinding;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.MacVendorUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeviceDetailActivity extends AppCompatActivity {
    
    private ActivityDeviceDetailBinding binding;
    private SignalDevice device;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDeviceDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Enable back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设备详情");
        }
        
        // Get device data from intent
        loadDeviceData();
        displayDeviceInfo();
    }
    
    private void loadDeviceData() {
        String macAddress = getIntent().getStringExtra("MAC_ADDRESS");
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");
        String deviceType = getIntent().getStringExtra("DEVICE_TYPE");
        String manufacturer = getIntent().getStringExtra("MANUFACTURER");
        int signalStrength = getIntent().getIntExtra("SIGNAL_STRENGTH", 0);
        int frequency = getIntent().getIntExtra("FREQUENCY", 0);
        double distance = getIntent().getDoubleExtra("DISTANCE", 0.0);
        long firstSeen = getIntent().getLongExtra("FIRST_SEEN", 0);
        long lastSeen = getIntent().getLongExtra("LAST_SEEN", 0);
        
        // Create device object (simplified, you may want to pass the whole object via Parcelable)
        // For now, we'll just use the individual fields
    }
    
    private void displayDeviceInfo() {
        String macAddress = getIntent().getStringExtra("MAC_ADDRESS");
        String deviceName = getIntent().getStringExtra("DEVICE_NAME");
        String deviceType = getIntent().getStringExtra("DEVICE_TYPE");
        String manufacturer = getIntent().getStringExtra("MANUFACTURER");
        int signalStrength = getIntent().getIntExtra("SIGNAL_STRENGTH", 0);
        int frequency = getIntent().getIntExtra("FREQUENCY", 0);
        double distance = getIntent().getDoubleExtra("DISTANCE", 0.0);
        long firstSeen = getIntent().getLongExtra("FIRST_SEEN", 0);
        long lastSeen = getIntent().getLongExtra("LAST_SEEN", 0);
        
        // Determine if this is a WiFi device
        boolean isWifi = deviceType != null && deviceType.equals("WIFI");
        boolean isBluetooth = deviceType != null && 
            (deviceType.equals("BLUETOOTH_LE") || deviceType.equals("BLUETOOTH_CLASSIC"));
        
        // Update card title based on device type
        if (isWifi) {
            binding.tvCardTitle.setText("WiFi信息");
        } else if (isBluetooth) {
            binding.tvCardTitle.setText("蓝牙信息");
        } else {
            binding.tvCardTitle.setText("设备信息");
        }
        
        // Display signal strength gauge
        binding.tvSignalStrength.setText(signalStrength + " dBm");
        binding.signalGauge.setProgress(convertDbmToProgress(signalStrength));
        
        // Display device information
        binding.tvMacAddress.setText(macAddress != null ? macAddress : "未知");
        
        // Update device type label based on device type
        if (isWifi) {
            binding.labelDeviceType.setText("WiFi类型:");
            binding.tvDeviceType.setText(getWifiTypeDescription(deviceType));
        } else if (isBluetooth) {
            binding.labelDeviceType.setText("蓝牙类型:");
            binding.tvDeviceType.setText(getBluetoothTypeDescription(deviceType));
        } else {
            binding.labelDeviceType.setText("设备类型:");
            binding.tvDeviceType.setText(deviceType != null ? deviceType : "未知");
        }
        
        // Update device name label
        if (isWifi) {
            binding.labelDeviceName.setText("SSID:");
        } else {
            binding.labelDeviceName.setText("蓝牙名称:");
        }
        binding.tvDeviceName.setText(deviceName != null && !deviceName.isEmpty() ? deviceName : "Unknown Device");
        
        binding.tvDistance.setText(String.format(Locale.getDefault(), "~%.1f米", distance));
        binding.tvSignalStrengthValue.setText(signalStrength + " dBm");
        
        // Get vendor from MAC address
        String vendor = "未知";
        if (macAddress != null && !macAddress.isEmpty()) {
            vendor = MacVendorUtils.getVendor(macAddress);
            if (vendor == null || vendor.isEmpty() || vendor.equals("Unknown")) {
                vendor = "未知";
            }
        }
        binding.tvManufacturer.setText(vendor);
        
        // Display frequency if available
        if (frequency > 0) {
            binding.tvFrequency.setText(frequency + " MHz");
            binding.layoutFrequency.setVisibility(android.view.View.VISIBLE);
        } else {
            binding.layoutFrequency.setVisibility(android.view.View.GONE);
        }
        
        // Display timestamps
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        binding.tvFirstSeen.setText(firstSeen > 0 ? sdf.format(new Date(firstSeen)) : "未知");
        binding.tvLastSeen.setText(lastSeen > 0 ? sdf.format(new Date(lastSeen)) : "未知");
        
        // Display device class/type info
        binding.tvDeviceClass.setText(getDeviceClassDescription(deviceType));
    }
    
    private String getWifiTypeDescription(String deviceType) {
        return "WiFi接入点";
    }
    
    private String getBluetoothTypeDescription(String deviceType) {
        if (deviceType == null) return "未知";
        
        switch (deviceType) {
            case "BLUETOOTH_LE":
                return "低功耗蓝牙";
            case "BLUETOOTH_CLASSIC":
                return "经典蓝牙";
            default:
                return "未知";
        }
    }
    
    private int convertDbmToProgress(int dbm) {
        // Convert dBm to progress (0-100)
        // Typical range: -100 dBm (weak) to -30 dBm (strong)
        if (dbm >= -30) return 100;
        if (dbm <= -100) return 0;
        return (int) ((dbm + 100) * 100.0 / 70.0);
    }
    
    private String getDeviceClassDescription(String deviceType) {
        if (deviceType == null) return "未知设备类型";
        
        switch (deviceType) {
            case "BLUETOOTH_LE":
                return "低功耗蓝牙设备";
            case "BLUETOOTH_CLASSIC":
                return "经典蓝牙设备";
            case "WIFI":
                return "WiFi接入点";
            case "CELLULAR":
                return "蜂窝基站";
            default:
                return "未知设备类型";
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
