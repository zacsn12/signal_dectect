package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
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
import org.zacsn.signal_dectect.domain.alert.AlertConfig;
import org.zacsn.signal_dectect.domain.alert.AlertMatch;
import org.zacsn.signal_dectect.domain.alert.AlertRuleMatcher;
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
    private static final String PREFS_NAME = "signal_inspect_settings";
    private static final String KEY_SELECTED_DISTANCE_METERS = "selected_distance_meters";
    private static final float DEFAULT_SELECTED_DISTANCE_METERS = 5.0f;
    
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
    private java.util.Set<String> watchlistMacs = new java.util.HashSet<>();
    private java.util.Set<String> watchlistBrands = new java.util.HashSet<>();
    private java.util.Set<String> whitelistMacs = new java.util.HashSet<>();
    private java.util.Set<String> blacklistMacs = new java.util.HashSet<>();
    private AlertConfig alertConfig = new AlertConfig(
            watchlistKeywords,
            watchlistMacs,
            watchlistBrands,
            whitelistMacs,
            blacklistMacs
    );
    private java.util.List<SignalDevice> latestVisibleDevices = new java.util.ArrayList<>();
    private java.util.List<SignalDevice> currentDevices = new java.util.ArrayList<>();
    private long scanStartTime = 0;
    private double selectedDistanceMeters = 0.0;
    private volatile boolean isSavingRecord = false;
    
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
        selectedDistanceMeters = loadSelectedDistanceMeters();

        binding.btnDistance.setOnClickListener(v -> showDistanceSettingDialog());

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
        syncDistanceFilterToViewModel();
        
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

    private void showDistanceSettingDialog() {
        View dialogView = getLayoutInflater().inflate(
                org.zacsn.signal_dectect.R.layout.dialog_distance_setting,
                null
        );
        TextView tvCurrent = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_distance_current);
        TextView btnAll = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_distance_all);
        TextView btnNear = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_distance_near);
        TextView btnMid = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_distance_mid);
        TextView btnFar = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_distance_far);

        tvCurrent.setText("当前：" + getDistanceLabel(selectedDistanceMeters));
        bindDistanceOption(btnAll, selectedDistanceMeters == 0.0);
        bindDistanceOption(btnNear, selectedDistanceMeters == 5.0);
        bindDistanceOption(btnMid, selectedDistanceMeters == 15.0);
        bindDistanceOption(btnFar, selectedDistanceMeters == 100.0);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnAll.setOnClickListener(v -> applyDistanceFilter(dialog, 0.0));
        btnNear.setOnClickListener(v -> applyDistanceFilter(dialog, 5.0));
        btnMid.setOnClickListener(v -> applyDistanceFilter(dialog, 15.0));
        btnFar.setOnClickListener(v -> applyDistanceFilter(dialog, 100.0));

        dialog.show();
        resizeMaterialDialog(dialog);
    }

    private void bindDistanceOption(TextView view, boolean selected) {
        view.setBackgroundResource(selected
                ? org.zacsn.signal_dectect.R.drawable.bg_pill_blue
                : org.zacsn.signal_dectect.R.drawable.bg_pill_gray);
        view.setTextColor(androidx.core.content.ContextCompat.getColor(
                this,
                selected ? org.zacsn.signal_dectect.R.color.primary_variant
                        : org.zacsn.signal_dectect.R.color.text_secondary
        ));
    }

    private void applyDistanceFilter(AlertDialog dialog, double distanceMeters) {
        selectedDistanceMeters = distanceMeters;
        saveSelectedDistanceMeters(distanceMeters);
        syncDistanceFilterToViewModel();
        android.widget.Toast.makeText(
                this,
                "巡检距离: " + getDistanceLabel(distanceMeters),
                android.widget.Toast.LENGTH_SHORT
        ).show();
        dialog.dismiss();
    }

    private double loadSelectedDistanceMeters() {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getFloat(KEY_SELECTED_DISTANCE_METERS, DEFAULT_SELECTED_DISTANCE_METERS);
    }

    private void saveSelectedDistanceMeters(double distanceMeters) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putFloat(KEY_SELECTED_DISTANCE_METERS, (float) distanceMeters)
                .apply();
    }

    private void syncDistanceFilterToViewModel() {
        if (viewModel != null) {
            viewModel.filterByRange(selectedDistanceMeters);
        }
    }

    private String getDistanceLabel(double distanceMeters) {
        if (distanceMeters <= 0) {
            return "不限距离";
        }
        if (distanceMeters == 5.0) {
            return "近距 ~5米";
        }
        if (distanceMeters == 15.0) {
            return "中距 ~15米";
        }
        if (distanceMeters == 100.0) {
            return "远距 ~100米";
        }
        return "~" + (int) distanceMeters + "米";
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
                requestStartNewScan();
            }
        });
    }

    private void requestStartNewScan() {
        if (hasUnsavedScanResults()) {
            showStartNewScanConfirmDialog();
            return;
        }
        startNewScan();
    }

    private boolean hasUnsavedScanResults() {
        return currentDevices != null && !currentDevices.isEmpty();
    }

    private void showStartNewScanConfirmDialog() {
        showSavePromptDialog(
                "保存扫描记录",
                "开始新的巡检前，请处理当前结果",
                "当前扫描列表中已有设备结果。开始新的巡检会清空列表，请先选择是否保存。",
                "不保存",
                "保存后开始",
                this::startNewScanAfterDiscardingScanResults,
                this::startNewScanAfterSavingScanRecord
        );
    }

    private void showSavePromptDialog(
            String title,
            String subtitle,
            String message,
            String discardText,
            String saveText,
            Runnable onDiscard,
            Runnable onSave
    ) {
        View dialogView = getLayoutInflater().inflate(
                org.zacsn.signal_dectect.R.layout.dialog_save_scan_prompt,
                null
        );
        TextView tvTitle = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_save_prompt_title);
        TextView tvSubtitle = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_save_prompt_subtitle);
        TextView tvCount = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_save_prompt_count);
        TextView tvMessage = dialogView.findViewById(org.zacsn.signal_dectect.R.id.tv_save_prompt_message);
        Button btnCancel = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_save_prompt_cancel);
        Button btnDiscard = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_save_prompt_discard);
        Button btnSave = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_save_prompt_save);

        tvTitle.setText(title);
        tvSubtitle.setText(subtitle);
        tvCount.setText(currentDevices.size() + " 个");
        tvMessage.setText(message);
        btnDiscard.setText(discardText);
        btnSave.setText(saveText);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDiscard.setOnClickListener(v -> {
            dialog.dismiss();
            if (onDiscard != null) {
                onDiscard.run();
            }
        });
        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            if (onSave != null) {
                onSave.run();
            }
        });

        dialog.show();
        resizeMaterialDialog(dialog);
    }

    private void startNewScan() {
        hasActiveConfiguredAlert = false;
        alertedConfiguredDevices.clear();
        highlightedTargetDevices.clear();
        adapter.setHighlightedMacs(highlightedTargetDevices);
        scanStartTime = 0;
        syncDistanceFilterToViewModel();
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
            java.util.Set<String> nextMacs = new java.util.HashSet<>();
            java.util.Set<String> nextBrands = new java.util.HashSet<>();
            for (WatchlistItemEntity entity : entities) {
                String matchType = entity.getMatchType();
                String matchValue = firstUseful(entity.getMatchValue(), entity.getMacAddress());
                if ("MAC".equalsIgnoreCase(matchType)) {
                    AlertRuleMatcher.addMac(nextMacs, matchValue);
                } else if ("BRAND".equalsIgnoreCase(matchType)) {
                    AlertRuleMatcher.addBrand(nextBrands, matchValue);
                } else if ("KEYWORD".equalsIgnoreCase(matchType)) {
                    AlertRuleMatcher.addKeyword(nextKeywords, matchValue);
                } else {
                    AlertRuleMatcher.addMac(nextMacs, entity.getMacAddress());
                    AlertRuleMatcher.addBrand(nextBrands, entity.getManufacturer());
                    AlertRuleMatcher.addBrand(nextBrands, entity.getDeviceName());
                    AlertRuleMatcher.addKeyword(nextKeywords, entity.getManufacturer());
                    AlertRuleMatcher.addKeyword(nextKeywords, entity.getDeviceName());
                }
            }
            watchlistKeywords = nextKeywords;
            watchlistMacs = nextMacs;
            watchlistBrands = nextBrands;
            rebuildAlertConfig();
        });

        whitelistDao.getAll().observe(this, entities -> {
            java.util.Set<String> nextMacs = new java.util.HashSet<>();
            for (WhitelistItemEntity entity : entities) {
                AlertRuleMatcher.addMac(nextMacs, entity.getMacAddress());
            }
            whitelistMacs = nextMacs;
            rebuildAlertConfig();
        });

        blacklistDao.getAll().observe(this, entities -> {
            java.util.Set<String> nextMacs = new java.util.HashSet<>();
            for (BlacklistItemEntity entity : entities) {
                AlertRuleMatcher.addMac(nextMacs, entity.getMacAddress());
            }
            blacklistMacs = nextMacs;
            rebuildAlertConfig();
        });
    }

    private void rebuildAlertConfig() {
        alertConfig = new AlertConfig(
                watchlistKeywords,
                watchlistMacs,
                watchlistBrands,
                whitelistMacs,
                blacklistMacs
        );
    }

    private java.util.List<SignalDevice> refreshDeviceList() {
        java.util.List<SignalDevice> sortedDevices = new java.util.ArrayList<>(latestVisibleDevices);
        sortedDevices.sort((d1, d2) -> {
            int targetPriority1 = highlightedTargetDevices.contains(AlertRuleMatcher.normalizeMac(d1.getMacAddress())) ? 0 : 1;
            int targetPriority2 = highlightedTargetDevices.contains(AlertRuleMatcher.normalizeMac(d2.getMacAddress())) ? 0 : 1;
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
            AlertMatch match = AlertRuleMatcher.match(device, alertConfig);
            if (match == null) {
                continue;
            }

            if (alertedConfiguredDevices.contains(match.getDedupKey())) {
                continue;
            }

            alertedConfiguredDevices.add(match.getDedupKey());
            highlightedTargetDevices.add(match.getNormalizedMac());
            adapter.setHighlightedMacs(highlightedTargetDevices);
            refreshDeviceList();
            if (!hasActiveConfiguredAlert) {
                hasActiveConfiguredAlert = true;
                boolean soundStarted = soundEffectManager.switchToAlertSound();
                if (!soundStarted) {
                    android.widget.Toast.makeText(this, "告警音效启动失败，请检查媒体音量", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            showConfiguredAlert(device, match.getAlertType(), match.getAlertReason());
        }
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
                + "确认厂商: " + safeManufacturer(device.getManufacturer(), "未确认") + "\n"
                + "候选线索: " + safeManufacturer(device.getCandidateManufacturer(), "无") + "\n"
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

    private void resizeMaterialDialog(AlertDialog dialog) {
        if (dialog.getWindow() == null) {
            return;
        }
        dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        android.view.WindowManager.LayoutParams layoutParams = new android.view.WindowManager.LayoutParams();
        layoutParams.copyFrom(dialog.getWindow().getAttributes());
        layoutParams.width = (int) (displayMetrics.widthPixels * 0.88);
        dialog.getWindow().setAttributes(layoutParams);
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value;
    }

    private String firstUseful(String first, String fallback) {
        return first != null && !first.trim().isEmpty() ? first.trim() : fallback;
    }

    private String safeManufacturer(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        String trimmed = value.trim();
        if ("未知".equals(trimmed) || "未知厂商".equals(trimmed) || "未确认".equals(trimmed)
                || "随机地址".equals(trimmed)) {
            return fallback;
        }
        return trimmed;
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
        showSavePromptDialog(
                "保存扫描记录",
                "退出巡检页面前，请处理当前结果",
                "当前扫描列表中已有设备结果。直接退出不会保留本次扫描记录，请选择是否保存。",
                "不保存退出",
                "保存并退出",
                this::finishAfterDiscardingScanResults,
                this::finishAfterSavingScanRecord
        );
    }
    
    /**
     * Show dialog to input record name.
     */
    private void showSaveRecordNameDialog() {
        showSaveRecordNameDialog(this::finish, this::finish);
    }

    private void showSaveRecordNameDialog(Runnable afterSave, Runnable onCancel) {
        View dialogView = getLayoutInflater().inflate(
                org.zacsn.signal_dectect.R.layout.dialog_save_record_name,
                null
        );
        EditText etName = dialogView.findViewById(org.zacsn.signal_dectect.R.id.et_record_name);
        Button btnCancel = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_record_name_cancel);
        Button btnConfirm = dialogView.findViewById(org.zacsn.signal_dectect.R.id.btn_record_name_confirm);

        // Generate default name
        String defaultName = getScanTypeName() + " - " + 
                new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date());
        etName.setText(defaultName);
        if (defaultName != null) {
            etName.setSelection(defaultName.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            if (onCancel != null) {
                onCancel.run();
            }
        });

        btnConfirm.setOnClickListener(v -> {
            String recordName = etName.getText().toString().trim();
            if (recordName.isEmpty()) {
                recordName = defaultName;
            }
            dialog.dismiss();
            saveRecord(recordName, afterSave);
        });

        dialog.show();
        resizeMaterialDialog(dialog);
    }
    
    /**
     * Save scan record to database.
     */
    private void saveRecord(String recordName) {
        saveRecord(recordName, null);
    }

    private void saveRecord(String recordName, Runnable afterSave) {
        if (isSavingRecord) {
            Toast.makeText(this, "记录正在保存，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }

        isSavingRecord = true;
        if (isScanning) {
            soundEffectManager.stopAllSounds();
            viewModel.stopScan();
        }

        long timestamp = System.currentTimeMillis();
        int scanTypeInt = getScanTypeInt();
        long durationSeconds = scanStartTime > 0 ? (timestamp - scanStartTime) / 1000 : 0;
        java.util.List<SignalDevice> devicesSnapshot = new java.util.ArrayList<>(currentDevices);

        new Thread(() -> {
            try {
                Gson gson = new Gson();
                String devicesJson = gson.toJson(devicesSnapshot);

                ScanRecordEntity record = new ScanRecordEntity(
                        timestamp,
                        scanTypeInt,
                        durationSeconds,
                        null,  // latitude
                        null,  // longitude
                        devicesSnapshot.size(),
                        devicesJson
                );
                record.setName(recordName);

                long insertedId = scanRecordDao.insertRecord(record);
                Log.i("SignalInspectActivity", "Scan record saved, id=" + insertedId
                        + ", deviceCount=" + devicesSnapshot.size());

                runOnUiThreadIfAlive(() -> {
                    isSavingRecord = false;
                    Toast.makeText(this, "记录已保存", Toast.LENGTH_SHORT).show();
                    currentDevices = new java.util.ArrayList<>();
                    latestVisibleDevices = new java.util.ArrayList<>();
                    if (afterSave != null) {
                        afterSave.run();
                    }
                });
            } catch (Exception e) {
                Log.e("SignalInspectActivity", "Failed to save scan record", e);
                runOnUiThreadIfAlive(() -> {
                    isSavingRecord = false;
                    Toast.makeText(this, "记录保存失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void finishAfterDiscardingScanResults() {
        currentDevices = new java.util.ArrayList<>();
        latestVisibleDevices = new java.util.ArrayList<>();
        if (isScanning) {
            viewModel.stopScan();
            soundEffectManager.stopAllSounds();
        }
        finish();
    }

    private void finishAfterSavingScanRecord() {
        if (isSavingRecord) {
            Toast.makeText(this, "记录正在保存，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        showSaveRecordNameDialog(this::finish, null);
    }

    private void startNewScanAfterDiscardingScanResults() {
        currentDevices = new java.util.ArrayList<>();
        latestVisibleDevices = new java.util.ArrayList<>();
        startNewScan();
    }

    private void startNewScanAfterSavingScanRecord() {
        if (isSavingRecord) {
            Toast.makeText(this, "记录正在保存，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        showSaveRecordNameDialog(this::startNewScan, null);
    }

    private void runOnUiThreadIfAlive(Runnable runnable) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(runnable);
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
