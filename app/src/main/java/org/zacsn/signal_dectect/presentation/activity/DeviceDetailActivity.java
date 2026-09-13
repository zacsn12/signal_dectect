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
        
        // Load and display device data
        displayDeviceInfo();
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
        
        boolean isCellular = deviceType != null && deviceType.equals("CELLULAR");
        boolean isWifi = deviceType != null && deviceType.equals("WIFI");
        boolean isBluetooth = deviceType != null && 
            (deviceType.equals("BLUETOOTH_LE") || deviceType.equals("BLUETOOTH_CLASSIC"));
        
        if (getSupportActionBar() != null) {
            if (isCellular) {
                getSupportActionBar().setTitle("基站详情");
            } else if (isWifi) {
                getSupportActionBar().setTitle("WiFi详情");
            } else if (isBluetooth) {
                getSupportActionBar().setTitle("蓝牙详情");
            } else {
                getSupportActionBar().setTitle("设备详情");
            }
        }
        
        if (isCellular) {
            displayCellularDeviceInfo(
                    macAddress,
                    deviceName,
                    candidateManufacturer,
                    manufacturerEvidence,
                    signalStrength,
                    firstSeen,
                    lastSeen
            );
        } else {
            displayStandardDeviceInfo(
                    macAddress,
                    deviceName,
                    deviceType,
                    manufacturer,
                    candidateManufacturer,
                    manufacturerSource,
                    manufacturerConfidence,
                    manufacturerVerdict,
                    manufacturerEvidence,
                    signalStrength,
                    frequency,
                    distance,
                    firstSeen,
                    lastSeen,
                    isWifi,
                    isBluetooth
            );
        }
    }

    private void displayCellularDeviceInfo(
            String macAddress,
            String deviceName,
            String candidateManufacturer,
            String manufacturerEvidence,
            int signalStrength,
            long firstSeen,
            long lastSeen
    ) {
        // Toggle containers
        binding.layoutCellularContainer.setVisibility(View.VISIBLE);
        binding.layoutStandardContainer.setVisibility(View.GONE);

        // Parse structured cellular parameters
        CellularInfo info = CellularInfo.parse(
                manufacturerEvidence,
                deviceName,
                macAddress,
                candidateManufacturer,
                signalStrength
        );

        // Top Signal Gauge
        binding.tvSignalStrength.setText(signalStrength + " dBm");
        binding.signalGauge.setProgress(convertCellularDbmToProgress(signalStrength));
        binding.tvSignalQuality.setText(getCellularSignalQuality(signalStrength));
        binding.tvSignalQuality.setTextColor(getCellularSignalQualityColor(signalStrength));

        // Serving status badge below gauge
        binding.tvServingBadge.setVisibility(View.VISIBLE);
        if (info.isServing) {
            binding.tvServingBadge.setText("当前服务基站 (Serving)");
            binding.tvServingBadge.setBackgroundResource(R.drawable.bg_pill_green);
            binding.tvServingBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else if (!info.cellId.equals("未知") || !info.pci.equals("未知")) {
            binding.tvServingBadge.setText("邻近基站 (Neighbor)");
            binding.tvServingBadge.setBackgroundResource(R.drawable.bg_pill_blue);
            binding.tvServingBadge.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else {
            binding.tvServingBadge.setText("蜂窝网络服务");
            binding.tvServingBadge.setBackgroundResource(R.drawable.bg_pill_gray);
            binding.tvServingBadge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }

        // Card 1: Network Overview
        binding.tvCellularOperator.setText(formatOperatorDisplayName(info.operator));
        bindOperatorBadgeStyle(info.operator, info.plmn);

        binding.tvCellularPlmn.setText(formatPlmnDisplayName(info.plmn));
        binding.tvCellularRadioType.setText(info.radioType);
        binding.tvCellularServingStatus.setText(
                info.isServing ? "当前主服小区 (已连接/注册)" : "邻近监听基站 (未注册)"
        );
        binding.tvCellularServingStatus.setTextColor(
                info.isServing ? ContextCompat.getColor(this, R.color.success)
                        : ContextCompat.getColor(this, R.color.text_secondary)
        );

        // Card 2: Cell Identity & RF Parameters
        updateCellularLabelsByRadioType(info.radioType);
        binding.tvCellularCellId.setText(info.cellId);
        binding.tvCellularPci.setText(info.pci);
        binding.tvCellularTac.setText(info.tac);
        binding.tvCellularChannel.setText(info.channel);
        binding.tvCellularSignalDbm.setText(signalStrength + " dBm (" + getCellularSignalQuality(signalStrength) + ")");

        // Card 3: Timestamps & Evidence
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        binding.tvCellularFirstSeen.setText(firstSeen > 0 ? sdf.format(new Date(firstSeen)) : "未知");
        binding.tvCellularLastSeen.setText(lastSeen > 0 ? sdf.format(new Date(lastSeen)) : "未知");

        if (info.rawEvidence != null && !info.rawEvidence.isEmpty()) {
            binding.tvCellularEvidence.setText("点击查看详细特征");
            binding.layoutCellularEvidence.setOnClickListener(v -> showFullEvidenceDialog(info));
        } else {
            binding.tvCellularEvidence.setText("暂无额外特征");
            binding.layoutCellularEvidence.setOnClickListener(null);
        }
    }

    private void updateCellularLabelsByRadioType(String radioType) {
        if (radioType == null) return;
        if (radioType.contains("5G")) {
            binding.labelCellularCellId.setText("小区标识 (5G NCI)");
            binding.labelCellularPci.setText("物理小区标识 (5G PCI)");
            binding.labelCellularTac.setText("跟踪区码 (5G TAC)");
            binding.labelCellularChannel.setText("载波频点 (NRARFCN)");
        } else if (radioType.contains("4G") || radioType.contains("LTE")) {
            binding.labelCellularCellId.setText("小区标识 (4G CI)");
            binding.labelCellularPci.setText("物理小区标识 (4G PCI)");
            binding.labelCellularTac.setText("跟踪区码 (4G TAC)");
            binding.labelCellularChannel.setText("载波频点 (EARFCN)");
        } else if (radioType.contains("3G") || radioType.contains("2G") || radioType.contains("GSM") || radioType.contains("WCDMA")) {
            binding.labelCellularCellId.setText("小区标识 (CID)");
            binding.labelCellularPci.setText("扰码/物理标识 (PSC)");
            binding.labelCellularTac.setText("位置区码 (LAC)");
            binding.labelCellularChannel.setText("载波频点 (ARFCN)");
        } else if (radioType.contains("CDMA")) {
            binding.labelCellularCellId.setText("基站标识 (BaseStation ID)");
            binding.labelCellularPci.setText("系统标识 (System ID)");
            binding.labelCellularTac.setText("网络标识 (Network ID)");
            binding.labelCellularChannel.setText("频点通道 (Channel)");
        }
    }

    private String formatOperatorDisplayName(String operator) {
        if (operator == null || operator.trim().isEmpty() || "未知运营商".equals(operator.trim())) {
            return "未知运营商";
        }
        if (operator.contains("移动")) return "中国移动";
        if (operator.contains("联通")) return "中国联通";
        if (operator.contains("电信")) return "中国电信";
        if (operator.contains("广电")) return "中国广电";
        return operator;
    }

    private void bindOperatorBadgeStyle(String operator, String plmn) {
        if (operator == null) operator = "";
        if (plmn == null) plmn = "";
        
        if (operator.contains("移动") || plmn.startsWith("46000") || plmn.startsWith("46002") || plmn.startsWith("46007") || plmn.startsWith("46008")) {
            binding.tvCellularOperator.setBackgroundResource(R.drawable.bg_pill_blue);
            binding.tvCellularOperator.setTextColor(ContextCompat.getColor(this, R.color.primary));
        } else if (operator.contains("电信") || plmn.startsWith("46003") || plmn.startsWith("46005") || plmn.startsWith("46011")) {
            binding.tvCellularOperator.setBackgroundResource(R.drawable.bg_pill_blue);
            binding.tvCellularOperator.setTextColor(ContextCompat.getColor(this, R.color.secondary));
        } else if (operator.contains("联通") || plmn.startsWith("46001") || plmn.startsWith("46006") || plmn.startsWith("46009")) {
            binding.tvCellularOperator.setBackgroundResource(R.drawable.bg_pill_orange);
            binding.tvCellularOperator.setTextColor(ContextCompat.getColor(this, R.color.warning));
        } else if (operator.contains("广电") || plmn.startsWith("46015")) {
            binding.tvCellularOperator.setBackgroundResource(R.drawable.bg_pill_green);
            binding.tvCellularOperator.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            binding.tvCellularOperator.setBackgroundResource(R.drawable.bg_pill_gray);
            binding.tvCellularOperator.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        }
    }

    private String formatPlmnDisplayName(String plmn) {
        if (plmn == null || plmn.trim().isEmpty() || "未知".equals(plmn) || "UNKNOWN".equalsIgnoreCase(plmn)) {
            return "未知";
        }
        if (plmn.length() >= 5) {
            String mcc = plmn.substring(0, 3);
            String mnc = plmn.substring(3);
            return plmn + " (MCC:" + mcc + " MNC:" + mnc + ")";
        }
        return plmn;
    }

    private String getCellularSignalQuality(int dbm) {
        if (dbm >= -80) return "信号极佳";
        if (dbm >= -95) return "信号良好";
        if (dbm >= -105) return "信号中等";
        if (dbm >= -115) return "信号较弱";
        return "边缘覆盖 / 信号微弱";
    }

    private int getCellularSignalQualityColor(int dbm) {
        if (dbm >= -80) return ContextCompat.getColor(this, R.color.success);
        if (dbm >= -95) return 0xFF93C5FD; // light blue
        if (dbm >= -105) return ContextCompat.getColor(this, R.color.warning);
        return 0xFFFCA5A5; // light red
    }

    private int convertCellularDbmToProgress(int dbm) {
        // Typical cellular RSRP range: -130 dBm (weakest) to -50 dBm (strongest)
        if (dbm >= -50) return 100;
        if (dbm <= -130) return 0;
        return (int) ((dbm + 130) * 100.0 / 80.0);
    }

    private void showFullEvidenceDialog(CellularInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("【基站采样证据】\n\n");
        if (info.rawEvidence != null && !info.rawEvidence.isEmpty()) {
            String[] parts = info.rawEvidence.split(",");
            for (String part : parts) {
                sb.append("• ").append(part.trim()).append("\n");
            }
        } else {
            sb.append("暂无更多特征数据");
        }
        
        new AlertDialog.Builder(this)
                .setTitle("基站特征证据链")
                .setMessage(sb.toString().trim())
                .setPositiveButton("确定", null)
                .show();
    }

    private void displayStandardDeviceInfo(
            String macAddress,
            String deviceName,
            String deviceType,
            String manufacturer,
            String candidateManufacturer,
            String manufacturerSource,
            int manufacturerConfidence,
            String verdictName,
            String manufacturerEvidence,
            int signalStrength,
            int frequency,
            double distance,
            long firstSeen,
            long lastSeen,
            boolean isWifi,
            boolean isBluetooth
    ) {
        binding.layoutCellularContainer.setVisibility(View.GONE);
        binding.layoutStandardContainer.setVisibility(View.VISIBLE);
        binding.tvServingBadge.setVisibility(View.GONE);
        binding.tvSignalQuality.setText("信号强度");
        binding.tvSignalQuality.setTextColor(0xFF93C5FD);
        
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
                verdictName,
                manufacturerEvidence
        );
        binding.tvManufacturer.setText(vendor);
        binding.layoutManufacturer.setOnClickListener(v -> showManufacturerDetailDialog(
                manufacturer,
                candidateManufacturer,
                manufacturerSource,
                manufacturerConfidence,
                verdictName,
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

    /**
     * Data model and parser for cellular base station details.
     */
    public static class CellularInfo {
        public String operator = "未知运营商";
        public String plmn = "未知";
        public String radioType = "蜂窝网络";
        public boolean isServing = false;
        public String cellId = "未知";
        public String pci = "未知";
        public String tac = "未知";
        public String channel = "未知";
        public int dbm = 0;
        public String rawEvidence = "";

        public static CellularInfo parse(
                String evidence,
                String deviceName,
                String macAddress,
                String candidateManufacturer,
                int dbm
        ) {
            CellularInfo info = new CellularInfo();
            info.dbm = dbm;
            info.rawEvidence = evidence != null ? evidence.trim() : "";

            // 1. Initial operator candidate
            if (candidateManufacturer != null && !candidateManufacturer.trim().isEmpty()
                    && !"未确认".equals(candidateManufacturer.trim())) {
                info.operator = candidateManufacturer.trim();
            }

            // 2. Parse structured key-values from evidence string
            if (evidence != null && !evidence.trim().isEmpty()) {
                String[] parts = evidence.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.startsWith("运营商=")) {
                        String val = trimmed.substring("运营商=".length()).trim();
                        if (!val.isEmpty()) info.operator = val;
                    } else if (trimmed.startsWith("PLMN=")) {
                        String val = trimmed.substring("PLMN=".length()).trim();
                        if (!val.isEmpty() && !"UNKNOWN".equalsIgnoreCase(val)) info.plmn = val;
                    } else if (trimmed.startsWith("制式=") || trimmed.startsWith("网络制式=")) {
                        String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!val.isEmpty()) info.radioType = val;
                    } else if (trimmed.startsWith("当前服务小区=")) {
                        String val = trimmed.substring("当前服务小区=".length()).trim();
                        info.isServing = "是".equalsIgnoreCase(val) || "true".equalsIgnoreCase(val);
                    } else if (trimmed.startsWith("小区=")) {
                        String summary = trimmed.substring("小区=".length()).trim();
                        parseIdentitySummary(info, summary);
                    } else if (trimmed.startsWith("区域=") || trimmed.startsWith("TAC=") || trimmed.startsWith("LAC=")) {
                        String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.tac = val;
                    } else if (trimmed.startsWith("小区ID=") || trimmed.startsWith("CI=") || trimmed.startsWith("NCI=") || trimmed.startsWith("CID=")) {
                        String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.cellId = val;
                    } else if (trimmed.startsWith("PCI/PSC=") || trimmed.startsWith("PCI=") || trimmed.startsWith("PSC=")) {
                        String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.pci = val;
                    } else if (trimmed.startsWith("频点=") || trimmed.startsWith("ARFCN=") || trimmed.startsWith("EARFCN=") || trimmed.startsWith("NRARFCN=")) {
                        String val = trimmed.substring(trimmed.indexOf('=') + 1).trim();
                        if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.channel = val;
                    }
                }
            }

            // 3. Fallback extraction from deviceName
            if (deviceName != null && !deviceName.trim().isEmpty()) {
                String dName = deviceName.trim();
                if (dName.contains("当前服务小区")) {
                    info.isServing = true;
                }
                if ("蜂窝网络".equals(info.radioType)) {
                    if (dName.contains("5G NR") || dName.contains("5G")) info.radioType = "5G NR";
                    else if (dName.contains("4G LTE") || dName.contains("4G")) info.radioType = "4G LTE";
                    else if (dName.contains("3G")) info.radioType = "3G";
                    else if (dName.contains("2G")) info.radioType = "2G";
                }
            }

            // 4. Fallback extraction from synthesized deviceKey/macAddress
            if (macAddress != null && macAddress.startsWith("CELLULAR_CELL_")) {
                String[] segments = macAddress.split("_");
                if (segments.length >= 7) {
                    if (info.plmn.equals("未知") && !segments[2].equalsIgnoreCase("UNKNOWN")) {
                        info.plmn = segments[2];
                    }
                    int lastIdx = segments.length - 1;
                    if (info.pci.equals("未知") && !segments[lastIdx].equalsIgnoreCase("unknown")) {
                        info.pci = segments[lastIdx];
                    }
                    if (info.cellId.equals("未知") && !segments[lastIdx - 1].equalsIgnoreCase("unknown")) {
                        info.cellId = segments[lastIdx - 1];
                    }
                    if (info.tac.equals("未知") && !segments[lastIdx - 2].equalsIgnoreCase("unknown")) {
                        info.tac = segments[lastIdx - 2];
                    }
                }
            } else if (macAddress != null && macAddress.startsWith("CELLULAR_OPERATOR_")) {
                String[] segments = macAddress.split("_");
                if (segments.length >= 3 && info.plmn.equals("未知") && !segments[2].equalsIgnoreCase("UNKNOWN")) {
                    info.plmn = segments[2];
                }
            }

            return info;
        }

        private static void parseIdentitySummary(CellularInfo info, String summary) {
            if (summary == null || summary.isEmpty()) return;
            // e.g. "区域=41235, 小区ID=12345678, PCI/PSC=235, 频点=627264"
            String[] tokens = summary.split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.startsWith("区域=")) {
                    String val = trimmed.substring("区域=".length()).trim();
                    if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.tac = val;
                } else if (trimmed.startsWith("小区ID=")) {
                    String val = trimmed.substring("小区ID=".length()).trim();
                    if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.cellId = val;
                } else if (trimmed.startsWith("PCI/PSC=")) {
                    String val = trimmed.substring("PCI/PSC=".length()).trim();
                    if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.pci = val;
                } else if (trimmed.startsWith("频点=")) {
                    String val = trimmed.substring("频点=".length()).trim();
                    if (!val.isEmpty() && !"unknown".equalsIgnoreCase(val)) info.channel = val;
                }
            }
        }
    }
}
