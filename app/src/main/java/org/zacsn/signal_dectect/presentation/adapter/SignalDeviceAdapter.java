package org.zacsn.signal_dectect.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.zacsn.signal_dectect.databinding.ItemSignalDeviceBinding;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.util.DistanceUtils;
import androidx.core.content.ContextCompat;

public class SignalDeviceAdapter extends ListAdapter<SignalDevice, SignalDeviceAdapter.ViewHolder> {
    
    private final OnDeviceClickListener listener;
    private final java.util.Set<String> highlightedMacs = new java.util.HashSet<>();
    
    public interface OnDeviceClickListener {
        void onDeviceClick(SignalDevice device);
    }
    
    public SignalDeviceAdapter(OnDeviceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    public void setHighlightedMacs(java.util.Set<String> macs) {
        highlightedMacs.clear();
        if (macs != null) {
            highlightedMacs.addAll(macs);
        }
        notifyDataSetChanged();
    }
    
    private static final DiffUtil.ItemCallback<SignalDevice> DIFF_CALLBACK = 
        new DiffUtil.ItemCallback<SignalDevice>() {
            @Override
            public boolean areItemsTheSame(@NonNull SignalDevice oldItem, @NonNull SignalDevice newItem) {
                return oldItem.getMacAddress().equals(newItem.getMacAddress());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull SignalDevice oldItem, @NonNull SignalDevice newItem) {
                return oldItem.getSignalStrength() == newItem.getSignalStrength() &&
                       oldItem.getDistance() == newItem.getDistance() &&
                       oldItem.getManufacturerConfidence() == newItem.getManufacturerConfidence() &&
                       oldItem.getManufacturerVerdict() == newItem.getManufacturerVerdict() &&
                       java.util.Objects.equals(oldItem.getManufacturer(), newItem.getManufacturer()) &&
                       java.util.Objects.equals(oldItem.getCandidateManufacturer(), newItem.getCandidateManufacturer()) &&
                       java.util.Objects.equals(oldItem.getManufacturerEvidence(), newItem.getManufacturerEvidence());
            }
        };
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSignalDeviceBinding binding = ItemSignalDeviceBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSignalDeviceBinding binding;
        
        ViewHolder(ItemSignalDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(SignalDevice device) {
            boolean isHighlighted = highlightedMacs.contains(normalizeMac(device.getMacAddress()));
            binding.viewCardBackground.setBackgroundResource(isHighlighted
                ? R.drawable.bg_pill_rose
                : R.drawable.bg_card_rounded);
            binding.tvTargetBadge.setVisibility(isHighlighted ? android.view.View.VISIBLE : android.view.View.GONE);

            binding.tvDeviceName.setText(device.getDeviceName() != null 
                ? device.getDeviceName() : "Unknown Device");
            binding.tvMacAddress.setText(device.getMacAddress());
            binding.tvSignalStrength.setText(device.getSignalStrength() + " dBm");
            binding.tvDistance.setText(DistanceUtils.formatMeters(device.getDistance()));
            binding.tvDeviceType.setText(device.getDeviceType().name());
            bindManufacturerVerdict(device);
            
            // Set signal strength status dot & text color dynamically
            int dbm = device.getSignalStrength();
            android.content.Context context = binding.getRoot().getContext();
            if (dbm >= -60) {
                binding.viewSignalStatus.setBackgroundResource(R.drawable.bg_icon_circle_green);
                binding.tvSignalStrength.setTextColor(ContextCompat.getColor(context, R.color.success));
            } else if (dbm >= -80) {
                binding.viewSignalStatus.setBackgroundResource(R.drawable.bg_icon_circle_orange);
                binding.tvSignalStrength.setTextColor(ContextCompat.getColor(context, R.color.warning));
            } else {
                binding.viewSignalStatus.setBackgroundResource(R.drawable.bg_icon_circle_red);
                binding.tvSignalStrength.setTextColor(ContextCompat.getColor(context, R.color.error));
            }
            
            // Set manufacturer brand icon dynamically
            String mfr = getDisplayManufacturer(device).toLowerCase(java.util.Locale.US);
            
            if (mfr.contains("apple")) {
                binding.ivManufacturerIcon.setImageResource(R.drawable.ic_brand_apple);
                binding.ivManufacturerIcon.clearColorFilter();
            } else if (mfr.contains("microsoft")) {
                binding.ivManufacturerIcon.setImageResource(R.drawable.ic_brand_microsoft);
                binding.ivManufacturerIcon.clearColorFilter();
            } else if (mfr.contains("huawei")) {
                binding.ivManufacturerIcon.setImageResource(R.drawable.ic_brand_huawei);
                binding.ivManufacturerIcon.clearColorFilter();
            } else if (mfr.contains("xiaomi") || mfr.contains("mi")) {
                binding.ivManufacturerIcon.setImageResource(R.drawable.ic_brand_xiaomi);
                binding.ivManufacturerIcon.clearColorFilter();
            } else {
                // Default fallback based on device type
                if (device.getDeviceType() == org.zacsn.signal_dectect.domain.model.DeviceType.WIFI) {
                    binding.ivManufacturerIcon.setImageResource(R.drawable.ic_wifi);
                } else {
                    binding.ivManufacturerIcon.setImageResource(R.drawable.ic_bluetooth);
                }
                binding.ivManufacturerIcon.setColorFilter(ContextCompat.getColor(context, R.color.primary));
            }
            
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        }

        private String normalizeMac(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().replace("-", ":").toUpperCase(java.util.Locale.US);
        }

        private void bindManufacturerVerdict(SignalDevice device) {
            ManufacturerVerdict verdict = device.getManufacturerVerdict();
            android.content.Context context = binding.getRoot().getContext();
            binding.tvManufacturerVerdict.setText(getVerdictLabel(device));
            binding.tvManufacturerSummary.setText(buildManufacturerSummary(device));

            switch (verdict) {
                case CONFIRMED:
                    binding.tvManufacturerVerdict.setBackgroundResource(R.drawable.bg_pill_green);
                    binding.tvManufacturerVerdict.setTextColor(ContextCompat.getColor(context, R.color.success));
                    break;
                case LIKELY:
                    binding.tvManufacturerVerdict.setBackgroundResource(R.drawable.bg_pill_blue);
                    binding.tvManufacturerVerdict.setTextColor(ContextCompat.getColor(context, R.color.primary_variant));
                    break;
                case POSSIBLE:
                    binding.tvManufacturerVerdict.setBackgroundResource(R.drawable.bg_pill_orange);
                    binding.tvManufacturerVerdict.setTextColor(ContextCompat.getColor(context, R.color.warning));
                    break;
                case UNKNOWN:
                default:
                    binding.tvManufacturerVerdict.setBackgroundResource(R.drawable.bg_pill_gray);
                    binding.tvManufacturerVerdict.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
                    break;
            }
        }

        private String getVerdictLabel(SignalDevice device) {
            ManufacturerVerdict verdict = device.getManufacturerVerdict();
            int confidence = device.getManufacturerConfidence();
            if (!isUsefulManufacturer(getDisplayManufacturer(device))) {
                return "未知厂商";
            }
            switch (verdict) {
                case CONFIRMED:
                    return "已确认 " + confidence + "%";
                case LIKELY:
                    return "高可信 " + confidence + "%";
                case POSSIBLE:
                    return "候选线索";
                default:
                    return "未知厂商";
            }
        }

        private String buildManufacturerSummary(SignalDevice device) {
            String manufacturer = getDisplayManufacturer(device);
            if (!isUsefulManufacturer(manufacturer)) {
                return "暂无明确厂商线索";
            }
            String sourceLabel = getSourceLabel(device.getManufacturerSource());
            if (device.getManufacturerVerdict() == ManufacturerVerdict.POSSIBLE
                    && device.getManufacturerConfidence() > 0) {
                return manufacturer + " / " + sourceLabel + " · 线索可信度 "
                        + device.getManufacturerConfidence() + "%";
            }
            return manufacturer + " / " + sourceLabel;
        }

        private String getDisplayManufacturer(SignalDevice device) {
            if (isUsefulManufacturer(device.getManufacturer())) {
                return device.getManufacturer();
            }
            if (isUsefulManufacturer(device.getCandidateManufacturer())) {
                return device.getCandidateManufacturer();
            }
            return "";
        }

        private String getSourceLabel(String source) {
            if (source == null) {
                return "来源未知";
            }
            switch (source) {
                case "gatt_device_info+pnp_id":
                    return "GATT设备信息+PnP ID";
                case "gatt_device_info":
                    return "GATT设备信息";
                case "gatt_pnp_id":
                    return "GATT PnP ID";
                case "gatt_device_info_conflict":
                    return "GATT线索冲突";
                case "device_name":
                    return "设备名称线索";
                case "ble_company_id":
                    return "BLE广播Company ID";
                case "mac_oui":
                    return "MAC OUI线索";
                case "ble_mac_oui":
                    return "BLE MAC OUI线索";
                case "classic_mac_oui":
                    return "经典蓝牙MAC OUI";
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
                case "cellular_operator":
                    return "运营商";
                default:
                    return "来源未知";
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
    }
}
