package org.zacsn.signal_dectect.presentation.activity;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import org.zacsn.signal_dectect.databinding.ActivityLanDeviceScanBinding;
import org.zacsn.signal_dectect.domain.model.LanDevice;
import org.zacsn.signal_dectect.presentation.adapter.LanDeviceAdapter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LanDeviceScanActivity extends AppCompatActivity {
    
    private ActivityLanDeviceScanBinding binding;
    private LanDeviceAdapter adapter;
    private List<LanDevice> deviceList = new ArrayList<>();
    private boolean isScanning = false;
    private ExecutorService executorService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLanDeviceScanBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.tvLargeDeviceCount.setText("0");
        binding.btnBack.setOnClickListener(v -> finish());
        
        executorService = Executors.newFixedThreadPool(20);
        
        setupRecyclerView();
        setupScanButton();
    }
    
    private void setupRecyclerView() {
        adapter = new LanDeviceAdapter(device -> {
            // Handle device click
        });
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }
    
    private void setupScanButton() {
        binding.btnStartScan.setOnClickListener(v -> {
            if (!isScanning) {
                startLanScan();
            } else {
                stopLanScan();
            }
        });
    }
    
    private void stopLanScan() {
        isScanning = false;
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        
        binding.btnStartScan.setText("开始扫描");
        binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
        binding.progressBar.setVisibility(View.GONE);
        binding.tvStatus.setText("扫描已停止");
        if (binding.radarSweep != null) {
            binding.radarSweep.clearAnimation();
        }
    }
    
    private void startLanScan() {
        isScanning = true;
        deviceList.clear();
        adapter.submitList(new ArrayList<>());
        
        // Ensure any previous executor is shut down
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = Executors.newFixedThreadPool(20);
        
        binding.btnStartScan.setText("停止扫描");
        binding.btnStartScan.setIconResource(android.R.drawable.ic_media_pause);
        binding.tvLargeDeviceCount.setText("0");
        binding.tvDeviceCount.setText("发现设备: 0");
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("正在扫描局域网设备...");
        
        // Start radar sweep animation
        android.view.animation.Animation rotateAnim = android.view.animation.AnimationUtils.loadAnimation(this, org.zacsn.signal_dectect.R.anim.radar_sweep);
        binding.radarSweep.startAnimation(rotateAnim);
        
        new Thread(() -> {
            try {
                String localIp = getLocalIpAddress();
                if (localIp == null) {
                    runOnUiThread(() -> {
                        if (binding == null || isFinishing() || isDestroyed()) return;
                        binding.tvStatus.setText("无法获取本机IP地址");
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnStartScan.setText("开始扫描");
                        binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
                        isScanning = false;
                        binding.radarSweep.clearAnimation();
                    });
                    return;
                }
                
                String subnet = localIp.substring(0, localIp.lastIndexOf("."));
                scanSubnet(subnet);
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (binding == null || isFinishing() || isDestroyed()) return;
                    binding.tvStatus.setText("扫描失败: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnStartScan.setText("开始扫描");
                    binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
                    isScanning = false;
                    binding.radarSweep.clearAnimation();
                });
            }
        }).start();
    }
    
    private String getLocalIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                
                java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof java.net.Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private void scanSubnet(String subnet) {
        int totalHosts = 254;
        java.util.concurrent.atomic.AtomicInteger completedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        
        for (int i = 1; i <= totalHosts; i++) {
            final String host = subnet + "." + i;
            
            executorService.execute(() -> {
                if (!isScanning || Thread.currentThread().isInterrupted()) {
                    return;
                }
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (!isScanning) return;
                    if (address.isReachable(1000)) {
                        if (!isScanning) return;
                        String hostname = address.getHostName();
                        String mac = getMacAddress(host);
                        
                        LanDevice device = new LanDevice(
                            host,
                            mac != null ? mac : "Unknown",
                            hostname.equals(host) ? "Unknown" : hostname,
                            "Unknown",
                            false
                        );
                        
                        deviceList.add(device);
                        
                        runOnUiThread(() -> {
                            if (binding == null || isFinishing() || isDestroyed()) return;
                            adapter.submitList(new ArrayList<>(deviceList));
                            binding.tvDeviceCount.setText("发现设备: " + deviceList.size());
                            binding.tvLargeDeviceCount.setText(String.valueOf(deviceList.size()));
                        });
                    }
                } catch (Exception e) {
                    // Host not reachable
                }
                
                int done = completedCount.incrementAndGet();
                runOnUiThread(() -> {
                    if (binding == null || isFinishing() || isDestroyed() || !isScanning) return;
                    int progress = (done * 100) / totalHosts;
                    binding.tvStatus.setText("扫描进度: " + progress + "%");
                    
                    if (done == totalHosts) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnStartScan.setText("开始扫描");
                        binding.btnStartScan.setIconResource(android.R.drawable.ic_media_play);
                        binding.tvStatus.setText("扫描完成，共发现 " + deviceList.size() + " 个设备");
                        isScanning = false;
                        if (binding.radarSweep != null) {
                            binding.radarSweep.clearAnimation();
                        }
                    }
                });
            });
        }
    }
    
    private String getMacAddress(String ip) {
        try {
            Process process = Runtime.getRuntime().exec("ping -c 1 " + ip);
            process.waitFor();
            
            process = Runtime.getRuntime().exec("cat /proc/net/arp");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains(ip)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        String mac = parts[3];
                        if (mac.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")) {
                            return mac.toUpperCase();
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (binding != null && binding.radarSweep != null) {
            binding.radarSweep.clearAnimation();
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        binding = null;
    }
}
