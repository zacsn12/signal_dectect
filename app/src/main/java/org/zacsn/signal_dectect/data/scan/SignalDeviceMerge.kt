package org.zacsn.signal_dectect.data.scan

import org.zacsn.signal_dectect.domain.model.SignalDevice
import kotlin.math.roundToInt

/**
 * 设备观测的合并规则（同一 MAC 的新旧两条记录如何折叠成一条）。
 *
 * 这些规则原先只存在于 data/scanner 包私有的 SignalDeviceStabilizer 里，
 * 只有各采集控制器能用；[ScanEngine] 作为聚合权威也需要同一套语义，
 * 因此提到 data/scan 下共用。控制器侧的调用点保持不变
 * （SignalDeviceStabilizer 保留为静态委托）。
 *
 * 本类不触碰 Android API，可直接在 JVM 单元测试中使用。
 */
object SignalDeviceMerge {

    /** RSSI 指数滑动平均：previous 为空时直接采用最新值。 */
    @JvmStatic
    fun smoothSignalStrength(previous: SignalDevice?, latestSignalStrength: Int): Int {
        if (previous == null) return latestSignalStrength
        val w = ScanTuning.RSSI_NEW_SAMPLE_WEIGHT
        return (previous.signalStrength * (1.0 - w) + latestSignalStrength * w).roundToInt()
    }

    /**
     * 合并同一设备的历史记录与最新观测。
     *
     * - 身份信息（厂商 / 来源 / 置信度 / 判定 / 证据）整体取自置信度更高的一方，
     *   避免 GATT 已确认的厂商被后续一条只有 OUI 猜测的广播覆盖回“未确认”。
     * - 名称与候选厂商逐字段择优，占位文案（unknown / 未知 / 随机地址 …）不覆盖有效值。
     * - firstSeen 与用户标记位（关注 / 黑名单 / 白名单）始终以历史记录为准。
     * - 在线状态与单调时间戳沿用最新观测；调用方通常紧接着再做一次 refreshedAt。
     */
    @JvmStatic
    fun merge(previous: SignalDevice?, latest: SignalDevice): SignalDevice {
        if (previous == null) return latest

        val identitySource =
            if (latest.manufacturerConfidence >= previous.manufacturerConfidence) latest else previous

        return SignalDevice(
            latest.macAddress,
            betterText(latest.deviceName, previous.deviceName),
            latest.deviceType,
            identitySource.manufacturer,
            betterText(latest.candidateManufacturer, previous.candidateManufacturer),
            identitySource.manufacturerSource,
            identitySource.manufacturerConfidence,
            identitySource.manufacturerVerdict,
            identitySource.manufacturerEvidence,
            latest.signalStrength,
            latest.frequency,
            latest.distance,
            previous.firstSeen,
            latest.lastSeen,
            previous.isFocused,
            previous.isBlacklisted,
            previous.isWhitelisted,
            latest.presenceState,
            latest.lastSeenElapsedRealtime
        )
    }

    private fun betterText(latest: String?, previous: String?): String? =
        if (isUseful(latest)) latest else previous

    private fun isUseful(value: String?): Boolean {
        val normalized = value?.trim()?.lowercase() ?: return false
        if (normalized.isEmpty()) return false
        return normalized !in PLACEHOLDERS
    }

    private val PLACEHOLDERS = setOf(
        "unknown",
        "unknown device",
        "unknown network",
        "hidden network",
        "未知",
        "未知厂商",
        "未确认",
        "随机地址"
    )
}
