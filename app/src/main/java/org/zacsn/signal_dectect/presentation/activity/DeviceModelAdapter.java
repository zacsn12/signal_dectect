package org.zacsn.signal_dectect.presentation.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.zacsn.signal_dectect.R;

import java.util.ArrayList;
import java.util.List;

public class DeviceModelAdapter extends RecyclerView.Adapter<DeviceModelAdapter.ViewHolder> {

    public static class ModelItem {
        public String name;
        public boolean isSelected;

        public ModelItem(String name, boolean isSelected) {
            this.name = name;
            this.isSelected = isSelected;
        }
    }

    private List<ModelItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ModelItem item, boolean isChecked);
    }

    public DeviceModelAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ModelItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device_model, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ModelItem item = items.get(position);
        holder.tvName.setText(item.name);
        holder.cbSelected.setChecked(item.isSelected);

        holder.itemView.setOnClickListener(v -> {
            item.isSelected = !item.isSelected;
            holder.cbSelected.setChecked(item.isSelected);
            if (listener != null) {
                listener.onItemClick(item, item.isSelected);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        CheckBox cbSelected;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_manufacturer_name);
            cbSelected = itemView.findViewById(R.id.cb_selected);
        }
    }
}
