package org.zacsn.signal_dectect.presentation.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
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
import org.zacsn.signal_dectect.util.SoundTestHelper;
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
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        binding = ActivitySignalInspectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize handler and sound effect manager
        handler = new android.os.Handler(android.os.Looper.getMainLooper());
        soundEffectManager = new SoundEffectManager(this);
        
        // Log sound manager initialization
        android.util.Log.i("SignalInspectActivity", "SoundEffectManager created");
        
        // Get scan type from intent
        String scanTypeStr = getIntent().getStringExtra("SCAN_TYPE");
        if (scanTypeStr != null) {
            switch (scanTypeStr) {
                case "BLUETOOTH":
                    scanType = ScanType.BLUETOOTH_ONLY;
                    break;
                case "WIFI":
                case "LAN":
                    scanType = ScanType.WIFI_ONLY;
                    break;
                case "CELLULAR":
                    scanType = ScanType.CELLULAR_ONLY;
                    break;
                default:
                    scanType = ScanType.ALL;
                    break;
            }
        } else {
            scanType = ScanType.ALL;
        }
        
        setTitle("信号巡检");
        binding.tvTitle.setText("信号巡检");

        // Volume adjustment icon
        binding.btnVolume.setOnClickListener(v -> {
            android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
            if (audioManager == null) return;
            
            View popupView = getLayoutInflater().inflate(org.zacsn.signal_dectect.R.layout.popup_volume, null);
            int popupWidth = (int) (300 * getResources().getDisplayMetrics().density);
            android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                    popupView,
                    popupWidth,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    true
            );
            
            // Required for tapping outside to dismiss
            popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            
            android.widget.SeekBar seekBar = popupView.findViewById(org.zacsn.signal_dectect.R.id.seek_bar_volume);
            int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
            int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
            
            seekBar.setMax(maxVolume);
            seekBar.setProgress(currentVolume);
            updateSoundEffectVolume(currentVolume, maxVolume);
            
            seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, progress, 0);
                        updateSoundEffectVolume(progress, maxVolume);
                    }
                }
                @Override
                public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
            });
            
            // Show popup aligned to the end (right) of the button
            popupWindow.showAsDropDown(v, 0, 20, android.view.Gravity.END);
        });

        // Settings gear popup menu
        binding.btnSettings.setOnClickListener(v -> {
            android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, v);
            popupMenu.getMenu().add(0, 1, 0, "蓝牙巡检").setChecked(scanType == ScanType.BLUETOOTH_ONLY);
            popupMenu.getMenu().add(0, 2, 0, "WiFi巡检").setChecked(scanType == ScanType.WIFI_ONLY);
            popupMenu.getMenu().add(0, 3, 0, "蜂窝巡检").setChecked(scanType == ScanType.CELLULAR_ONLY);
            popupMenu.getMenu().add(0, 4, 0, "全部扫描").setChecked(scanType == ScanType.ALL);
            popupMenu.getMenu().setGroupCheckable(0, true, true);
            popupMenu.setOnMenuItemClickListener(item -> {
                if (isScanning) {
                    android.widget.Toast.makeText(this, "请先停止巡检再切换模式", android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                }
                switch (item.getItemId()) {
                    case 1:
                        scanType = ScanType.BLUETOOTH_ONLY;
                        break;
                    case 2:
                        scanType = ScanType.WIFI_ONLY;
                        break;
                    case 3:
                        scanType = ScanType.CELLULAR_ONLY;
                        break;
                    case 4:
                        scanType = ScanType.ALL;
                        break;
                }
                android.widget.Toast.makeText(this, "已切换为: " + item.getTitle(), android.widget.Toast.LENGTH_SHORT).show();
                return true;
            });
            popupMenu.show();
        });
        
        // Setup custom back button
        binding.btnBack.setOnClickListener(v -> onBackPressed());
        
        // Add long-press on title to run audio test (for debugging)
        binding.tvTitle.setOnLongClickListener(v -> {
            android.util.Log.i("SignalInspectActivity", "Running audio test...");
            android.widget.Toast.makeText(this, "运行音频测试...", android.widget.Toast.LENGTH_SHORT).show();
            SoundTestHelper.runAudioTest(this);
            return true;
        });
        
        viewModel = new ViewModelProvider(this).get(SignalInspectViewModel.class);
        
        setupRecyclerView();
        setupFab();
        observeViewModel();
    }

    private void updateSoundEffectVolume(int volume, int maxVolume) {
        if (soundEffectManager == null || maxVolume <= 0) {
            return;
        }
        soundEffectManager.setVolume(volume / (float) maxVolume);
    }

    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
        boolean handled = super.onKeyDown(keyCode, event);
        if (keyCode == android.view.KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == android.view.KeyEvent.KEYCODE_VOLUME_MUTE) {
            syncSoundEffectVolumeFromSystem();
        }
        return handled;
    }

    private void syncSoundEffectVolumeFromSystem() {
        android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
        if (audioManager == null) {
            return;
        }
        int maxVolume = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC);
        int currentVolume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC);
        updateSoundEffectVolume(currentVolume, maxVolume);
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
            Log.i("SignalInspectActivity", "Inspect button clicked, isScanning=" + isScanning + ", scanType=" + scanType.toInt());
            if (isScanning) {
                soundEffectManager.stopAllSounds();
                viewModel.stopScan();
            } else {
                // Reset Apple device flags when starting new scan
                hasAppleClassicBluetooth = false;
                hasShownAppleLeInfo = false;
                alertedAppleDevices.clear();
                appleLeDevices.clear();
                boolean soundStarted = soundEffectManager.startNormalScanSound();
                if (!soundStarted) {
                    android.widget.Toast.makeText(this, "巡检音效启动失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
                }

                boolean scanStarted = viewModel.startScan(scanType);
                if (!scanStarted) {
                    soundEffectManager.stopAllSounds();
                    android.widget.Toast.makeText(this, "权限未授予，无法开始巡检", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    private void observeViewModel() {
        viewModel.getScanState().observe(this, state -> {
            isScanning = state == SignalInspectViewModel.ScanState.SCANNING;
            binding.fab.setIconResource(isScanning 
                ? android.R.drawable.ic_media_pause 
                : android.R.drawable.ic_media_play);
            binding.fab.setText(isScanning ? "停止巡检" : "开始巡检");
            
            // Record scan start time
            if (isScanning && scanStartTime == 0) {
                scanStartTime = System.currentTimeMillis();
            }
            
            // Control sound effects and radar sweep animations based on scanning state
            if (isScanning) {
                Log.i("SignalInspectActivity", "Starting scan - triggering sound effect");
                if (!soundEffectManager.isPlaying()) {
                    boolean soundStarted = soundEffectManager.startNormalScanSound();
                    if (!soundStarted) {
                        android.widget.Toast.makeText(this, "巡检音效启动失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                android.view.animation.Animation rotateAnim = android.view.animation.AnimationUtils.loadAnimation(this, org.zacsn.signal_dectect.R.anim.radar_sweep);
                binding.radarSweep.startAnimation(rotateAnim);
            } else if (state == SignalInspectViewModel.ScanState.ERROR) {
                Log.w("SignalInspectActivity", "Scan failed - stopping sound effect");
                soundEffectManager.stopAllSounds();
                binding.radarSweep.clearAnimation();
            } else {
                Log.i("SignalInspectActivity", "Stopping scan - stopping sound effect");
                soundEffectManager.stopAllSounds();
                binding.radarSweep.clearAnimation();
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
            binding.tvLargeDeviceCount.setText(String.valueOf(sortedDevices.size()));
            
            // Check for Apple devices and switch to alert sound if found
            checkForAppleDevices(sortedDevices);
        });
        
        viewModel.getScanDuration().observe(this, duration -> {
            binding.tvDuration.setText("扫描时长: " + duration + "s");
        });

        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show();
            }
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
                        boolean soundStarted = soundEffectManager.switchToAlertSound();
                        if (!soundStarted) {
                            android.widget.Toast.makeText(this, "告警音效启动失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
                        }
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
        View dialogView = getLayoutInflater().inflate(org.zacsn.signal_dectect.R.layout.dialog_apple_alert, null);
        TextView tvTitle = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_message);
        Button btnNegative = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_negative);
        Button btnPositive = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_positive);
        
        tvTitle.setText("警告：检测到 Apple 经典设备");
        
        // Style the header container to red warning color for classic bluetooth
        View headerView = dialogView.findViewById(org.zacsn.signal_dectect.R.id.dialog_header);
        headerView.setBackgroundColor(android.graphics.Color.parseColor("#991B1B")); // Red warning color
        
        String info = "发现 Apple 经典蓝牙设备！\n\n" +
                      "⚠️ 这可能是正在广播或连接的 iPhone、iPad 或 Mac 终端。\n\n" +
                      "• 设备名称: " + device.getDeviceName() + "\n" +
                      "• MAC 地址: " + device.getMacAddress() + "\n" +
                      "• 信号强度: " + device.getSignalStrength() + " dBm\n" +
                      "• 芯片厂商: " + device.getManufacturer() + "\n" +
                      "• 设备类型: 经典蓝牙";
        tvMessage.setText(info);
        
        btnNegative.setText("继续扫描");
        btnPositive.setText("查看详情");
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
                
        btnNegative.setOnClickListener(v -> dialog.dismiss());
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            openDeviceDetail(device);
        });
        
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = displayMetrics.widthPixels;
            int displayHeight = displayMetrics.heightPixels;
            
            android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (displayWidth * 0.85);
            dialog.getWindow().setAttributes(layoutParams);
            
            View dialogScroll = dialogView.findViewById(org.zacsn.signal_dectect.R.id.dialog_scroll_view);
            if (dialogScroll != null) {
                int maxScrollHeight = (int) (displayHeight * 0.40);
                dialogScroll.post(() -> {
                    if (dialogScroll.getMeasuredHeight() > maxScrollHeight) {
                        android.view.ViewGroup.LayoutParams lp = dialogScroll.getLayoutParams();
                        lp.height = maxScrollHeight;
                        dialogScroll.setLayoutParams(lp);
                    }
                });
            }
        }
    }
    
    /**
     * Show info dialog for Apple BLE devices.
     */
    private void showAppleLeDevicesInfo() {
        View dialogView = getLayoutInflater().inflate(org.zacsn.signal_dectect.R.layout.dialog_apple_alert, null);
        TextView tvTitle = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_message);
        Button btnNegative = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_negative);
        Button btnPositive = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_positive);
        
        tvTitle.setText("感知：检测到 Apple BLE 设备");
        
        // Keep header deep police blue (#0A1E36) for info
        View headerView = dialogView.findViewById(org.zacsn.signal_dectect.R.id.dialog_header);
        headerView.setBackgroundColor(android.graphics.Color.parseColor("#0A1E36"));
        
        StringBuilder message = new StringBuilder();
        message.append("检测到 ").append(appleLeDevices.size()).append(" 个 Apple 低功耗蓝牙终端。\n\n");
        message.append("ℹ️ 这些设备可能是周边的 AirPods 耳机、Apple Watch 手表等。\n\n");
        message.append("探测列表：\n");
        
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
        
        tvMessage.setText(message.toString());
        
        btnNegative.setVisibility(View.GONE); // Only need one button for simple alert
        btnPositive.setText("知道了");
        
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
                
        btnPositive.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = displayMetrics.widthPixels;
            int displayHeight = displayMetrics.heightPixels;
            
            android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (displayWidth * 0.85);
            dialog.getWindow().setAttributes(layoutParams);
            
            View dialogScroll = dialogView.findViewById(org.zacsn.signal_dectect.R.id.dialog_scroll_view);
            if (dialogScroll != null) {
                int maxScrollHeight = (int) (displayHeight * 0.40);
                dialogScroll.post(() -> {
                    if (dialogScroll.getMeasuredHeight() > maxScrollHeight) {
                        android.view.ViewGroup.LayoutParams lp = dialogScroll.getLayoutParams();
                        lp.height = maxScrollHeight;
                        dialogScroll.setLayoutParams(lp);
                    }
                });
            }
        }
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
        } else if (scanType == ScanType.CELLULAR_ONLY) {
            return "蜂窝巡检";
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
        } else if (scanType == ScanType.CELLULAR_ONLY) {
            return 4;  // Cellular
        } else if (scanType == ScanType.ALL) {
            return 7;  // BT + WiFi + Cellular (1+2+4)
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
