package org.zacsn.signal_dectect.presentation.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.zacsn.signal_dectect.R;

import java.util.ArrayList;
import java.util.List;

public class ListManagerAdapter extends RecyclerView.Adapter<ListManagerAdapter.ViewHolder> {

    private List<ListItem> items = new ArrayList<>();
    private final int listType;
    private final OnItemDeleteListener listener;

    public interface OnItemDeleteListener {
        void onDelete(ListItem item);
    }

    public ListManagerAdapter(int listType, OnItemDeleteListener listener) {
        this.listType = listType;
        this.listener = listener;
    }

    public void setItems(List<ListItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list_manager, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ListItem item = items.get(position);
        holder.tvTitle.setText(item.primaryKey);
        
        if (item.subtitle != null && !item.subtitle.isEmpty()) {
            holder.tvSubtitle.setText(item.subtitle);
            holder.tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            holder.tvSubtitle.setVisibility(View.GONE);
        }

        // Set list-specific icon and background circles
        if (listType == ListManagerActivity.TYPE_WHITELIST) {
            holder.flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_green);
            holder.ivListIcon.setImageResource(R.drawable.ic_whitelist);
        } else if (listType == ListManagerActivity.TYPE_BLACKLIST) {
            holder.flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_red);
            holder.ivListIcon.setImageResource(R.drawable.ic_blacklist);
        } else {
            holder.flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_blue);
            holder.ivListIcon.setImageResource(R.drawable.ic_device_model);
        }

        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvSubtitle;
        ImageView ivDelete;
        
        FrameLayout flIconBg;
        ImageView ivListIcon;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            ivDelete = itemView.findViewById(R.id.iv_delete);
            flIconBg = itemView.findViewById(R.id.fl_icon_bg);
            ivListIcon = itemView.findViewById(R.id.iv_list_icon);
        }
    }
}
