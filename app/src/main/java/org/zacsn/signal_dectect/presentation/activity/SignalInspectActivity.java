package org.zacsn.signal_dectect.presentation.activity;

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
import org.zacsn.signal_dectect.data.database.BlacklistDao;
import org.zacsn.signal_dectect.data.database.BlacklistItemEntity;
import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import org.zacsn.signal_dectect.data.database.WatchlistDao;
import org.zacsn.signal_dectect.data.database.WatchlistItemEntity;
import org.zacsn.signal_dectect.data.database.WhitelistDao;
import org.zacsn.signal_dectect.data.database.WhitelistItemEntity;
import org.zacsn.signal_dectect.databinding.ActivitySignalInspectBinding;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
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
    private boolean hasActiveConfiguredAlert = false;
    private java.util.Set<String> alertedConfiguredDevices = new java.util.HashSet<>();
    private java.util.Set<String> highlightedTargetDevices = new java.util.HashSet<>();
    private java.util.Set<String> watchlistKeywords = new java.util.HashSet<>();
    private java.util.Set<String> whitelistMacs = new java.util.HashSet<>();
    private java.util.Set<String> blacklistMacs = new java.util.HashSet<>();
    private java.util.List<SignalDevice> latestVisibleDevices = new java.util.ArrayList<>();
    private java.util.List<SignalDevice> currentDevices = new java.util.ArrayList<>();
    private long scanStartTime = 0;
    
    @Inject
    ScanRecordDao scanRecordDao;
    @Inject
    WatchlistDao watchlistDao;
    @Inject
    WhitelistDao whitelistDao;
    @Inject
    BlacklistDao blacklistDao;
    
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
        observeAlertConfig();
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
        intent.putExtra("CANDIDATE_MANUFACTURER", device.getCandidateManufacturer());
        intent.putExtra("MANUFACTURER_SOURCE", device.getManufacturerSource());
        intent.putExtra("MANUFACTURER_CONFIDENCE", device.getManufacturerConfidence());
        intent.putExtra("MANUFACTURER_VERDICT", device.getManufacturerVerdict().name());
        intent.putExtra("MANUFACTURER_EVIDENCE", device.getManufacturerEvidence());
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
                hasActiveConfiguredAlert = false;
                alertedConfiguredDevices.clear();
                highlightedTargetDevices.clear();
                adapter.setHighlightedMacs(highlightedTargetDevices);
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
            latestVisibleDevices = new java.util.ArrayList<>(devices);
            java.util.List<SignalDevice> sortedDevices = refreshDeviceList();

            checkConfiguredAlerts(sortedDevices);
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
    
    private void observeAlertConfig() {
        watchlistDao.getAll().observe(this, entities -> {
            java.util.Set<String> nextKeywords = new java.util.HashSet<>();
            for (WatchlistItemEntity entity : entities) {
                addKeyword(nextKeywords, entity.getMacAddress());
                addKeyword(nextKeywords, entity.getManufacturer());
                addKeyword(nextKeywords, entity.getDeviceName());
            }
            watchlistKeywords = nextKeywords;
        });

        whitelistDao.getAll().observe(this, entities -> {
            java.util.Set<String> nextMacs = new java.util.HashSet<>();
            for (WhitelistItemEntity entity : entities) {
                addMac(nextMacs, entity.getMacAddress());
            }
            whitelistMacs = nextMacs;
        });

        blacklistDao.getAll().observe(this, entities -> {
            java.util.Set<String> nextMacs = new java.util.HashSet<>();
            for (BlacklistItemEntity entity : entities) {
                addMac(nextMacs, entity.getMacAddress());
            }
            blacklistMacs = nextMacs;
        });
    }

    private java.util.List<SignalDevice> refreshDeviceList() {
        java.util.List<SignalDevice> sortedDevices = new java.util.ArrayList<>(latestVisibleDevices);
        sortedDevices.sort((d1, d2) -> {
            int targetPriority1 = highlightedTargetDevices.contains(normalizeMac(d1.getMacAddress())) ? 0 : 1;
            int targetPriority2 = highlightedTargetDevices.contains(normalizeMac(d2.getMacAddress())) ? 0 : 1;
            if (targetPriority1 != targetPriority2) {
                return Integer.compare(targetPriority1, targetPriority2);
            }

            int typePriority1 = getDeviceTypePriority(d1.getDeviceType());
            int typePriority2 = getDeviceTypePriority(d2.getDeviceType());
            if (typePriority1 != typePriority2) {
                return Integer.compare(typePriority1, typePriority2);
            }

            return Long.compare(d2.getLastSeen(), d1.getLastSeen());
        });

        adapter.submitList(sortedDevices);
        binding.tvDeviceCount.setText("设备数: " + sortedDevices.size());
        binding.tvLargeDeviceCount.setText(String.valueOf(sortedDevices.size()));
        return sortedDevices;
    }

    private void checkConfiguredAlerts(java.util.List<SignalDevice> devices) {
        if (!isScanning) {
            return;
        }

        for (SignalDevice device : devices) {
            String macAddress = normalizeMac(device.getMacAddress());
            if (macAddress.isEmpty() || whitelistMacs.contains(macAddress)) {
                continue;
            }

            String alertType = null;
            String alertReason = null;
            if (blacklistMacs.contains(macAddress)) {
                alertType = "黑名单告警";
                alertReason = "该设备 MAC 地址命中黑名单";
            } else {
                String matchedKeyword = findWatchlistMatch(device);
                if (matchedKeyword != null) {
                    alertType = "巡检机型告警";
                    alertReason = "设备信息命中巡检机型: " + matchedKeyword;
                }
            }

            if (alertType == null || alertedConfiguredDevices.contains(alertType + ":" + macAddress)) {
                continue;
            }

            alertedConfiguredDevices.add(alertType + ":" + macAddress);
            highlightedTargetDevices.add(macAddress);
            adapter.setHighlightedMacs(highlightedTargetDevices);
            refreshDeviceList();
            if (!hasActiveConfiguredAlert) {
                hasActiveConfiguredAlert = true;
                boolean soundStarted = soundEffectManager.switchToAlertSound();
                if (!soundStarted) {
                    android.widget.Toast.makeText(this, "告警音效启动失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            showConfiguredAlert(device, alertType, alertReason);
        }
    }

    private String findWatchlistMatch(SignalDevice device) {
        String manufacturer = safeLower(device.getManufacturer()).trim();
        String macAddress = safeLower(device.getMacAddress()).trim();

        for (String keyword : watchlistKeywords) {
            if (keyword.equals(macAddress) || keyword.equals(normalizeMac(device.getMacAddress()).toLowerCase(java.util.Locale.US))) {
                return keyword + " (MAC精确命中)";
            }

            if (isManufacturerConfirmed(device) && manufacturer.contains(keyword)) {
                return keyword + " (厂商已确认)";
            }
        }
        return null;
    }

    private boolean isManufacturerConfirmed(SignalDevice device) {
        return device.getManufacturerVerdict() == ManufacturerVerdict.CONFIRMED;
    }

    private void showConfiguredAlert(SignalDevice device, String alertType, String alertReason) {
        View dialogView = getLayoutInflater().inflate(org.zacsn.signal_dectect.R.layout.dialog_apple_alert, null);
        TextView tvTitle = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_dialog_message);
        Button btnNegative = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_negative);
        Button btnPositive = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_dialog_positive);

        tvTitle.setText(alertType);

        View headerView = dialogView.findViewById(org.zacsn.signal_dectect.R.id.dialog_header);
        headerView.setBackgroundColor(android.graphics.Color.parseColor("#991B1B"));

        String info = alertReason + "\n\n"
                + "设备名称: " + safeText(device.getDeviceName()) + "\n"
                + "MAC 地址: " + safeText(device.getMacAddress()) + "\n"
                + "信号强度: " + device.getSignalStrength() + " dBm\n"
                + "确认厂商: " + safeText(device.getManufacturer()) + "\n"
                + "候选线索: " + safeText(device.getCandidateManufacturer()) + "\n"
                + "判定等级: " + device.getManufacturerVerdict().name() + "\n"
                + "线索来源: " + safeText(device.getManufacturerSource()) + "\n"
                + "证据摘要: " + safeText(device.getManufacturerEvidence()) + "\n"
                + "设备类型: " + device.getDeviceType().name();
        tvMessage.setText(info);

        btnNegative.setText("确认");
        btnPositive.setText("查看详情");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnNegative.setOnClickListener(v -> {
            dialog.dismiss();
            restoreNormalScanSoundAfterAlert();
        });
        btnPositive.setOnClickListener(v -> {
            dialog.dismiss();
            restoreNormalScanSoundAfterAlert();
            openDeviceDetail(device);
        });

        dialog.show();
        resizeAlertDialog(dialog, dialogView);
    }

    private void restoreNormalScanSoundAfterAlert() {
        hasActiveConfiguredAlert = false;
        if (isScanning && soundEffectManager != null && soundEffectManager.isAlertMode()) {
            boolean soundStarted = soundEffectManager.startNormalScanSound();
            if (!soundStarted) {
                android.widget.Toast.makeText(this, "巡检音效恢复失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void resizeAlertDialog(AlertDialog dialog, View dialogView) {
        if (dialog.getWindow() == null) {
            return;
        }

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

    private void addKeyword(java.util.Set<String> keywords, String value) {
        String normalized = safeLower(value).trim();
        if (!normalized.isEmpty()) {
            keywords.add(normalized);
        }
    }

    private void addMac(java.util.Set<String> macs, String value) {
        String normalized = normalizeMac(value);
        if (!normalized.isEmpty()) {
            macs.add(normalized);
        }
    }

    private String normalizeMac(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("-", ":").toUpperCase(java.util.Locale.US);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.US);
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value;
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
        record.setName(recordName);
        
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
