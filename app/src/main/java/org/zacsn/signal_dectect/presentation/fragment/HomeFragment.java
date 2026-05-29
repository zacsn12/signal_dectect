package org.zacsn.signal_dectect.presentation.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.zacsn.signal_dectect.R;
import org.zacsn.signal_dectect.data.database.BlacklistItemEntity;
import org.zacsn.signal_dectect.data.repository.DeviceRepository;
import org.zacsn.signal_dectect.data.repository.ScanRepository;
import org.zacsn.signal_dectect.databinding.FragmentHomeBinding;
import org.zacsn.signal_dectect.presentation.activity.LanDeviceScanActivity;
import org.zacsn.signal_dectect.presentation.activity.SignalInspectActivity;
import javax.inject.Inject;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {
    
    private FragmentHomeBinding binding;

    @Inject
    ScanRepository scanRepository;

    @Inject
    DeviceRepository deviceRepository;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupClickListeners();
        setupDashboardObservers();
    }
    
    private void setupClickListeners() {
        // Bluetooth Inspect
        binding.cardBluetoothInspect.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SignalInspectActivity.class);
            intent.putExtra("SCAN_TYPE", "BLUETOOTH");
            startActivity(intent);
        });
        
        // WiFi Inspect
        binding.cardWifiInspect.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SignalInspectActivity.class);
            intent.putExtra("SCAN_TYPE", "WIFI");
            startActivity(intent);
        });
        
        // LAN Scan - Use dedicated LAN scan activity
        binding.cardLanScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), LanDeviceScanActivity.class);
            startActivity(intent);
        });

        // Cellular Scan
        binding.cardCellularScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SignalInspectActivity.class);
            intent.putExtra("SCAN_TYPE", "CELLULAR");
            startActivity(intent);
        });
    }

    private void setupDashboardObservers() {
        if (getContext() == null) return;

        // Observe scan history to display history record count
        scanRepository.getScanHistory().observe(getViewLifecycleOwner(), records -> {
            if (records != null && binding != null) {
                binding.tvHistoryCount.setText(String.valueOf(records.size()));
            }
        });

        // Observe whitelist items to display trusted device count
        deviceRepository.getWhitelist().observe(getViewLifecycleOwner(), whitelist -> {
            if (whitelist != null && binding != null) {
                binding.tvWhitelistCount.setText(String.valueOf(whitelist.size()));
            }
        });

        // Observe blacklist items to calculate security score and details status
        deviceRepository.getBlacklist().observe(getViewLifecycleOwner(), blacklist -> {
            if (blacklist == null || binding == null || getContext() == null) return;

            boolean hasBluetoothThreat = false;
            boolean hasWifiThreat = false;
            boolean hasCellularThreat = false;

            for (BlacklistItemEntity item : blacklist) {
                String type = item.getDeviceType();
                if ("bluetooth".equalsIgnoreCase(type)) {
                    hasBluetoothThreat = true;
                } else if ("wifi".equalsIgnoreCase(type)) {
                    hasWifiThreat = true;
                } else if ("cellular".equalsIgnoreCase(type)) {
                    hasCellularThreat = true;
                }
            }

            // Calculate score (deduct 15 points per blacklisted threat device, min 0)
            int score = Math.max(0, 100 - blacklist.size() * 15);
            binding.tvScoreVal.setText(String.valueOf(score));

            // Set score text color & overall protection status badge based on score
            if (score >= 90) {
                binding.tvScoreVal.setTextColor(requireContext().getColor(R.color.success));
                binding.tvDashboardStatus.setText("系统安全 · 实时防护中");
                binding.tvDashboardStatus.setTextColor(requireContext().getColor(R.color.success));
            } else if (score >= 60) {
                binding.tvScoreVal.setTextColor(requireContext().getColor(R.color.warning));
                binding.tvDashboardStatus.setText("存在隐患 · 请排查威胁");
                binding.tvDashboardStatus.setTextColor(requireContext().getColor(R.color.warning));
            } else {
                binding.tvScoreVal.setTextColor(requireContext().getColor(R.color.error));
                binding.tvDashboardStatus.setText("检测到威胁 · 极高风险");
                binding.tvDashboardStatus.setTextColor(requireContext().getColor(R.color.error));
            }

            // Update sub-items status
            if (hasBluetoothThreat) {
                binding.tvStatusBluetooth.setText("检测到威胁");
                binding.tvStatusBluetooth.setTextColor(requireContext().getColor(R.color.error));
            } else {
                binding.tvStatusBluetooth.setText("运行正常");
                binding.tvStatusBluetooth.setTextColor(requireContext().getColor(R.color.success));
            }

            if (hasWifiThreat) {
                binding.tvStatusWifi.setText("检测到威胁");
                binding.tvStatusWifi.setTextColor(requireContext().getColor(R.color.error));
            } else {
                binding.tvStatusWifi.setText("运行正常");
                binding.tvStatusWifi.setTextColor(requireContext().getColor(R.color.success));
            }

            if (hasCellularThreat) {
                binding.tvStatusCellular.setText("检测到威胁");
                binding.tvStatusCellular.setTextColor(requireContext().getColor(R.color.error));
            } else {
                binding.tvStatusCellular.setText("运行正常");
                binding.tvStatusCellular.setTextColor(requireContext().getColor(R.color.success));
            }
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
