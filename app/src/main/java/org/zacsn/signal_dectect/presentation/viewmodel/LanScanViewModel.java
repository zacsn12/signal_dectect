package org.zacsn.signal_dectect.presentation.viewmodel;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.zacsn.signal_dectect.domain.model.LanDevice;
import org.zacsn.signal_dectect.util.MacVendorUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

@HiltViewModel
public class LanScanViewModel extends ViewModel {
    private static final int TOTAL_HOSTS = 254;
    private static final int THREAD_COUNT = 32;
    private static final int REACHABLE_TIMEOUT_MS = 600;
    private static final long ARP_CACHE_TTL_MS = 1_000L;

    private final MutableLiveData<LanScanState> scanState = new MutableLiveData<>(LanScanState.IDLE);
    private final MutableLiveData<List<LanDevice>> devices = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> statusText = new MutableLiveData<>("准备扫描");
    private final Context context;

    private final List<LanDevice> deviceList = Collections.synchronizedList(new ArrayList<>());
    private ExecutorService executorService;
    private volatile boolean isScanning = false;
    private final AtomicInteger scanGeneration = new AtomicInteger(0);
    private final Object arpCacheLock = new Object();
    private Map<String, String> cachedArpTable = new HashMap<>();
    private long cachedArpTableAt = 0L;

    @Inject
    public LanScanViewModel(@ApplicationContext Context context) {
        this.context = context;
    }

    public void startScan() {
        stopExecutor();
        int generation = scanGeneration.incrementAndGet();
        isScanning = true;
        deviceList.clear();
        clearArpCache();
        devices.postValue(new ArrayList<>());
        scanState.postValue(LanScanState.SCANNING);
        statusText.postValue("正在扫描局域网设备...");

        ExecutorService scanExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        executorService = scanExecutor;
        new Thread(() -> {
            NetworkInfo networkInfo = getNetworkInfo();
            if (!isCurrentScan(generation)) {
                return;
            }
            if (networkInfo == null) {
                isScanning = false;
                scanState.postValue(LanScanState.ERROR);
                statusText.postValue("无法获取本机局域网信息");
                stopExecutor();
                return;
            }

            statusText.postValue("正在扫描 " + networkInfo.subnetPrefix + ".0/24");
            scanSubnet(networkInfo, generation, scanExecutor);
        }).start();
    }

