package org.zacsn.signal_dectect.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.zacsn.signal_dectect.databinding.ItemLanDeviceBinding;
import org.zacsn.signal_dectect.domain.model.LanDevice;

public class LanDeviceAdapter extends ListAdapter<LanDevice, LanDeviceAdapter.ViewHolder> {
    
    private final OnDeviceClickListener listener;
    
    public interface OnDeviceClickListener {
        void onDeviceClick(LanDevice device);
    }
    
    public LanDeviceAdapter(OnDeviceClickListener listener) {
        super(new DiffUtil.ItemCallback<LanDevice>() {
            @Override
            public boolean areItemsTheSame(@NonNull LanDevice oldItem, @NonNull LanDevice newItem) {
                return oldItem.getIpAddress().equals(newItem.getIpAddress());
            }
            
            @Override
            public boolean areContentsTheSame(@NonNull LanDevice oldItem, @NonNull LanDevice newItem) {
                return oldItem.equals(newItem);
            }
        });
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLanDeviceBinding binding = ItemLanDeviceBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemLanDeviceBinding binding;
        
        ViewHolder(ItemLanDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(LanDevice device, OnDeviceClickListener listener) {
            binding.tvIpAddress.setText(device.getIpAddress());
            binding.tvHostname.setText(device.getHostname());
            binding.tvMacAddress.setText("MAC: " + device.getMacAddress());

            if (device.isGateway()) {
                binding.tvDeviceType.setText("网关/路由器");
                binding.tvDeviceType.setBackgroundResource(org.zacsn.signal_dectect.R.drawable.bg_pill_green);
                binding.tvDeviceType.setTextColor(itemView.getContext().getColor(org.zacsn.signal_dectect.R.color.success));
            } else if (device.isCamera()) {
                binding.tvDeviceType.setText("摄像头设备");
                binding.tvDeviceType.setBackgroundResource(org.zacsn.signal_dectect.R.drawable.bg_pill_orange);
                binding.tvDeviceType.setTextColor(itemView.getContext().getColor(org.zacsn.signal_dectect.R.color.warning));
            } else {
                binding.tvDeviceType.setText(device.getDeviceCategory());
                binding.tvDeviceType.setBackgroundResource(org.zacsn.signal_dectect.R.drawable.bg_pill_blue);
                binding.tvDeviceType.setTextColor(itemView.getContext().getColor(org.zacsn.signal_dectect.R.color.primary));
            }

            binding.tvManufacturer.setText("厂商: " + safeText(device.getManufacturer()));
            binding.tvConfidence.setText("可信度 " + device.getConfidence() + "%");
            binding.tvDiscoveryMethod.setText("发现方式: " + safeText(device.getDiscoveryMethod()));
            
            itemView.setOnClickListener(v -> listener.onDeviceClick(device));
        }

        private String safeText(String value) {
            return value == null || value.trim().isEmpty() ? "未知" : value;
        }
    }
}
