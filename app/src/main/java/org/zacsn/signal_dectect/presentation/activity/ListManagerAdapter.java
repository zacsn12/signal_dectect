package org.zacsn.signal_dectect.presentation.activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.zacsn.signal_dectect.R;

import java.util.ArrayList;
import java.util.List;

public class ListManagerAdapter extends RecyclerView.Adapter<ListManagerAdapter.ViewHolder> {

    private List<ListItem> items = new ArrayList<>();
    private final OnItemDeleteListener listener;

    public interface OnItemDeleteListener {
        void onDelete(ListItem item);
    }

    public ListManagerAdapter(OnItemDeleteListener listener) {
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

        ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_item_title);
            tvSubtitle = itemView.findViewById(R.id.tv_item_subtitle);
            ivDelete = itemView.findViewById(R.id.iv_delete);
        }
    }
}
