package org.zacsn.signal_dectect.presentation.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import org.zacsn.signal_dectect.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        
        // Format brand name for display
        holder.tvName.setText(formatBrandName(item.name));
        holder.switchSelected.setChecked(item.isSelected);
        
        // Bind brand logo or letter avatar
        bindBrandLogo(holder, item.name);

        holder.itemView.setOnClickListener(v -> {
            item.isSelected = !item.isSelected;
            holder.switchSelected.setChecked(item.isSelected);
            if (listener != null) {
                listener.onItemClick(item, item.isSelected);
            }
        });
    }

    private String formatBrandName(String rawName) {
        if (rawName == null) return "";
        switch (rawName.toLowerCase(Locale.US)) {
            case "apple": return "Apple";
            case "samsung": return "Samsung";
            case "huawei": return "Huawei";
            case "xiaomi": return "Xiaomi";
            case "oppo": return "OPPO";
            case "vivo": return "vivo";
            case "honor": return "Honor";
            case "oneplus": return "OnePlus";
            case "meizu": return "Meizu";
            case "sony": return "Sony";
            case "google": return "Google";
            case "microsoft": return "Microsoft";
            default:
                if (rawName.length() > 0) {
                    return rawName.substring(0, 1).toUpperCase(Locale.US) + rawName.substring(1);
                }
                return rawName;
        }
    }

    private void bindBrandLogo(ViewHolder holder, String brandName) {
        String key = brandName.toLowerCase(Locale.US);
        int iconResId = 0;
        
        switch (key) {
            case "apple":
                iconResId = R.drawable.ic_brand_apple;
                break;
            case "huawei":
                iconResId = R.drawable.ic_brand_huawei;
                break;
            case "xiaomi":
                iconResId = R.drawable.ic_brand_xiaomi;
                break;
            case "microsoft":
                iconResId = R.drawable.ic_brand_microsoft;
                break;
            case "samsung":
                iconResId = R.drawable.ic_brand_samsung;
                break;
            case "oppo":
                iconResId = R.drawable.ic_brand_oppo;
                break;
            case "vivo":
                iconResId = R.drawable.ic_brand_vivo;
                break;
            case "honor":
                iconResId = R.drawable.ic_brand_honor;
                break;
            case "oneplus":
                iconResId = R.drawable.ic_brand_oneplus;
                break;
            case "meizu":
                iconResId = R.drawable.ic_brand_meizu;
                break;
            case "sony":
                iconResId = R.drawable.ic_brand_sony;
                break;
            case "google":
                iconResId = R.drawable.ic_brand_google;
                break;
        }

        if (iconResId != 0) {
            holder.ivLogo.setImageResource(iconResId);
            holder.ivLogo.setVisibility(View.VISIBLE);
            holder.ivLogo.setImageTintList(null); // Keep original vector colors
            holder.tvLetter.setVisibility(View.GONE);
        } else {
            holder.ivLogo.setVisibility(View.GONE);
            holder.tvLetter.setVisibility(View.VISIBLE);
            String letter = key.length() > 0 ? key.substring(0, 1).toUpperCase(Locale.US) : "?";
            holder.tvLetter.setText(letter);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        ImageView ivLogo;
        TextView tvLetter;
        SwitchMaterial switchSelected;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_manufacturer_name);
            ivLogo = itemView.findViewById(R.id.iv_brand_logo);
            tvLetter = itemView.findViewById(R.id.tv_brand_avatar_letter);
            switchSelected = itemView.findViewById(R.id.switch_selected);
        }
    }
}
