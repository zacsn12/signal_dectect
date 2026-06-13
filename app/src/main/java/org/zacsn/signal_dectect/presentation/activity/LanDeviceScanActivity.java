package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.zacsn.signal_dectect.databinding.ActivityLanDeviceScanBinding;
import org.zacsn.signal_dectect.domain.model.LanDevice;
import org.zacsn.signal_dectect.presentation.adapter.LanDeviceAdapter;
import org.zacsn.signal_dectect.presentation.viewmodel.LanScanViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LanDeviceScanActivity extends AppCompatActivity {
    private ActivityLanDeviceScanBinding binding;
    private LanDeviceAdapter adapter;
    private LanScanViewModel viewModel;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(LanScanViewModel.class);

        binding.tvLargeDeviceCount.setText("0");
        binding.btnBack.setOnClickListener(v -> finish());

        setupRecyclerView();
        setupScanButton();
        observeViewModel();
    }

    private void setupRecyclerView() {
        adapter = new LanDeviceAdapter(device -> {
            showLanDeviceDetail(device);
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupScanButton() {
        binding.btnStartScan.setOnClickListener(v -> {
            if (isScanning) {
                viewModel.stopScan();
            } else {
                viewModel.startScan();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getScanState().observe(this, state -> {
            isScanning = state == LanScanViewModel.LanScanState.SCANNING;
            if (isScanning) {
                binding.btnStartScan.setText("停止扫描");
                binding.btnStartScan.setIconResource(android.R.drawable.ic_media_pause);
                binding.progressBar.setVisibility(View.VISIBLE);
                android.view.animation.Animation rotateAnim = android.view.animation.AnimationUtils.loadAnimation(
                        this,
                        org.zacsn.signal_dectect.R.anim.radar_sweep
                );
                binding.radarSweep.startAnimation(rotateAnim);
            } else {
                binding.btnStartScan.setText("开始扫描");
                binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
                binding.progressBar.setVisibility(View.GONE);
                binding.radarSweep.clearAnimation();
            }
        });

        viewModel.getDevices().observe(this, devices -> {
            java.util.List<LanDevice> sorted = new java.util.ArrayList<>(devices);
            sorted.sort((first, second) -> {
                if (first.isGateway() != second.isGateway()) {
                    return first.isGateway() ? -1 : 1;
                }
                return Integer.compare(lastOctet(first.getIpAddress()), lastOctet(second.getIpAddress()));
            });
            adapter.submitList(sorted);
            binding.tvDeviceCount.setText("发现设备: " + devices.size());
            binding.tvLargeDeviceCount.setText(String.valueOf(devices.size()));
        });

        viewModel.getStatusText().observe(this, status -> binding.tvStatus.setText(status));
    }

    private void showLanDeviceDetail(LanDevice device) {
        String message = "IP 地址: " + safeText(device.getIpAddress()) + "\n"
                + "MAC 地址: " + safeText(device.getMacAddress()) + "\n"
                + "主机名: " + safeText(device.getHostname()) + "\n"
                + "厂商: " + safeText(device.getManufacturer()) + "\n"
                + "设备类别: " + safeText(device.getDeviceCategory()) + "\n"
                + "发现方式: " + safeText(device.getDiscoveryMethod()) + "\n"
                + "可信度: " + device.getConfidence() + "%\n"
                + "网关设备: " + (device.isGateway() ? "是" : "否");

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("局域网设备详情")
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .show();
    }

    private int lastOctet(String ip) {
        try {
            return Integer.parseInt(ip.substring(ip.lastIndexOf(".") + 1));
        } catch (Exception e) {
            return 999;
        }
    }

    private String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null && binding.radarSweep != null) {
            binding.radarSweep.clearAnimation();
        }
        binding = null;
    }
}
