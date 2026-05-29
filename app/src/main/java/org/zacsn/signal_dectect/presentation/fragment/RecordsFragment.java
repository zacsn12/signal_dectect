package org.zacsn.signal_dectect.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.zacsn.signal_dectect.databinding.FragmentRecordsBinding;
import org.zacsn.signal_dectect.presentation.activity.ScanRecordDetailActivity;
import org.zacsn.signal_dectect.presentation.adapter.ScanRecordAdapter;
import org.zacsn.signal_dectect.presentation.viewmodel.RecordsViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.ArrayList;
import java.util.List;

@AndroidEntryPoint
public class RecordsFragment extends Fragment {
    
    private FragmentRecordsBinding binding;
    private RecordsViewModel viewModel;
    private ScanRecordAdapter adapter;
    private boolean isManageMode = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        binding = FragmentRecordsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this).get(RecordsViewModel.class);
        
        setupRecyclerView();
        setupButtons();
        observeViewModel();
        setupSearch();
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterRecords(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void setupRecyclerView() {
        adapter = new ScanRecordAdapter(
            // Click listener - open detail or show rename dialog
            record -> {
                if (isManageMode) {
                    showRenameDialog(record);
                } else {
                    Intent intent = new Intent(requireContext(), ScanRecordDetailActivity.class);
                    intent.putExtra("RECORD_ID", record.getId());
                    startActivity(intent);
                }
            },
            // Long click listener - show options
            record -> {
                if (!isManageMode) {
                    showRecordOptionsDialog(record);
                }
                return true;
            }
        );
        
        adapter.setOnSelectionChangedListener(selectedCount -> {
            binding.tvSelectedCount.setText("已选择: " + selectedCount);
            binding.checkboxSelectAll.setChecked(
                selectedCount > 0 && selectedCount == adapter.getItemCount()
            );
        });
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
    }

    
    private void setupButtons() {
        // Manage button
        binding.btnManage.setOnClickListener(v -> {
            toggleManageMode();
        });
        
        // Select all checkbox
        binding.checkboxSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                adapter.selectAll();
            } else {
                adapter.clearSelection();
            }
        });
        
        // Delete selected button
        binding.btnDeleteSelected.setOnClickListener(v -> {
            if (adapter.getSelectedIds().isEmpty()) {
                android.widget.Toast.makeText(requireContext(), 
                    "请先选择要删除的记录", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            showBatchDeleteConfirmDialog();
        });
        
        // Cancel selection button
        binding.btnCancelSelection.setOnClickListener(v -> {
            exitManageMode();
        });
    }
    
    private void toggleManageMode() {
        if (isManageMode) {
            exitManageMode();
        } else {
            enterManageMode();
        }
    }
    
    private void enterManageMode() {
        isManageMode = true;
        adapter.setSelectionMode(true);
        binding.selectionToolbar.setVisibility(View.VISIBLE);
        binding.btnManage.setText("完成");
    }
    
    private void exitManageMode() {
        isManageMode = false;
        adapter.setSelectionMode(false);
        binding.selectionToolbar.setVisibility(View.GONE);
        binding.btnManage.setText("管理");
        binding.checkboxSelectAll.setChecked(false);
    }
    
    private void showRecordOptionsDialog(org.zacsn.signal_dectect.data.database.ScanRecordEntity record) {
        String[] options = {"重命名", "删除"};
        new AlertDialog.Builder(requireContext())
                .setTitle("记录操作")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Rename
                        showRenameDialog(record);
                    } else if (which == 1) {
                        // Delete
                        showDeleteConfirmDialog(record);
                    }
                })
                .show();
    }
    
    private void showRenameDialog(org.zacsn.signal_dectect.data.database.ScanRecordEntity record) {
        EditText editText = new EditText(requireContext());
        editText.setHint("输入记录名称");
        
        // Set current name if exists
        if (record.getName() != null && !record.getName().isEmpty()) {
            editText.setText(record.getName());
            editText.setSelection(record.getName().length());
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("重命名记录")
                .setView(editText)
                .setPositiveButton("确定", (dialog, which) -> {
                    String newName = editText.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        viewModel.updateRecordName(record.getId(), newName);
                        android.widget.Toast.makeText(requireContext(), 
                            "记录已重命名", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showDeleteConfirmDialog(org.zacsn.signal_dectect.data.database.ScanRecordEntity record) {
        new AlertDialog.Builder(requireContext())
                .setTitle("删除记录")
                .setMessage("确定要删除这条扫描记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deleteRecord(record);
                    android.widget.Toast.makeText(requireContext(), 
                        "记录已删除", android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private void showBatchDeleteConfirmDialog() {
        int count = adapter.getSelectedIds().size();
        new AlertDialog.Builder(requireContext())
                .setTitle("批量删除")
                .setMessage("确定要删除选中的 " + count + " 条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    List<Long> selectedIds = new ArrayList<>(adapter.getSelectedIds());
                    viewModel.deleteRecords(selectedIds);
                    android.widget.Toast.makeText(requireContext(), 
                        "已删除 " + count + " 条记录", android.widget.Toast.LENGTH_SHORT).show();
                    exitManageMode();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    private List<org.zacsn.signal_dectect.data.database.ScanRecordEntity> allRecordsList = new ArrayList<>();
    private String currentSearchQuery = "";

    private void observeViewModel() {
        viewModel.getAllRecords().observe(getViewLifecycleOwner(), records -> {
            allRecordsList = records;
            filterRecords(currentSearchQuery);
            binding.btnManage.setEnabled(!records.isEmpty());
        });
    }

    private void filterRecords(String query) {
        currentSearchQuery = query == null ? "" : query.toLowerCase().trim();
        List<org.zacsn.signal_dectect.data.database.ScanRecordEntity> filteredList = new ArrayList<>();
        
        if (currentSearchQuery.isEmpty()) {
            filteredList.addAll(allRecordsList);
        } else {
            for (org.zacsn.signal_dectect.data.database.ScanRecordEntity record : allRecordsList) {
                boolean matchName = record.getName() != null && record.getName().toLowerCase().contains(currentSearchQuery);
                // Also search by formatted time if needed, but name is main target
                if (matchName) {
                    filteredList.add(record);
                } else {
                    // Try to match formatted time
                    String timeStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(record.getTimestamp()));
                    if (timeStr.contains(currentSearchQuery)) {
                        filteredList.add(record);
                    }
                }
            }
        }
        
        adapter.submitList(filteredList);
        
        if (filteredList.isEmpty()) {
            binding.tvEmptyMessage.setVisibility(View.VISIBLE);
            binding.recyclerView.setVisibility(View.GONE);
        } else {
            binding.tvEmptyMessage.setVisibility(View.GONE);
            binding.recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
