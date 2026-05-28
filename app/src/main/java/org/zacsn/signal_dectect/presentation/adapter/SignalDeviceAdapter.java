package org.zacsn.signal_dectect.presentation.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.zacsn.signal_dectect.databinding.ItemSignalDeviceBinding;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

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
            
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeviceClick(device);
                }
            });
        }
    }
}
