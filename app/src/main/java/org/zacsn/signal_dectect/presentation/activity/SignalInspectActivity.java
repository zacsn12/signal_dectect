package org.zacsn.signal_dectect.presentation.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.gson.Gson;
import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import org.zacsn.signal_dectect.databinding.ActivitySignalInspectBinding;
import org.zacsn.signal_dectect.domain.model.ScanType;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.presentation.adapter.SignalDeviceAdapter;
import org.zacsn.signal_dectect.presentation.viewmodel.SignalInspectViewModel;
import org.zacsn.signal_dectect.util.SoundEffectManager;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class SignalInspectActivity extends AppCompatActivity {
    
    private ActivitySignalInspectBinding binding;
    private SignalInspectViewModel viewModel;
    private SignalDeviceAdapter adapter;
    private SoundEffectManager soundEffectManager;
    private android.os.Handler handler;
    private boolean isScanning = false;
    private ScanType scanType = ScanType.ALL;
    private boolean hasAppleClassicBluetooth = false;
    private boolean hasShownAppleLeInfo = false;
    private java.util.Set<String> alertedAppleDevices = new java.util.HashSet<>();
    private java.util.List<SignalDevice> appleLeDevices = new java.util.ArrayList<>();
    private java.util.List<SignalDevice> currentDevices = new java.util.ArrayList<>();
    private long scanStartTime = 0;
    
    @Inject
    ScanRecordDao scanRecordDao;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignalInspectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Initialize handler and sound effect manager
        handler = new android.os.Handler(android.os.Looper.getMainLooper());
        soundEffectManager = new SoundEffectManager(this);
        
        // Get scan type from intent
        String scanTypeStr = getIntent().getStringExtra("SCAN_TYPE");
        if (scanTypeStr != null) {
            switch (scanTypeStr) {
                case "BLUETOOTH":
                    scanType = ScanType.BLUETOOTH_ONLY;
                    setTitle("蓝牙巡检");
                    break;
                case "WIFI":
                    scanType = ScanType.WIFI_ONLY;
                    setTitle("WiFi巡检");
                    break;
                case "LAN":
                    scanType = ScanType.WIFI_ONLY; // LAN scan uses WiFi
                    setTitle("局域网扫描");
                    break;
                default:
                    scanType = ScanType.ALL;
                    setTitle("信号检测");
                    break;
            }
        }
        
        viewModel = new ViewModelProvider(this).get(SignalInspectViewModel.class);
        
        setupRecyclerView();
        setupFab();
        observeViewModel();
    }
    
    private void setupRecyclerView() {
        adapter = new SignalDeviceAdapter(device -> {
            // Handle device click - open detail activity
            openDeviceDetail(device);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void openDeviceDetail(org.zacsn.signal_dectect.domain.model.SignalDevice device) {
        android.content.Intent intent = new android.content.Intent(this, DeviceDetailActivity.class);
        intent.putExtra("MAC_ADDRESS", device.getMacAddress());
        intent.putExtra("DEVICE_NAME", device.getDeviceName());
        intent.putExtra("DEVICE_TYPE", device.getDeviceType().name());
        intent.putExtra("MANUFACTURER", device.getManufacturer());
        intent.putExtra("SIGNAL_STRENGTH", device.getSignalStrength());
        intent.putExtra("FREQUENCY", device.getFrequency() != null ? device.getFrequency() : 0);
        intent.putExtra("DISTANCE", device.getDistance());
        intent.putExtra("FIRST_SEEN", device.getFirstSeen());
        intent.putExtra("LAST_SEEN", device.getLastSeen());
        startActivity(intent);
    }
    
    private void setupFab() {
        binding.fab.setOnClickListener(v -> {
            if (isScanning) {
                viewModel.stopScan();
            } else {
                // Reset Apple device flags when starting new scan
                hasAppleClassicBluetooth = false;
                hasShownAppleLeInfo = false;
                alertedAppleDevices.clear();
                appleLeDevices.clear();
                viewModel.startScan(scanType);
            }
        });
    }
    
    private void observeViewModel() {
        viewModel.getScanState().observe(this, state -> {
            isScanning = state == SignalInspectViewModel.ScanState.SCANNING;
            binding.fab.setImageResource(isScanning 
                ? android.R.drawable.ic_media_pause 
                : android.R.drawable.ic_media_play);
            
            // Record scan start time
            if (isScanning && scanStartTime == 0) {
                scanStartTime = System.currentTimeMillis();
            }
            
            // Control sound effects based on scanning state
            if (isScanning) {
                soundEffectManager.startNormalScanSound();
            } else {
                soundEffectManager.stopAllSounds();
            }
        });
        
        viewModel.getDevices().observe(this, devices -> {
            // Store current devices for saving
            currentDevices = new java.util.ArrayList<>(devices);
            
            // Sort devices: Classic Bluetooth first, then BLE, then others
            java.util.List<SignalDevice> sortedDevices = new java.util.ArrayList<>(devices);
            sortedDevices.sort((d1, d2) -> {
                int priority1 = getDeviceTypePriority(d1.getDeviceType());
                int priority2 = getDeviceTypePriority(d2.getDeviceType());
                return Integer.compare(priority1, priority2);
            });
            
            adapter.submitList(sortedDevices);
            binding.tvDeviceCount.setText("设备数: " + sortedDevices.size());
            
            // Check for Apple devices and switch to alert sound if found
            checkForAppleDevices(sortedDevices);
        });
        
        viewModel.getScanDuration().observe(this, duration -> {
            binding.tvDuration.setText("扫描时长: " + duration + "s");
        });
    }
    
    /**
     * Get device type priority for sorting.
     * Lower number = higher priority (appears first in list).
     */
    private int getDeviceTypePriority(org.zacsn.signal_dectect.domain.model.DeviceType deviceType) {
        switch (deviceType) {
            case BLUETOOTH_CLASSIC:
                return 1; // Highest priority
            case BLUETOOTH_LE:
                return 2; // Second priority
            case WIFI:
                return 3; // Third priority
            default:
                return 4; // Lowest priority
        }
    }
    
    /**
     * Check if any Apple devices are in the device list.
     * - Classic Bluetooth Apple devices: Switch to alert sound and show urgent dialog
     * - BLE Apple devices: Collect and show info dialog (no sound change)
     */
    private void checkForAppleDevices(java.util.List<SignalDevice> devices) {
        if (!isScanning) {
            return; // Not scanning
        }
        
        // Track newly found BLE devices for batch notification
        boolean foundNewAppleLe = false;
        
        for (SignalDevice device : devices) {
            String manufacturer = device.getManufacturer();
            String macAddress = device.getMacAddress();
            
            if (manufacturer != null && manufacturer.toLowerCase().contains("apple")) {
                
                // Skip if already alerted for this device
                if (alertedAppleDevices.contains(macAddress)) {
                    continue;
                }
                
                // Check device type
                if (device.getDeviceType() == org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_CLASSIC) {
                    // Classic Bluetooth Apple device - HIGH PRIORITY
                    if (!hasAppleClassicBluetooth) {
                        hasAppleClassicBluetooth = true;
                        soundEffectManager.switchToAlertSound();
                    }
                    
                    alertedAppleDevices.add(macAddress);
                    android.util.Log.w("SignalInspectActivity", 
                            "Apple Classic Bluetooth device detected! MAC: " + macAddress);
                    
                    // Show urgent alert dialog immediately
                    showAppleClassicBluetoothAlert(device);
                    
                } else if (device.getDeviceType() == org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_LE) {
                    // BLE Apple device - LOW PRIORITY
                    boolean isNewDevice = true;
                    for (SignalDevice existingDevice : appleLeDevices) {
                        if (existingDevice.getMacAddress().equals(macAddress)) {
                            isNewDevice = false;
                            break;
                        }
                    }
                    
                    if (isNewDevice) {
                        appleLeDevices.add(device);
                        alertedAppleDevices.add(macAddress);
                        foundNewAppleLe = true;
                        android.util.Log.i("SignalInspectActivity", 
                                "Apple BLE device detected: " + device.getDeviceName() + " (" + macAddress + ")");
                    }
                }
            }
        }
        
        // Show BLE devices info if new devices found (only if no classic BT alert is active)
        if (foundNewAppleLe && !hasAppleClassicBluetooth && !hasShownAppleLeInfo) {
            hasShownAppleLeInfo = true;
            // Delay showing BLE info to allow more devices to be collected
            handler.postDelayed(() -> {
                if (!appleLeDevices.isEmpty()) {
                    showAppleLeDevicesInfo();
                }
            }, 2000); // Wait 2 seconds to collect more BLE devices
        }
    }
    
    /**
     * Show urgent alert dialog for Apple Classic Bluetooth device.
     */
    private void showAppleClassicBluetoothAlert(SignalDevice device) {
        new AlertDialog.Builder(this)
                .setTitle("🚨 警告：检测到 Apple 经典蓝牙设备")
                .setMessage("发现 Apple 经典蓝牙设备！\n\n" +
                        "⚠️ 这可能是 iPhone、iPad 或 Mac 设备\n\n" +
                        "设备名称: " + device.getDeviceName() + "\n" +
                        "MAC 地址: " + device.getMacAddress() + "\n" +
                        "厂商: " + device.getManufacturer() + "\n" +
                        "信号强度: " + device.getSignalStrength() + " dBm\n" +
                        "设备类型: 经典蓝牙")
                .setPositiveButton("查看详情", (dialog, which) -> {
                    openDeviceDetail(device);
                })
                .setNegativeButton("继续扫描", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    /**
     * Show info dialog for Apple BLE devices.
     */
    private void showAppleLeDevicesInfo() {
        StringBuilder message = new StringBuilder();
        message.append("检测到 ").append(appleLeDevices.size()).append(" 个 Apple 低功耗蓝牙设备\n\n");
        message.append("ℹ️ 这些设备可能是 AirPods、Apple Watch 等\n\n");
        message.append("设备列表：\n");
        
        for (int i = 0; i < appleLeDevices.size() && i < 5; i++) {
            SignalDevice device = appleLeDevices.get(i);
            message.append("\n").append(i + 1).append(". ")
                    .append(device.getDeviceName())
                    .append("\n   MAC: ").append(device.getMacAddress())
                    .append("\n   信号: ").append(device.getSignalStrength()).append(" dBm");
        }
        
        if (appleLeDevices.size() > 5) {
            message.append("\n\n... 还有 ").append(appleLeDevices.size() - 5).append(" 个设备");
        }
        
        new AlertDialog.Builder(this)
                .setTitle("ℹ️ 检测到 Apple BLE 设备")
                .setMessage(message.toString())
                .setPositiveButton("知道了", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .setIcon(android.R.drawable.ic_dialog_info)
                .show();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Don't stop scanning when just pausing (e.g., opening detail activity)
        // Only stop when finishing
    }
    
    @Override
    public void onBackPressed() {
        // If there are scanned devices, ask if user wants to save
        if (currentDevices != null && !currentDevices.isEmpty()) {
            showSaveRecordDialog();
        } else {
            super.onBackPressed();
        }
    }
    
    /**
     * Show dialog to ask if user wants to save scan record.
     */
    private void showSaveRecordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("保存扫描记录")
                .setMessage("是否要保存本次扫描记录？\n\n设备数: " + currentDevices.size())
                .setPositiveButton("保存", (dialog, which) -> {
                    showSaveRecordNameDialog();
                })
                .setNegativeButton("不保存", (dialog, which) -> {
                    finish();
                })
                .setNeutralButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * Show dialog to input record name.
     */
    private void showSaveRecordNameDialog() {
        EditText editText = new EditText(this);
        editText.setHint("输入记录名称（可选）");
        
        // Generate default name
        String defaultName = getScanTypeName() + " - " + 
                new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date());
        editText.setText(defaultName);
        
        new AlertDialog.Builder(this)
                .setTitle("记录名称")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String recordName = editText.getText().toString().trim();
                    if (recordName.isEmpty()) {
                        recordName = defaultName;
                    }
                    saveRecord(recordName);
                    finish();
                })
                .setNegativeButton("取消", (dialog, which) -> {
                    finish();
                })
                .show();
    }
    
    /**
     * Save scan record to database.
     */
    private void saveRecord(String recordName) {
        long timestamp = System.currentTimeMillis();
        int scanTypeInt = getScanTypeInt();
        long duration = scanStartTime > 0 ? (timestamp - scanStartTime) / 1000 : 0;
        int deviceCount = currentDevices.size();
        
        // Convert devices to JSON
        Gson gson = new Gson();
        String devicesJson = gson.toJson(currentDevices);
        
        ScanRecordEntity record = new ScanRecordEntity(
                timestamp,
                scanTypeInt,
                duration,
                null,  // latitude
                null,  // longitude
                deviceCount,
                devicesJson
        );
        
        // Save in background thread
        new Thread(() -> {
            scanRecordDao.insertRecord(record);
            runOnUiThread(() -> {
                android.widget.Toast.makeText(this, "记录已保存", android.widget.Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
    
    /**
     * Get scan type name for display.
     */
    private String getScanTypeName() {
        if (scanType == ScanType.BLUETOOTH_ONLY) {
            return "蓝牙巡检";
        } else if (scanType == ScanType.WIFI_ONLY) {
            return "WiFi巡检";
        } else {
            return "扫描";
        }
    }
    
    /**
     * Get scan type as integer for database.
     */
    private int getScanTypeInt() {
        if (scanType == ScanType.BLUETOOTH_ONLY) {
            return 1;  // BT
        } else if (scanType == ScanType.WIFI_ONLY) {
            return 2;  // WiFi
        } else if (scanType == ScanType.ALL) {
            return 3;  // BT + WiFi
        } else {
            return 0;
        }
    }
    
    @Override
    protected void onStop() {
        super.onStop();
        // Stop scanning when activity is no longer visible and finishing
        if (isFinishing() && isScanning) {
            viewModel.stopScan();
            soundEffectManager.stopAllSounds();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure scanning is stopped when activity is destroyed
        if (isScanning) {
            viewModel.stopScan();
        }
        // Release sound effect manager
        soundEffectManager.release();
        binding = null;
    }
}