    public void stopScan() {
        isScanning = false;
        scanGeneration.incrementAndGet();
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

    private NetworkInfo getNetworkInfo() {
        try {
            android.net.wifi.WifiManager wifiManager =
                    (android.net.wifi.WifiManager) context.getApplicationContext()
                            .getSystemService(Context.WIFI_SERVICE);

            String gatewayIp = null;
            if (wifiManager != null) {
                android.net.DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                if (dhcpInfo != null) {
                    gatewayIp = intToIp(dhcpInfo.gateway);
                    String wifiIp = intToIp(dhcpInfo.ipAddress);
                    if (wifiIp != null && !wifiIp.startsWith("0.") && !wifiIp.startsWith("127.")) {
                        return new NetworkInfo(
                                wifiIp,
                                gatewayIp,
                                wifiIp.substring(0, wifiIp.lastIndexOf("."))
                        );
                    }
                }
            }

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
                            return new NetworkInfo(
                                    ip,
                                    gatewayIp,
                                    ip.substring(0, ip.lastIndexOf("."))
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            statusText.postValue("获取本机IP失败: " + e.getMessage());
        }
        return null;
    }

    private String intToIp(int value) {
        if (value == 0) {
            return null;
        }
        return (value & 0xFF) + "."
                + ((value >> 8) & 0xFF) + "."
                + ((value >> 16) & 0xFF) + "."
                + ((value >> 24) & 0xFF);
    }

    private void scanSubnet(NetworkInfo networkInfo, int generation, ExecutorService scanExecutor) {
        AtomicInteger completedCount = new AtomicInteger(0);
        Set<String> scheduledHosts = new HashSet<>();

        Map<String, String> initialArp = getArpTableSnapshot(true);
        for (String ip : initialArp.keySet()) {
            if (isSameSubnet(ip, networkInfo.subnetPrefix) && scheduledHosts.add(ip)) {
                addOrUpdateDevice(ip, initialArp.get(ip), resolveHostname(ip), "ARP缓存", networkInfo, generation);
            }
        }

        for (int i = 1; i <= TOTAL_HOSTS; i++) {
            final String host = networkInfo.subnetPrefix + "." + i;
            if (!scheduledHosts.add(host)) {
                continue;
            }
        }

        if (networkInfo.gatewayIp != null && isSameSubnet(networkInfo.gatewayIp, networkInfo.subnetPrefix)) {
            scheduledHosts.add(networkInfo.gatewayIp);
        }

        int totalTasks = scheduledHosts.size();
        if (totalTasks == 0) {
            if (!isCurrentScan(generation)) {
                return;
            }
            isScanning = false;
            scanState.postValue(LanScanState.COMPLETE);
            statusText.postValue("扫描完成，共发现 0 个设备");
            stopExecutor();
            return;
        }

        for (String host : scheduledHosts) {
            scheduleHostProbe(networkInfo, host, completedCount, totalTasks, generation, scanExecutor);
        }
    }

    private void scheduleHostProbe(
            NetworkInfo networkInfo,
            String host,
            AtomicInteger completedCount,
            int totalTasks,
            int generation,
            ExecutorService scanExecutor
    ) {
        if (scanExecutor == null || scanExecutor.isShutdown()) {
            return;
        }
        scanExecutor.execute(() -> {
            if (!isCurrentScan(generation) || Thread.currentThread().isInterrupted()) {
                return;
            }

            try {
                InetAddress address = InetAddress.getByName(host);
                boolean reachable = address.isReachable(REACHABLE_TIMEOUT_MS);
                pingHost(host);
                String mac = getMacAddress(host);
                if (isCurrentScan(generation) && (reachable || mac != null || host.equals(networkInfo.gatewayIp))) {
                    addOrUpdateDevice(
                            host,
                            mac,
                            resolveHostname(host),
                            reachable ? "ICMP/ARP探测" : "ARP缓存",
                            networkInfo,
                            generation
                    );
                }
            } catch (Exception e) {
                // Host not reachable.
            }

            int done = completedCount.incrementAndGet();
            if (!isCurrentScan(generation)) {
                return;
            }

            int progress = Math.min(100, (done * 100) / totalTasks);
            statusText.postValue("扫描进度: " + progress + "%");

            if (done >= totalTasks) {
                if (!isCurrentScan(generation)) {
                    return;
                }
                isScanning = false;
                scanState.postValue(LanScanState.COMPLETE);
                statusText.postValue("扫描完成，共发现 " + deviceList.size() + " 个设备");
                stopExecutor();
            }
        });
    }

    private String getMacAddress(String ip) {
        return getArpTableSnapshot(false).get(ip);
    }

    private void pingHost(String ip) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"ping", "-c", "1", "-W", "1", ip});
            process.waitFor();
        } catch (Exception e) {
            // Ignore ping failures.
        }
    }

    private Map<String, String> readArpTable() {
        Map<String, String> result = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/net/arp"))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    String ip = parts[0];
                    String mac = parts[3];
                    if (mac.matches("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")
                            && !"00:00:00:00:00:00".equals(mac)) {
                        result.put(ip, mac.toUpperCase());
                    }
                }
            }
        } catch (Exception e) {
            // ARP lookup may fail on newer Android versions.
        }
        return result;
    }

    private Map<String, String> getArpTableSnapshot(boolean forceRefresh) {
        long now = System.currentTimeMillis();
        synchronized (arpCacheLock) {
            if (!forceRefresh && now - cachedArpTableAt <= ARP_CACHE_TTL_MS) {
                return new HashMap<>(cachedArpTable);
            }
            cachedArpTable = readArpTable();
            cachedArpTableAt = now;
            return new HashMap<>(cachedArpTable);
        }
    }

    private void clearArpCache() {
        synchronized (arpCacheLock) {
            cachedArpTable = new HashMap<>();
            cachedArpTableAt = 0L;
        }
    }

    private void addOrUpdateDevice(
            String ip,
            String mac,
            String hostname,
            String discoveryMethod,
            NetworkInfo networkInfo,
            int generation
    ) {
        if (!isCurrentScan(generation)) {
            return;
        }
        String safeMac = mac != null ? mac : "Unknown";
        String manufacturer = mac != null ? MacVendorUtils.getVendor(mac) : "未知";
        if (manufacturer == null || manufacturer.trim().isEmpty()) {
            manufacturer = "未知";
        }
        boolean isGateway = ip.equals(networkInfo.gatewayIp);
        String category = inferDeviceCategory(ip, hostname, manufacturer, isGateway);
        boolean isCamera = "摄像头设备".equals(category);
        int confidence = calculateLanConfidence(mac, hostname, manufacturer, isGateway);

        LanDevice device = new LanDevice(
                ip,
                safeMac,
                normalizeHostname(ip, hostname),
                manufacturer,
                isCamera,
                category,
                discoveryMethod,
                confidence,
                isGateway,
                System.currentTimeMillis()
        );

        synchronized (deviceList) {
            if (!isCurrentScan(generation)) {
                return;
            }
            for (int i = 0; i < deviceList.size(); i++) {
                if (deviceList.get(i).getIpAddress().equals(ip)) {
                    deviceList.set(i, device);
                    postSortedDevices();
                    return;
                }
            }
            deviceList.add(device);
            postSortedDevices();
        }
    }

    private void postSortedDevices() {
        List<LanDevice> snapshot = new ArrayList<>(deviceList);
        snapshot.sort(Comparator
                .comparing(LanDevice::isGateway).reversed()
                .thenComparing(LanDevice::getIpAddress, this::compareIpAddress));
        devices.postValue(snapshot);
    }

    private int compareIpAddress(String first, String second) {
        return Integer.compare(lastOctet(first), lastOctet(second));
    }

    private int lastOctet(String ip) {
        try {
            return Integer.parseInt(ip.substring(ip.lastIndexOf(".") + 1));
        } catch (Exception e) {
            return 999;
        }
    }

    private String resolveHostname(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            return address.getCanonicalHostName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String normalizeHostname(String ip, String hostname) {
        if (hostname == null || hostname.trim().isEmpty() || hostname.equals(ip)) {
            return "Unknown";
        }
        return hostname;
    }

    private boolean isSameSubnet(String ip, String subnetPrefix) {
        return ip != null && ip.startsWith(subnetPrefix + ".");
    }

    private String inferDeviceCategory(String ip, String hostname, String manufacturer, boolean isGateway) {
        if (isGateway) {
            return "网关/路由器";
        }
        String text = ((hostname == null ? "" : hostname) + " " + (manufacturer == null ? "" : manufacturer))
                .toLowerCase();
        if (text.contains("camera") || text.contains("ipc") || text.contains("hikvision")
                || text.contains("dahua") || text.contains("ezviz")) {
            return "摄像头设备";
        }
        if (text.contains("router") || text.contains("gateway") || text.contains("tp-link")
                || text.contains("tplink") || text.contains("huawei") || text.contains("xiaomi")) {
            return "网络设备";
        }
        if (text.contains("android") || text.contains("iphone") || text.contains("ipad")
                || text.contains("samsung") || text.contains("oppo") || text.contains("vivo")) {
            return "移动终端";
        }
        if (text.contains("pc") || text.contains("desktop") || text.contains("laptop")
                || text.contains("windows") || text.contains("macbook")) {
            return "电脑终端";
        }
        return "未知设备";
    }

    private int calculateLanConfidence(String mac, String hostname, String manufacturer, boolean isGateway) {
        int confidence = 35;
        if (mac != null) {
            confidence += 30;
        }
        if (manufacturer != null && !"未知".equals(manufacturer) && !"未知厂商".equals(manufacturer)) {
            confidence += 20;
        }
        if (hostname != null && !"Unknown".equals(hostname) && !hostname.trim().isEmpty()) {
            confidence += 10;
        }
        if (isGateway) {
            confidence += 5;
        }
        return Math.min(confidence, 95);
    }

    private static final class NetworkInfo {
        private final String localIp;
        private final String gatewayIp;
        private final String subnetPrefix;

        private NetworkInfo(String localIp, String gatewayIp, String subnetPrefix) {
            this.localIp = localIp;
            this.gatewayIp = gatewayIp;
            this.subnetPrefix = subnetPrefix;
        }
    }

    private void stopExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
        executorService = null;
    }

    private boolean isCurrentScan(int generation) {
        return isScanning && scanGeneration.get() == generation;
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
