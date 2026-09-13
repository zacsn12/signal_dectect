package org.zacsn.signal_dectect.presentation.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.databinding.ItemFakeBaseStationBinding;
import org.zacsn.signal_dectect.domain.model.FakeBaseStationCell;
import org.zacsn.signal_dectect.domain.model.FakeCellRiskLevel;

public class FakeBaseStationAdapter extends ListAdapter<FakeBaseStationCell, FakeBaseStationAdapter.ViewHolder> {

    public interface OnCellClickListener {
        void onCellClick(FakeBaseStationCell cell);
    }

    private final OnCellClickListener clickListener;

    public FakeBaseStationAdapter(OnCellClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    private static final DiffUtil.ItemCallback<FakeBaseStationCell> DIFF_CALLBACK = new DiffUtil.ItemCallback<FakeBaseStationCell>() {
        @Override
        public boolean areItemsTheSame(@NonNull FakeBaseStationCell oldItem, @NonNull FakeBaseStationCell newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull FakeBaseStationCell oldItem, @NonNull FakeBaseStationCell newItem) {
            return oldItem.getSignalDbm() == newItem.getSignalDbm()
                    && oldItem.getRiskScore() == newItem.getRiskScore()
                    && oldItem.getRiskLevel() == newItem.getRiskLevel()
                    && oldItem.isServing() == newItem.isServing()
                    && oldItem.getAnomalyReasons().equals(newItem.getAnomalyReasons());
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFakeBaseStationBinding binding = ItemFakeBaseStationBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FakeBaseStationCell cell = getItem(position);
        holder.bind(cell, clickListener);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemFakeBaseStationBinding binding;

        public ViewHolder(ItemFakeBaseStationBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(FakeBaseStationCell cell, OnCellClickListener listener) {
            Context context = itemView.getContext();

            binding.tvOperator.setText(cell.getOperator());
            bindOperatorBadge(cell.getOperator(), cell.getPlmn());

            binding.tvRadioType.setText(cell.getRadioType());
            binding.tvServingBadge.setVisibility(cell.isServing() ? View.VISIBLE : View.GONE);

            // Risk badge
            bindRiskBadge(cell.getRiskLevel(), context);

            // Identities
            String idLabel = (cell.getRadioType().contains("5G") || cell.getRadioType().contains("4G")) ? "TAC" : "LAC";
            String cidLabel = cell.getRadioType().contains("5G") ? "NCI" : (cell.getRadioType().contains("4G") ? "CI" : "CID");
            binding.tvCellIdentity.setText(idLabel + ": " + cell.getLacTac() + " · " + cidLabel + ": " + cell.getCellId());
            binding.tvCellSubIdentity.setText("PCI: " + cell.getPciPsc() + " · 频点: " + cell.getChannel() + " · PLMN: " + cell.getPlmn());

            // Signal
            binding.tvSignalDbm.setText(cell.getSignalDbm() + " dBm");
            binding.tvSignalDesc.setText(getSignalQualityDesc(cell.getSignalDbm()));

            // Anomaly reasons
            if (!cell.getAnomalyReasons().isEmpty() && cell.getRiskLevel() != FakeCellRiskLevel.SAFE) {
                binding.layoutAnomalyBox.setVisibility(View.VISIBLE);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cell.getAnomalyReasons().size(); i++) {
                    sb.append("• ").append(cell.getAnomalyReasons().get(i));
                    if (i < cell.getAnomalyReasons().size() - 1) {
                        sb.append("\n");
                    }
                }
                binding.tvAnomalyReasons.setText(sb.toString());
                if (cell.getRiskLevel() == FakeCellRiskLevel.DANGEROUS) {
                    binding.layoutAnomalyBox.setBackgroundColor(0xFFFEE2E2);
                    binding.tvAnomalyReasons.setTextColor(0xFFDC2626);
                } else {
                    binding.layoutAnomalyBox.setBackgroundColor(0xFFFEF3C7);
                    binding.tvAnomalyReasons.setTextColor(0xFFD97706);
                }
            } else {
                binding.layoutAnomalyBox.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCellClick(cell);
            });
        }

        private void bindOperatorBadge(String operator, String plmn) {
            if (operator == null) operator = "";
            if (plmn == null) plmn = "";

            if (operator.contains("移动") || plmn.startsWith("46000") || plmn.startsWith("46002") || plmn.startsWith("46007")) {
                binding.tvOperator.setBackgroundResource(R.drawable.bg_pill_blue);
                binding.tvOperator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.primary));
            } else if (operator.contains("电信") || plmn.startsWith("46003") || plmn.startsWith("46005") || plmn.startsWith("46011")) {
                binding.tvOperator.setBackgroundResource(R.drawable.bg_pill_blue);
                binding.tvOperator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.secondary));
            } else if (operator.contains("联通") || plmn.startsWith("46001") || plmn.startsWith("46006") || plmn.startsWith("46009")) {
                binding.tvOperator.setBackgroundResource(R.drawable.bg_pill_orange);
                binding.tvOperator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.warning));
            } else if (operator.contains("广电") || plmn.startsWith("46015")) {
                binding.tvOperator.setBackgroundResource(R.drawable.bg_pill_green);
                binding.tvOperator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.success));
            } else {
                binding.tvOperator.setBackgroundResource(R.drawable.bg_pill_gray);
                binding.tvOperator.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }

        private void bindRiskBadge(FakeCellRiskLevel level, Context context) {
            switch (level) {
                case DANGEROUS:
                    binding.tvRiskBadge.setText("⚠️ 伪基站威胁");
                    binding.tvRiskBadge.setBackgroundResource(R.drawable.bg_pill_red);
                    binding.tvRiskBadge.setTextColor(0xFFDC2626);
                    break;
                case SUSPICIOUS:
                    binding.tvRiskBadge.setText("可疑关注");
                    binding.tvRiskBadge.setBackgroundResource(R.drawable.bg_pill_orange);
                    binding.tvRiskBadge.setTextColor(0xFFD97706);
                    break;
                case SAFE:
                default:
                    binding.tvRiskBadge.setText("正常基站");
                    binding.tvRiskBadge.setBackgroundResource(R.drawable.bg_pill_green);
                    binding.tvRiskBadge.setTextColor(0xFF059669);
                    break;
            }
        }

        private String getSignalQualityDesc(int dbm) {
            if (dbm >= -80) return "信号极佳";
            if (dbm >= -95) return "信号良好";
            if (dbm >= -105) return "信号中等";
            if (dbm >= -115) return "信号较弱";
            return "信号微弱";
        }
    }
}
