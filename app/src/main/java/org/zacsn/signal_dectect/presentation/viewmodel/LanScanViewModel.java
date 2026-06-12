package org.zacsn.signal_dectect.presentation.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.domain.model.LanDevice;
import org.zacsn.signal_dectect.util.MacVendorUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class LanScanViewModel extends ViewModel {
    private static final int TOTAL_HOSTS = 254;

    private final MutableLiveData<LanScanState> scanState = new MutableLiveData<>(LanScanState.IDLE);
    private final MutableLiveData<List<LanDevice>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusText = new MutableLiveData<>("准备扫描");

    private final List<LanDevice> deviceList = Collections.synchronizedList(new ArrayList<>());
    private ExecutorService executorService;
    private volatile boolean isScanning = false;

    @Inject
    public LanScanViewModel() {
    }

    public void startScan() {
        stopExecutor();
        isScanning = true;
        deviceList.clear();
        devices.postValue(new ArrayList<>());
        scanState.postValue(LanScanState.SCANNING);
        statusText.postValue("正在扫描局域网设备...");

        executorService = Executors.newFixedThreadPool(20);
        new Thread(() -> {
            String localIp = getLocalIpAddress();
            if (localIp == null) {
                isScanning = false;
                scanState.postValue(LanScanState.ERROR);
                statusText.postValue("无法获取本机IP地址");
                stopExecutor();
                return;
            }

            String subnet = localIp.substring(0, localIp.lastIndexOf("."));
            scanSubnet(subnet);
        }).start();
    }

    public void stopScan() {
        isScanning = false;
        stopExecutor();
        scanState.postValue(LanScanState.IDLE);
        statusText.postValue("扫描已停止");
    }

    public LiveData<LanScanState> getScanState() {
        return scanState;
    }

    public LiveData<List<LanDevice>> getDevices() {
        return devices;
    }

    public LiveData<String> getStatusText() {
        return statusText;
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
            statusText.postValue("获取本机IP失败: " + e.getMessage());
        }
        return null;
    }

    private void scanSubnet(String subnet) {
        AtomicInteger completedCount = new AtomicInteger(0);

        for (int i = 1; i <= TOTAL_HOSTS; i++) {
            final String host = subnet + "." + i;

            executorService.execute(() -> {
                if (!isScanning || Thread.currentThread().isInterrupted()) {
                    return;
                }

                try {
                    InetAddress address = InetAddress.getByName(host);
                    if (isScanning && address.isReachable(1000)) {
                        String hostname = address.getHostName();
                        String mac = getMacAddress(host);
                        String manufacturer = mac != null ? MacVendorUtils.getVendor(mac) : "未知";

                        LanDevice device = new LanDevice(
                                host,
                                mac != null ? mac : "Unknown",
                                hostname.equals(host) ? "Unknown" : hostname,
                                manufacturer,
                                false
                        );

                        deviceList.add(device);
                        devices.postValue(new ArrayList<>(deviceList));
                    }
                } catch (Exception e) {
                    // Host not reachable.
                }

                int done = completedCount.incrementAndGet();
                if (!isScanning) {
                    return;
                }

                int progress = (done * 100) / TOTAL_HOSTS;
                statusText.postValue("扫描进度: " + progress + "%");

                if (done == TOTAL_HOSTS) {
                    isScanning = false;
                    scanState.postValue(LanScanState.COMPLETE);
                    statusText.postValue("扫描完成，共发现 " + deviceList.size() + " 个设备");
                    stopExecutor();
                }
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
                            reader.close();
                            return mac.toUpperCase();
                        }
                    }
                }
            }
            reader.close();
        } catch (Exception e) {
            // ARP lookup may fail on newer Android versions.
        }
        return null;
    }

    private void stopExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = null;
    }

    @Override
    protected void onCleared() {
        stopScan();
        super.onCleared();
    }

    public enum LanScanState {
        IDLE, SCANNING, COMPLETE, ERROR
    }
}
