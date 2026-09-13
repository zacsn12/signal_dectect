package org.zacsn.signal_dectect.data.scan

import android.os.SystemClock
import org.zacsn.signal_dectect.domain.model.DeviceType
import org.zacsn.signal_dectect.domain.model.PresenceState
import org.zacsn.signal_dectect.domain.model.ScanType
import org.zacsn.signal_dectect.domain.model.SignalDevice
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 单线程扫描聚合引擎，也是设备状态的唯一权威。
 *
 * 取代此前 SignalScanService 中“多来源回调 + CopyOnWriteArrayList + 主线程巡检”的做法：
 * 所有对设备表的读写、合并去重、在线状态更新与过期清理，都在同一个后台单线程上串行完成，
 * 从根本上消除并发写入导致的重复设备 / 丢更新问题。
 *
 * 线程约定：
 * - [submit] 可从任意线程调用（内部投递到引擎线程）。
 * - [Listener] 的回调在引擎线程上触发；UI 侧消费者需自行切回主线程
 *   （现有 ViewModel 使用 LiveData.postValue，天然线程安全）。
 * - [allDevices] / [sessionDevices] 返回的是最近一次快照，读取无锁、不阻塞。
 *
 * 快照与回调是分开的：设备表一有变更就立刻重算快照（后台线程上的一次浅拷贝），
 * 保证跨线程读取始终新鲜；而 [Listener] 回调按 [ScanTuning.EMIT_INTERVAL_MS] 合并发出，
 * 避免 BLE 高频广播把通知刷新与 LiveData 打爆。
 */
