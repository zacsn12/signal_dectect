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
            
            if (device.isCamera()) {
                binding.tvDeviceType.setText("📷 摄像头设备");
                binding.tvDeviceType.setVisibility(android.view.View.VISIBLE);
            } else {
                binding.tvDeviceType.setVisibility(android.view.View.GONE);
            }
            
            itemView.setOnClickListener(v -> listener.onDeviceClick(device));
        }
    }
}
