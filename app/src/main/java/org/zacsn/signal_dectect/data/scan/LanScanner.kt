package org.zacsn.signal_dectect.data.scan

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.zacsn.signal_dectect.domain.model.LanDevice
import org.zacsn.signal_dectect.util.MacVendorUtils
import java.io.BufferedReader
import java.io.FileReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 局域网设备发现引擎（Kotlin / 协程）。
 *
 * 旧实现依赖 /proc/net/arp 与 ping —— 在 Android 10+/无 root 下基本失效。
 * 本引擎以“主动 TCP 连接常见服务端口”为主判活：端口连上 = 在线且暴露服务；
 * 连接被拒(RST) = 主机在线但该端口关闭；两者都算发现。再辅以 ICMP 与 ARP（可读时）。
 * 端口画像用于识别摄像头(RTSP/DVR)、路由、PC。并发用协程 + 信号量结构化控制，可随时取消。
 *
 * 仍受 Android 无 root 的固有限制（MAC 多数拿不到）；目标是在系统允许范围内尽力发现，
 * 并如实呈现能力边界，而非假装拿到全部设备 MAC。
 */
class LanScanner(context: Context) {

    enum class State { IDLE, SCANNING, COMPLETE, ERROR }

    interface Listener {
        @JvmSuppressWildcards
        fun onDevices(devices: List<LanDevice>)
        fun onStatus(text: String)
        fun onState(state: State)
    }

    private val appContext: Context = context.applicationContext

    @Volatile
    var listener: Listener? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var scanJob: Job? = null

    private val runId = AtomicInteger(0)
    private val results = ConcurrentHashMap<String, LanDevice>()

    @Volatile
    private var arp: Map<String, String> = emptyMap()

    fun start() {
        stop()
        val myRun = runId.incrementAndGet()
        results.clear()
        listener?.onDevices(emptyList())
        listener?.onState(State.SCANNING)
        listener?.onStatus("正在扫描局域网设备...")
        scanJob = scope.launch {
            try {
                runScan(myRun)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                listener?.onState(State.ERROR)
                listener?.onStatus("扫描出错: " + (e.message ?: e.javaClass.simpleName))
            }
        }
    }

    fun stop() {
        scanJob?.cancel()
        scanJob = null
    }

    fun shutdown() {
        scope.cancel()
    }

    private suspend fun runScan(myRun: Int) = coroutineScope {
        val net = withContext(Dispatchers.IO) { resolveNetwork() }
        if (net == null) {
            listener?.onState(State.ERROR)
            listener?.onStatus("无法获取本机局域网信息")
            return@coroutineScope
        }
        listener?.onStatus("正在扫描 ${net.subnetPrefix}.0/24")

        // ARP 预热（可读时）：先把缓存里的同网段设备显示出来
        arp = withContext(Dispatchers.IO) { readArpTable() }
        for ((ip, mac) in arp) {
            if (ip.startsWith(net.subnetPrefix + ".")) {
                upsert(myRun, net, ip, mac, "ARP缓存", emptyList())
            }
        }

        val hosts = LinkedHashSet<String>()
        for (i in HOST_MIN..HOST_MAX) {
            hosts.add("${net.subnetPrefix}.$i")
        }
        net.gatewayIp?.let { gw ->
            if (gw.startsWith(net.subnetPrefix + ".")) hosts.add(gw)
        }

        val total = hosts.size
        val done = AtomicInteger(0)
        val gate = Semaphore(PARALLELISM)

        hosts.map { host ->
            launch(Dispatchers.IO) {
                gate.withPermit { probeHost(myRun, net, host) }
                val d = done.incrementAndGet()
                listener?.onStatus("扫描进度: ${minOf(100, d * 100 / total)}%")
            }
        }.joinAll()

        if (runId.get() == myRun) {
            listener?.onState(State.COMPLETE)
            listener?.onStatus("扫描完成，共发现 ${results.size} 个设备")
        }
    }

    private fun probeHost(myRun: Int, net: Network, host: String) {
        if (runId.get() != myRun) return

        val open = ArrayList<Int>()
        var refused = false
        for (port in PROBE_PORTS) {
            if (runId.get() != myRun) return
            Socket().use { socket ->
                try {
                    socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                    open.add(port)
                } catch (e: java.net.ConnectException) {
                    refused = true
                } catch (e: Exception) {
                    // 超时 / 不可达 —— 无信号
                }
            }
        }

        var alive = open.isNotEmpty() || refused
        if (!alive) {
            alive = try {
                InetAddress.getByName(host).isReachable(REACHABLE_TIMEOUT_MS)
            } catch (e: Exception) {
                false
            }
        }

        val isGateway = host == net.gatewayIp
        val mac = arp[host]
        if (!alive && mac == null && !isGateway) return

        val method = when {
            open.isNotEmpty() -> "端口探测(" + open.joinToString(",") + ")"
            refused -> "端口探测"
            mac != null -> "ARP缓存"
            else -> "ICMP探测"
        }
        upsert(myRun, net, host, mac, method, open)
    }