class ScanEngine @JvmOverloads constructor(
    private val clock: Clock = Clock { SystemClock.elapsedRealtime() },
    private val scheduler: Scheduler = ExecutorScheduler()
) {

    fun interface Listener {
        /** 设备列表发生变化（新增 / 更新 / 状态变更 / 移除）时回调，携带当前完整快照。 */
        fun onDevicesUpdated(devices: List<SignalDevice>)
    }

    /** 单调时钟。抽出来是为了让引擎能在纯 JVM 单元测试里推进时间。 */
    fun interface Clock {
        fun now(): Long
    }

    fun interface Cancellation {
        fun cancel()
    }

    /**
     * 引擎的执行与定时抽象。
     *
     * 刻意做得比 [ScheduledExecutorService] 窄：测试只需实现三个方法，
     * 就能用「调用线程同步执行 + 手动驱动节拍」拿到完全确定的断言。
     */
    interface Scheduler {
        /** 投递一个任务到引擎线程；同一实例的任务保证 FIFO 串行执行。 */
        fun execute(task: Runnable)

        /** 以固定间隔重复执行，返回取消句柄。 */
        fun schedulePeriodic(intervalMs: Long, task: Runnable): Cancellation

        fun shutdown()
    }

    // 以下两个 Map 与 dirty / lastPruneAt 仅在引擎线程访问，无需额外同步。key = 设备 MAC。
    private val devices = LinkedHashMap<String, SignalDevice>()
    private val sessionDevices = LinkedHashMap<String, SignalDevice>()
    private var dirty = false
    private var lastPruneAt = 0L

    @Volatile private var scanType: ScanType? = null
    @Volatile private var running = false
    @Volatile private var tickHandle: Cancellation? = null

    /**
     * 轮次号。每次 start / stop 递增，用于丢弃上一轮的迟到观测
     * （与 LanScanner 的 runId 是同一套做法）。
     */
    private val generation = AtomicInteger(0)

    // 最近一次快照，供无锁读取。
    @Volatile private var latest: List<SignalDevice> = emptyList()
    @Volatile private var latestSession: List<SignalDevice> = emptyList()

    @Volatile var listener: Listener? = null

    /** 开始一轮扫描：清空上一轮设备，按类型过滤，并启动周期性节拍。 */
    fun start(type: ScanType?) {
        val myRun = generation.incrementAndGet()
        running = true
        tickHandle?.cancel()
        scheduler.execute {
            if (generation.get() != myRun) return@execute
            // 清空与置类型都在引擎线程内完成，且排在后续 submit 之前（FIFO），
            // 因此不会出现「观测先入表又被 clear 抹掉」。
            scanType = type
            devices.clear()
            sessionDevices.clear()
            dirty = false
            lastPruneAt = clock.now()
            snapshot()
            notifyListener()
        }
        tickHandle = scheduler.schedulePeriodic(ScanTuning.EMIT_INTERVAL_MS) { tick(myRun) }
    }

    /** 停止扫描：保留已发现设备列表（与原实现一致），仅停止节拍并做一次末态发射。 */
    fun stop() {
        running = false
        generation.incrementAndGet()
        tickHandle?.cancel()
        tickHandle = null
        scheduler.execute {
            dirty = false
            snapshot()
            notifyListener()
        }
    }

    /** 提交一条来自任意采集源的设备观测。线程安全。 */
    fun submit(observed: SignalDevice) {
        if (!running) return
        val myRun = generation.get()
        scheduler.execute {
            if (!running || generation.get() != myRun) return@execute
            if (!matchesScanType(observed)) return@execute
            val key = observed.macAddress ?: return@execute
            // 合并而非覆盖：GATT 已确认的厂商、firstSeen、用户标记位都要保住。
            // 顺序不能反 —— merge 沿用最新观测的时间戳，refreshedAt 才把它推到当前时刻。
            val merged = SignalDeviceMerge.merge(devices[key], observed).refreshedAt(clock.now())
            devices[key] = merged
            sessionDevices[key] = merged
            snapshot()
            dirty = true
        }
    }

    fun allDevices(): List<SignalDevice> = latest

    fun sessionDevices(): List<SignalDevice> = latestSession

    fun shutdown() {
        tickHandle?.cancel()
        tickHandle = null
        scheduler.shutdown()
    }

    // ---- 以下仅在引擎线程执行 ----

    /**
     * 节拍：按需做过期巡检，并把累积的变更一次性发给监听者。
     *
     * 巡检与发射共用一个定时器，靠 [ScanTuning.STALE_PRUNE_INTERVAL_MS] 控制巡检的实际频率。
     */
    private fun tick(expectedGeneration: Int) {
        if (!running || generation.get() != expectedGeneration) return
        val now = clock.now()
        if (now - lastPruneAt >= ScanTuning.STALE_PRUNE_INTERVAL_MS) {
            lastPruneAt = now
            prune(now)
        }
        if (dirty) {
            dirty = false
            notifyListener()
        }
    }

    private fun prune(now: Long) {
        var changed = false
        val iterator = devices.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val device = entry.value
            var lastSeen = device.lastSeenElapsedRealtime
            if (lastSeen <= 0L) lastSeen = now
            val elapsed = (now - lastSeen).coerceAtLeast(0L)
            val staleTimeout = ScanTuning.staleTimeoutMs(device.deviceType)
            val nextState = when {
                elapsed > staleTimeout -> PresenceState.STALE
                elapsed > ScanTuning.missingTimeoutMs(device.deviceType) -> PresenceState.MISSING
                else -> PresenceState.ACTIVE
            }
            if (nextState == PresenceState.STALE &&
                elapsed > staleTimeout + ScanTuning.STALE_RETENTION_MS
            ) {
                iterator.remove()
                changed = true
                continue
            }
            if (device.presenceState != nextState) {
                val updated = device.withPresenceState(nextState)
                entry.setValue(updated)
                if (sessionDevices.containsKey(entry.key)) {
                    sessionDevices[entry.key] = updated
                }
                changed = true
            }
        }
        if (changed) {
            snapshot()
            dirty = true
        }
    }

    private fun matchesScanType(device: SignalDevice): Boolean {
        val type = scanType ?: return true
        val dt = device.deviceType
        if (type.isBluetooth && (dt == DeviceType.BLUETOOTH_LE || dt == DeviceType.BLUETOOTH_CLASSIC)) return true
        if (type.isWifi && dt == DeviceType.WIFI) return true
        if (type.isCellular && dt == DeviceType.CELLULAR) return true
        return false
    }

    private fun snapshot() {
        latest = ArrayList(devices.values)
        latestSession = ArrayList(sessionDevices.values)
    }

    private fun notifyListener() {
        listener?.onDevicesUpdated(latest)
    }

    /** 生产实现：单个守护线程，兼做投递与定时。 */
    private class ExecutorScheduler : Scheduler {
        private val worker: ScheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "scan-engine").apply { isDaemon = true }
            }

        override fun execute(task: Runnable) {
            worker.execute(task)
        }

        override fun schedulePeriodic(intervalMs: Long, task: Runnable): Cancellation {
            val future = worker.scheduleWithFixedDelay(task, intervalMs, intervalMs, TimeUnit.MILLISECONDS)
            return Cancellation { future.cancel(false) }
        }

        override fun shutdown() {
            worker.shutdownNow()
        }
    }
}
