package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import org.zacsn.signal_dectect.R;
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
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
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
        String displayDeviceName = deviceName != null && !deviceName.isEmpty() ? deviceName : "Unknown Device";
        binding.tvDeviceName.setText(displayDeviceName);
        binding.layoutDeviceName.setOnClickListener(v -> showFullDeviceNameDialog(
                isWifi ? "完整 SSID" : "完整广播名称",
                displayDeviceName
        ));
        
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

    private void showFullDeviceNameDialog(String title, String deviceName) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(deviceName != null && !deviceName.trim().isEmpty() ? deviceName : "未知")
                .setPositiveButton("确定", null)
                .show();
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
            case "ble_apple_audio":
                return "Apple音频设备画像";
            case "ble_apple_findmy":
                return "Apple定位设备画像";
            case "ble_apple_handoff":
                return "Apple Handoff画像";
            case "ble_apple_nearby":
                return "Apple Nearby画像";
            case "ble_apple_protocol":
                return "Apple生态协议";
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
            return "未知厂商: 暂无明确厂商线索";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getManufacturerVerdictLabel(verdict))
                .append(": ")
                .append(displayManufacturer);
        if (manufacturerConfidence > 0) {
            if (verdict == ManufacturerVerdict.POSSIBLE) {
                builder.append(" / 线索可信度 ").append(manufacturerConfidence).append("%");
            } else if (verdict != ManufacturerVerdict.UNKNOWN) {
                builder.append(" / ").append(manufacturerConfidence).append("%");
            }
        }

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
        boolean hasDisplayManufacturer = isUsefulManufacturer(displayManufacturer);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_manufacturer_detail, null);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_manufacturer_dialog_subtitle);
        TextView tvVerdict = dialogView.findViewById(R.id.tv_manufacturer_dialog_verdict);
        TextView tvConfidence = dialogView.findViewById(R.id.tv_manufacturer_dialog_confidence);
        TextView tvDisplay = dialogView.findViewById(R.id.tv_manufacturer_dialog_display);
        TextView tvSource = dialogView.findViewById(R.id.tv_manufacturer_dialog_source);
        TextView tvConfirmed = dialogView.findViewById(R.id.tv_manufacturer_dialog_confirmed);
        TextView tvCandidate = dialogView.findViewById(R.id.tv_manufacturer_dialog_candidate);
        TextView tvEvidence = dialogView.findViewById(R.id.tv_manufacturer_dialog_evidence);
        Button btnOk = dialogView.findViewById(R.id.btn_manufacturer_dialog_ok);

        tvSubtitle.setText(getManufacturerDialogSubtitle(verdict));
        tvVerdict.setText(getManufacturerVerdictLabel(verdict));
        tvConfidence.setText(buildConfidenceText(verdict, manufacturerConfidence, hasDisplayManufacturer));
        tvDisplay.setText(hasDisplayManufacturer ? displayManufacturer : "未知厂商");
        tvSource.setText("来源：" + getManufacturerSourceLabel(manufacturerSource));
        tvConfirmed.setText("确认厂商：" + (isUsefulManufacturer(manufacturer) ? manufacturer : "未确认"));
        tvCandidate.setText("候选线索：" + (isUsefulManufacturer(candidateManufacturer) ? candidateManufacturer : "无"));
        tvEvidence.setText(
                manufacturerEvidence != null && !manufacturerEvidence.trim().isEmpty()
                        ? manufacturerEvidence.trim()
                        : "暂无证据摘要"
        );
        applyVerdictStyle(dialogView, verdict);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        btnOk.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        resizeManufacturerDialog(dialog, dialogView);
    }

    private String buildConfidenceText(ManufacturerVerdict verdict, int confidence, boolean hasDisplayManufacturer) {
        if (!hasDisplayManufacturer || confidence <= 0) {
            return "无明确厂商时不展示百分比";
        }
        return (verdict == ManufacturerVerdict.POSSIBLE ? "线索可信度 " : "置信度 ") + confidence + "%";
    }

    private String getManufacturerDialogSubtitle(ManufacturerVerdict verdict) {
        switch (verdict) {
            case CONFIRMED:
                return "已获得较强厂商证据";
            case LIKELY:
                return "存在高可信厂商线索";
            case POSSIBLE:
                return "仅作为排查候选线索";
            default:
                return "暂无明确厂商证据";
        }
    }

    private void applyVerdictStyle(View dialogView, ManufacturerVerdict verdict) {
        TextView tvVerdict = dialogView.findViewById(R.id.tv_manufacturer_dialog_verdict);
        TextView tvConfidence = dialogView.findViewById(R.id.tv_manufacturer_dialog_confidence);
        int color;
        switch (verdict) {
            case CONFIRMED:
                color = ContextCompat.getColor(this, R.color.success);
                break;
            case LIKELY:
                color = ContextCompat.getColor(this, R.color.primary_variant);
                break;
            case POSSIBLE:
                color = ContextCompat.getColor(this, R.color.warning);
                break;
            default:
                color = ContextCompat.getColor(this, R.color.text_secondary);
                break;
        }
        tvVerdict.setTextColor(color);
        tvConfidence.setTextColor(color);
    }

    private void resizeManufacturerDialog(AlertDialog dialog, View dialogView) {
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

        View scroll = dialogView.findViewById(R.id.manufacturer_detail_scroll);
        int maxHeight = (int) (displayMetrics.heightPixels * 0.48);
        scroll.post(() -> {
            if (scroll.getMeasuredHeight() > maxHeight) {
                android.view.ViewGroup.LayoutParams lp = scroll.getLayoutParams();
                lp.height = maxHeight;
                scroll.setLayoutParams(lp);
            }
        });
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