    private fun upsert(
        myRun: Int,
        net: Network,
        ip: String,
        macRaw: String?,
        method: String,
        openPorts: List<Int>
    ) {
        if (runId.get() != myRun) return

        val manufacturer = (if (macRaw != null) MacVendorUtils.getVendor(macRaw) else null)
            ?.takeIf { it.isNotBlank() } ?: "未知"
        val isGateway = ip == net.gatewayIp
        val hostname = normalizeHostname(ip, resolveHostname(ip))
        val category = inferCategory(hostname, manufacturer, isGateway, openPorts)
        val isCamera = category == "摄像头设备"
        val confidence = confidenceOf(macRaw, hostname, manufacturer, isGateway, openPorts)

        val device = LanDevice(
            ip,
            macRaw ?: "Unknown",
            hostname,
            manufacturer,
            isCamera,
            category,
            method,
            confidence,
            isGateway,
            System.currentTimeMillis()
        )

        if (runId.get() != myRun) return
        results[ip] = device
        listener?.onDevices(ArrayList(results.values))
    }

    private fun inferCategory(
        hostname: String,
        manufacturer: String,
        isGateway: Boolean,
        openPorts: List<Int>
    ): String {
        if (isGateway) return "网关/路由器"
        if (openPorts.any { it == 554 || it == 8000 || it == 37777 || it == 34567 }) {
            return "摄像头设备"
        }
        val text = (hostname + " " + manufacturer).lowercase()
        if (listOf("camera", "ipc", "hikvision", "dahua", "ezviz").any { text.contains(it) }) {
            return "摄像头设备"
        }
        if (openPorts.contains(445) || openPorts.contains(139)) {
            return "电脑终端"
        }
        if (listOf("router", "gateway", "tp-link", "tplink", "huawei", "xiaomi").any { text.contains(it) }) {
            return "网络设备"
        }
        if (listOf("android", "iphone", "ipad", "samsung", "oppo", "vivo").any { text.contains(it) }) {
            return "移动终端"
        }
        if (listOf("pc", "desktop", "laptop", "windows", "macbook").any { text.contains(it) }) {
            return "电脑终端"
        }
        if (openPorts.contains(80) || openPorts.contains(443) || openPorts.contains(8080)) {
            return "网络设备"
        }
        return "未知设备"
    }

    private fun confidenceOf(
        mac: String?,
        hostname: String,
        manufacturer: String,
        isGateway: Boolean,
        openPorts: List<Int>
    ): Int {
        var c = 35
        if (mac != null) c += 30
        if (manufacturer != "未知" && manufacturer != "未知厂商") c += 20
        if (hostname != "Unknown" && hostname.isNotBlank()) c += 10
        if (isGateway) c += 5
        if (openPorts.isNotEmpty()) c += 10
        return minOf(c, 98)
    }

    private fun resolveNetwork(): Network? {
        try {
            val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            var gatewayIp: String? = null
            if (wifi != null) {
                @Suppress("DEPRECATION")
                val dhcp = wifi.dhcpInfo
                if (dhcp != null) {
                    gatewayIp = intToIp(dhcp.gateway)
                    val wifiIp = intToIp(dhcp.ipAddress)
                    if (wifiIp != null && !wifiIp.startsWith("0.") && !wifiIp.startsWith("127.")) {
                        return Network(wifiIp, gatewayIp, wifiIp.substring(0, wifiIp.lastIndexOf(".")))
                    }
                }
            }
            val ifaces = NetworkInterface.getNetworkInterfaces()
            while (ifaces.hasMoreElements()) {
                val iface = ifaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr is Inet4Address) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("127.")) {
                            return Network(ip, gatewayIp, ip.substring(0, ip.lastIndexOf(".")))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // 忽略，返回 null
        }
        return null
    }

    private fun intToIp(value: Int): String? {
        if (value == 0) return null
        return "${value and 0xFF}.${(value shr 8) and 0xFF}.${(value shr 16) and 0xFF}.${(value shr 24) and 0xFF}"
    }

    private fun readArpTable(): Map<String, String> {
        val result = HashMap<String, String>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split(Regex("\\s+"))
                    if (parts.size >= 4) {
                        val ip = parts[0]
                        val mac = parts[3]
                        if (mac.matches(Regex("([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}")) &&
                            mac != "00:00:00:00:00:00"
                        ) {
                            result[ip] = mac.uppercase()
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            // /proc/net/arp 在新版本 Android 上通常不可读
        }
        return result
    }

    private fun resolveHostname(ip: String): String {
        return try {
            InetAddress.getByName(ip).canonicalHostName
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun normalizeHostname(ip: String, hostname: String?): String {
        if (hostname.isNullOrBlank() || hostname == ip) return "Unknown"
        return hostname
    }

    private data class Network(
        val localIp: String,
        val gatewayIp: String?,
        val subnetPrefix: String
    )

    companion object {
        private val PROBE_PORTS = intArrayOf(80, 443, 8080, 554, 8000, 37777, 34567, 22, 445)
        private const val CONNECT_TIMEOUT_MS = 300
        private const val REACHABLE_TIMEOUT_MS = 400
        private const val PARALLELISM = 64
        private const val HOST_MIN = 1
        private const val HOST_MAX = 254
    }
}
