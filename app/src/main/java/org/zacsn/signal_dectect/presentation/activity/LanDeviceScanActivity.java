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
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("局域网扫描");
        }
        
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
            }
        });
    }
    
    private void startLanScan() {
        isScanning = true;
        deviceList.clear();
        adapter.submitList(new ArrayList<>());
        
        binding.btnStartScan.setEnabled(false);
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("正在扫描局域网设备...");
        
        new Thread(() -> {
            try {
                String localIp = getLocalIpAddress();
                if (localIp == null) {
                    runOnUiThread(() -> {
                        binding.tvStatus.setText("无法获取本机IP地址");
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnStartScan.setEnabled(true);
                        isScanning = false;
                    });
                    return;
                }
                
                String subnet = localIp.substring(0, localIp.lastIndexOf("."));
                scanSubnet(subnet);
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.tvStatus.setText("扫描失败: " + e.getMessage());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnStartScan.setEnabled(true);
                    isScanning = false;
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
        int scannedCount = 0;
        int totalHosts = 254;
        
        for (int i = 1; i <= totalHosts; i++) {
            final String host = subnet + "." + i;
            final int currentCount = i;
            
            executorService.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (address.isReachable(1000)) {
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
                            adapter.submitList(new ArrayList<>(deviceList));
                            binding.tvDeviceCount.setText("发现设备: " + deviceList.size());
                        });
                    }
                } catch (Exception e) {
                    // Host not reachable
                }
                
                runOnUiThread(() -> {
                    int progress = (currentCount * 100) / totalHosts;
                    binding.tvStatus.setText("扫描进度: " + progress + "%");
                    
                    if (currentCount == totalHosts) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.btnStartScan.setEnabled(true);
                        binding.tvStatus.setText("扫描完成，共发现 " + deviceList.size() + " 个设备");
                        isScanning = false;
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
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        binding = null;
    }
}
