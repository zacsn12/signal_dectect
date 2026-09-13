package org.zacsn.signal_dectect.data.scan

import org.zacsn.signal_dectect.domain.model.DeviceType

/**
 * 扫描相关可调参数的集中处。
 *
 * 此前这些阈值散落在 SignalScanService、各扫描控制器与 SignalDeviceStabilizer 里，
 * 无法统一调参。后续采集/聚合逻辑统一从这里取值，也为将来接入远程配置
 * （不发版即可下发新阈值）留出唯一入口。
 *
 * 在线状态判定基于单调时钟 elapsedRealtime，不受用户修改系统时间影响。
 */
object ScanTuning {

    /** 过期状态巡检周期。 */
    const val STALE_PRUNE_INTERVAL_MS = 1_000L

    /**
     * 设备列表变更向上回调的最小间隔。
     *
     * BLE 使用 MATCH_MODE_AGGRESSIVE + reportDelay=0，每秒可产生几十条观测；
     * 逐条回调会触发同频率的通知刷新（系统对单包通知约 10 次/秒即开始限流丢帧）
     * 与 LiveData 抖动，因此变更先记脏，由巡检节拍统一发出。
     */
    const val EMIT_INTERVAL_MS = 400L

    /** 进入 STALE 后再保留多久才从列表移除。 */
    const val STALE_RETENTION_MS = 5 * 60_000L

    /** RSSI 平滑：新样本权重（指数滑动平均）。 */
    const val RSSI_NEW_SAMPLE_WEIGHT = 0.35

    // 各设备类型的 暂失(MISSING) / 离线(STALE) 超时（毫秒）
    const val BLE_MISSING_TIMEOUT_MS = 15_000L
    const val BLE_STALE_TIMEOUT_MS = 35_000L
    const val CLASSIC_BT_MISSING_TIMEOUT_MS = 25_000L
    const val CLASSIC_BT_STALE_TIMEOUT_MS = 50_000L
    const val WIFI_MISSING_TIMEOUT_MS = 30_000L
    const val WIFI_STALE_TIMEOUT_MS = 70_000L
    const val CELLULAR_MISSING_TIMEOUT_MS = 40_000L
    const val CELLULAR_STALE_TIMEOUT_MS = 90_000L

    @JvmStatic
    fun missingTimeoutMs(type: DeviceType?): Long = when (type) {
        DeviceType.BLUETOOTH_CLASSIC -> CLASSIC_BT_MISSING_TIMEOUT_MS
        DeviceType.WIFI -> WIFI_MISSING_TIMEOUT_MS
        DeviceType.CELLULAR -> CELLULAR_MISSING_TIMEOUT_MS
        else -> BLE_MISSING_TIMEOUT_MS
    }

    @JvmStatic
    fun staleTimeoutMs(type: DeviceType?): Long = when (type) {
        DeviceType.BLUETOOTH_CLASSIC -> CLASSIC_BT_STALE_TIMEOUT_MS
        DeviceType.WIFI -> WIFI_STALE_TIMEOUT_MS
        DeviceType.CELLULAR -> CELLULAR_STALE_TIMEOUT_MS
        else -> BLE_STALE_TIMEOUT_MS
    }
}
