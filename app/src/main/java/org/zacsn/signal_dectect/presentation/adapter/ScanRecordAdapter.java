package org.zacsn.signal_dectect.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import org.zacsn.signal_dectect.data.database.ScanRecordEntity;
import org.zacsn.signal_dectect.databinding.ItemScanRecordBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ScanRecordAdapter extends ListAdapter<ScanRecordEntity, ScanRecordAdapter.ViewHolder> {
    
    private final OnRecordClickListener clickListener;
    private final OnRecordLongClickListener longClickListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    
    private boolean selectionMode = false;
    private final Set<Long> selectedIds = new HashSet<>();
    private OnSelectionChangedListener selectionChangedListener;
    
    public interface OnRecordClickListener {
        void onRecordClick(ScanRecordEntity record);
    }
    
    public interface OnRecordLongClickListener {
        boolean onRecordLongClick(ScanRecordEntity record);
    }
    
    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }
    
    public ScanRecordAdapter(OnRecordClickListener clickListener, OnRecordLongClickListener longClickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }
    
    // Constructor for backward compatibility (only click listener)
    public ScanRecordAdapter(OnRecordClickListener clickListener) {
        this(clickListener, null);
    }
    
    private static final DiffUtil.ItemCallback<ScanRecordEntity> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<ScanRecordEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ScanRecordEntity oldItem, @NonNull ScanRecordEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ScanRecordEntity oldItem, @NonNull ScanRecordEntity newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp() &&
                   oldItem.getDeviceCount() == newItem.getDeviceCount() &&
                   java.util.Objects.equals(oldItem.getName(), newItem.getName());
        }
    };

    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemScanRecordBinding binding = ItemScanRecordBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }
    
    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) {
            selectedIds.clear();
        }
        notifyDataSetChanged();
    }
    
    public boolean isSelectionMode() {
        return selectionMode;
    }
    
    public Set<Long> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }
    
    public void selectAll() {
        selectedIds.clear();
        for (int i = 0; i < getItemCount(); i++) {
            selectedIds.add(getItem(i).getId());
        }
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelectionChanged();
    }
    
    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }
    
    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedIds.size());
        }
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemScanRecordBinding binding;
        
        ViewHolder(ItemScanRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(ScanRecordEntity record) {
            // Set scan type
            String scanType = getScanTypeName(record.getScanType());
            binding.tvScanType.setText(scanType);
            
            // Set scan time
            String timeStr = dateFormat.format(new Date(record.getTimestamp()));
            binding.tvScanTime.setText(timeStr);
            
            // Set record name if available
            if (record.getName() != null && !record.getName().isEmpty()) {
                binding.tvRecordName.setText(record.getName());
                binding.tvRecordName.setVisibility(View.VISIBLE);
            } else {
                binding.tvRecordName.setVisibility(View.GONE);
            }
            
            // Set device count
            binding.tvDeviceCount.setText("设备数: " + record.getDeviceCount());
            
            // Set Apple device info (if available)
            // Note: This requires parsing devicesJson to count Apple devices
            // For now, hide it
            binding.tvAppleInfo.setVisibility(View.GONE);
            
            // Handle selection mode
            if (selectionMode) {
                binding.checkboxSelect.setVisibility(View.VISIBLE);
                binding.checkboxSelect.setChecked(selectedIds.contains(record.getId()));
                binding.checkboxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedIds.add(record.getId());
                    } else {
                        selectedIds.remove(record.getId());
                    }
                    notifySelectionChanged();
                });
            } else {
                binding.checkboxSelect.setVisibility(View.GONE);
            }
            
            // Set click listener
            itemView.setOnClickListener(v -> {
                if (selectionMode) {
                    // Toggle selection in selection mode
                    binding.checkboxSelect.setChecked(!binding.checkboxSelect.isChecked());
                } else {
                    // Normal click
                    if (clickListener != null) {
                        clickListener.onRecordClick(record);
                    }
                }
            });
            
            // Set long click listener
            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onRecordLongClick(record);
                }
                return false;
            });
        }
        
        private String getScanTypeName(int scanType) {
            // scanType is a bitmask: 1=BT, 2=WiFi, 4=Cellular
            if (scanType == 1) {
                return "蓝牙巡检";
            } else if (scanType == 2) {
                return "WiFi巡检";
            } else if (scanType == 3) {
                return "蓝牙+WiFi";
            } else {
                return "扫描记录";
            }
        }
    }
}
