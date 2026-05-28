package org.zacsn.signal_dectect.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.zacsn.signal_dectect.databinding.ItemSignalDeviceBinding;
import org.zacsn.signal_dectect.domain.model.SignalDevice;
import org.zacsn.signal_dectect.R;
import androidx.core.content.ContextCompat;

public class SignalDeviceAdapter extends ListAdapter<SignalDevice, SignalDeviceAdapter.ViewHolder> {
    
    private final OnDeviceClickListener listener;
    
    public interface OnDeviceClickListener {
        void onDeviceClick(SignalDevice device);
    }
    
    public SignalDeviceAdapter(OnDeviceClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
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
                       oldItem.getDistance() == newItem.getDistance();
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
            binding.tvDeviceName.setText(device.getDeviceName() != null 
                ? device.getDeviceName() : "Unknown Device");
            binding.tvMacAddress.setText(device.getMacAddress());
            binding.tvSignalStrength.setText(device.getSignalStrength() + " dBm");
            binding.tvDistance.setText(String.format("%.2f m", device.getDistance()));
            binding.tvDeviceType.setText(device.getDeviceType().name());
            
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
            String mfr = device.getManufacturer();
            if (mfr == null) {
                mfr = "";
            }
            mfr = mfr.toLowerCase();
            
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
    }
}
