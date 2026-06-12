package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import org.zacsn.signal_dectect.databinding.ActivityDeviceDetailBinding;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.util.DistanceUtils;
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
        
        // Set custom toolbar
        setSupportActionBar(binding.toolbar);
        
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
        String candidateManufacturer = getIntent().getStringExtra("CANDIDATE_MANUFACTURER");
        String manufacturerSource = getIntent().getStringExtra("MANUFACTURER_SOURCE");
        int manufacturerConfidence = getIntent().getIntExtra("MANUFACTURER_CONFIDENCE", 0);
        String manufacturerVerdict = getIntent().getStringExtra("MANUFACTURER_VERDICT");
        String manufacturerEvidence = getIntent().getStringExtra("MANUFACTURER_EVIDENCE");
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
        String candidateManufacturer = getIntent().getStringExtra("CANDIDATE_MANUFACTURER");
        String manufacturerSource = getIntent().getStringExtra("MANUFACTURER_SOURCE");
        int manufacturerConfidence = getIntent().getIntExtra("MANUFACTURER_CONFIDENCE", 0);
        String manufacturerVerdict = getIntent().getStringExtra("MANUFACTURER_VERDICT");
        String manufacturerEvidence = getIntent().getStringExtra("MANUFACTURER_EVIDENCE");
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
            binding.labelDeviceName.setText("广播名称:");
        }
        binding.tvDeviceName.setText(deviceName != null && !deviceName.isEmpty() ? deviceName : "Unknown Device");
        
        binding.tvDistance.setText(DistanceUtils.formatMetersChinese(distance));
        binding.tvSignalStrengthValue.setText(signalStrength + " dBm");
        
        String vendor = buildManufacturerSummary(
                manufacturer,
                candidateManufacturer,
                manufacturerSource,
                manufacturerConfidence,
                manufacturerVerdict,
                manufacturerEvidence
        );
        binding.tvManufacturer.setText(vendor);
        binding.layoutManufacturer.setOnClickListener(v -> showManufacturerDetailDialog(
                manufacturer,
                candidateManufacturer,
                manufacturerSource,
                manufacturerConfidence,
                manufacturerVerdict,
                manufacturerEvidence
        ));
        
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

    private String getManufacturerSourceLabel(String source) {
        if (source == null) {
            return "来源未知";
        }

        switch (source) {
            case "mac_oui":
                return "MAC厂商库";
            case "ble_mac_oui":
                return "BLE MAC厂商库";
            case "classic_mac_oui":
                return "经典蓝牙MAC厂商库";
            case "classic_mac_name_match":
                return "经典蓝牙MAC+名称一致";
            case "wifi_bssid_oui":
                return "WiFi BSSID厂商库";
            case "wifi_ssid_name":
                return "WiFi SSID品牌线索";
            case "wifi_bssid_ssid_match":
                return "WiFi BSSID+SSID一致";
            case "wifi_randomized_bssid":
                return "WiFi随机BSSID";
            case "ble_company_id":
                return "BLE广播厂商ID";
            case "device_name":
                return "设备名称";
            case "gatt_device_info":
                return "GATT设备信息";
            case "gatt_pnp_id":
                return "GATT PnP ID";
            case "gatt_device_info+pnp_id":
                return "GATT设备信息+PnP ID";
            case "gatt_device_info_conflict":
                return "GATT信息存在冲突";
            case "cellular_operator":
                return "运营商";
            default:
                return "来源未知";
        }
    }

    private String buildManufacturerSummary(
            String manufacturer,
            String candidateManufacturer,
            String manufacturerSource,
            int manufacturerConfidence,
            String verdictName,
            String manufacturerEvidence
    ) {
        ManufacturerVerdict verdict = parseManufacturerVerdict(verdictName, manufacturerSource, manufacturerConfidence);
        String displayManufacturer = isUsefulManufacturer(manufacturer) ? manufacturer : candidateManufacturer;
        if (!isUsefulManufacturer(displayManufacturer)) {
            displayManufacturer = "未知厂商";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getManufacturerVerdictLabel(verdict))
                .append(": ")
                .append(displayManufacturer)
                .append(" / ")
                .append(manufacturerConfidence)
                .append("%");

        return builder.toString();
    }

    private void showManufacturerDetailDialog(
            String manufacturer,
            String candidateManufacturer,
            String manufacturerSource,
            int manufacturerConfidence,
            String verdictName,
            String manufacturerEvidence
    ) {
        ManufacturerVerdict verdict = parseManufacturerVerdict(verdictName, manufacturerSource, manufacturerConfidence);
        String displayManufacturer = isUsefulManufacturer(manufacturer) ? manufacturer : candidateManufacturer;
        if (!isUsefulManufacturer(displayManufacturer)) {
            displayManufacturer = "未知厂商";
        }

        StringBuilder message = new StringBuilder();
        message.append("判定结果: ").append(getManufacturerVerdictLabel(verdict)).append("\n")
                .append("显示厂商: ").append(displayManufacturer).append("\n")
                .append("确认厂商: ").append(isUsefulManufacturer(manufacturer) ? manufacturer : "未确认").append("\n")
                .append("候选线索: ").append(isUsefulManufacturer(candidateManufacturer) ? candidateManufacturer : "无").append("\n")
                .append("置信度: ").append(manufacturerConfidence).append("%\n")
                .append("来源: ").append(getManufacturerSourceLabel(manufacturerSource));

        if (manufacturerEvidence != null && !manufacturerEvidence.trim().isEmpty()) {
            message.append("\n\n证据摘要:\n").append(manufacturerEvidence.trim());
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("厂商识别详情")
                .setMessage(message.toString())
                .setPositiveButton("知道了", null)
                .show();
    }

    private ManufacturerVerdict parseManufacturerVerdict(String verdictName, String source, int confidence) {
        if (verdictName != null) {
            try {
                return ManufacturerVerdict.valueOf(verdictName);
            } catch (IllegalArgumentException ignored) {
                // Fall through to legacy inference.
            }
        }
        if (confidence >= 95 || "gatt_device_info".equals(source) || "gatt_pnp_id".equals(source)
                || "gatt_device_info+pnp_id".equals(source)
                || "wifi_bssid_ssid_match".equals(source)) {
            return ManufacturerVerdict.CONFIRMED;
        }
        if (confidence >= 80) {
            return ManufacturerVerdict.LIKELY;
        }
        if (confidence > 0) {
            return ManufacturerVerdict.POSSIBLE;
        }
        return ManufacturerVerdict.UNKNOWN;
    }

    private String getManufacturerVerdictLabel(ManufacturerVerdict verdict) {
        switch (verdict) {
            case CONFIRMED:
                return "已确认厂商";
            case LIKELY:
                return "高可信疑似";
            case POSSIBLE:
                return "候选线索";
            default:
                return "未知厂商";
        }
    }

    private boolean isUsefulManufacturer(String manufacturer) {
        return manufacturer != null
                && !manufacturer.trim().isEmpty()
                && !"未知".equals(manufacturer)
                && !"未知厂商".equals(manufacturer)
                && !"未确认".equals(manufacturer)
                && !"随机地址".equals(manufacturer);
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
