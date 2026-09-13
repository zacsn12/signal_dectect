package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.zacsn.signal_dectect.data.database.ScanRecordDao;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import org.zacsn.signal_dectect.databinding.ActivityScanRecordDetailBinding;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.presentation.adapter.SignalDeviceAdapter;
import org.zacsn.signal_dectect.util.GsonFactory;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@AndroidEntryPoint
public class ScanRecordDetailActivity extends AppCompatActivity {
    
    private ActivityScanRecordDetailBinding binding;
    private SignalDeviceAdapter adapter;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    
    @Inject
    ScanRecordDao scanRecordDao;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScanRecordDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Setup toolbar (don't call setSupportActionBar, use the toolbar directly)
        binding.toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle("扫描记录详情");
        
        // Get record ID from intent
        long recordId = getIntent().getLongExtra("RECORD_ID", -1);
        if (recordId == -1) {
            finish();
            return;
        }
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Load record details
        loadRecordDetails(recordId);
    }
    
    private void setupRecyclerView() {
        adapter = new SignalDeviceAdapter(device -> {
            // Open device detail
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
        });
        binding.recyclerViewDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerViewDevices.setAdapter(adapter);
    }

    private void loadRecordDetails(long recordId) {
        new Thread(() -> {
            try {
                ScanRecordEntity record = scanRecordDao.getRecordById(recordId);
                if (record == null) {
                    android.util.Log.e("ScanRecordDetail", "Record not found: " + recordId);
                    runOnUiThread(this::finish);
                    return;
                }
                
                android.util.Log.d("ScanRecordDetail", "Loading record: " + recordId);
                android.util.Log.d("ScanRecordDetail", "Devices JSON length: " + 
                        (record.getDevicesJson() != null ? record.getDevicesJson().length() : 0));
                
                List<SignalDevice> devices = org.zacsn.signal_dectect.domain.model.EntityMappers.parseDevicesJson(
                        record.getDevicesJson(),
                        record.getTimestamp()
                );
                
                final List<SignalDevice> finalDevices = devices;
                final int displayCount = Math.max(record.getDeviceCount(), finalDevices.size());
                
                runOnUiThread(() -> {
                    try {
                        if (record.getName() != null && !record.getName().trim().isEmpty()) {
                            binding.toolbar.setTitle(record.getName());
                        }
                        
                        String scanTypeName = getScanTypeName(record.getScanType());
                        binding.tvScanType.setText("扫描类型: " + scanTypeName);
                        
                        String timeStr = dateFormat.format(new Date(record.getTimestamp()));
                        binding.tvScanTime.setText("扫描时间: " + timeStr);
                        
                        binding.tvDuration.setText("扫描时长: " + record.getDuration() + " 秒");
                        binding.tvDeviceCount.setText("设备总数: " + displayCount + " 台");
                        
                        if (finalDevices.isEmpty()) {
                            binding.tvEmptyDevices.setVisibility(android.view.View.VISIBLE);
                            binding.recyclerViewDevices.setVisibility(android.view.View.GONE);
                        } else {
                            binding.tvEmptyDevices.setVisibility(android.view.View.GONE);
                            binding.recyclerViewDevices.setVisibility(android.view.View.VISIBLE);
                        }
                        
                        adapter.submitList(new ArrayList<>(finalDevices));
                    } catch (Exception e) {
                        android.util.Log.e("ScanRecordDetail", "Error updating UI", e);
                        android.widget.Toast.makeText(ScanRecordDetailActivity.this, 
                                "加载记录失败", android.widget.Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("ScanRecordDetail", "Error loading record", e);
                runOnUiThread(() -> {
                    android.widget.Toast.makeText(ScanRecordDetailActivity.this, 
                            "加载记录失败", android.widget.Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
    
    private String getScanTypeName(int scanType) {
        switch (scanType) {
            case 1:
                return "蓝牙巡检";
            case 2:
                return "WiFi巡检";
            case 3:
                return "蓝牙+WiFi";
            case 4:
                return "蜂窝巡检";
            case 7:
                return "全部扫描";
            default:
                return "扫描记录";
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
