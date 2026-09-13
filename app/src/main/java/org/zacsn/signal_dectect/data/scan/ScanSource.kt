package org.zacsn.signal_dectect.data.scan

import org.zacsn.signal_dectect.domain.model.ScanType
import org.zacsn.signal_dectect.domain.model.SignalDevice

/**
 * 统一的扫描来源抽象。蓝牙 / WiFi / 蜂窝（及后续协议）各自实现本接口，
 * 由 Service 以统一方式启停与订阅，替代此前每个控制器各写一套
 * onDeviceFound / onSignalUpdate 回调、Service 三段 if 分别适配的做法。
 */
interface ScanSource {
    /** 该来源是否服务于给定扫描类型（决定按类型选择性启停）。 */
    fun appliesTo(scanType: ScanType): Boolean

    /** 日志 / 错误提示用的中文短名，如“蓝牙巡检”。 */
    fun label(): String

    fun start()

    fun stop()

    /** 设置统一观测回调；传 null 表示清除。 */
    fun setObserver(observer: ScanObserver?)
}

/** 统一观测回调：所有来源产出同一种事件。 */
interface ScanObserver {
    fun onDevice(device: SignalDevice)
    fun onError(message: String)
}
