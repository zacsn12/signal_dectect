package org.zacsn.signal_dectect.data.scan

import org.zacsn.signal_dectect.data.scanner.BluetoothScanController
import org.zacsn.signal_dectect.data.scanner.CellularScanController
import org.zacsn.signal_dectect.data.scanner.WiFiScanController
import org.zacsn.signal_dectect.domain.model.ScanType
import org.zacsn.signal_dectect.domain.model.SignalDevice

/**
 * 把现有三个 Java 扫描控制器适配成统一的 [ScanSource]，不改动控制器内部逻辑。
 * 各控制器特有的回调（onDeviceFound / onSignalUpdate）在此桥接到统一的 [ScanObserver]。
 */

class BluetoothScanSource(private val controller: BluetoothScanController) : ScanSource {
    override fun appliesTo(scanType: ScanType): Boolean = scanType.isBluetooth
    override fun label(): String = "蓝牙巡检"
    override fun start() {
        controller.startScan()
    }
    override fun stop() {
        controller.stopScan()
    }
    override fun setObserver(observer: ScanObserver?) {
        if (observer == null) {
            controller.setScanListener(null)
            return
        }
        controller.setScanListener(object : BluetoothScanController.ScanListener {
            override fun onDeviceFound(device: SignalDevice) {
                observer.onDevice(device)
            }
            override fun onScanError(error: String) {
                observer.onError(error)
            }
        })
    }
}

class WiFiScanSource(private val controller: WiFiScanController) : ScanSource {
    override fun appliesTo(scanType: ScanType): Boolean = scanType.isWifi
    override fun label(): String = "WiFi巡检"
    override fun start() {
        controller.startScan()
    }
    override fun stop() {
        controller.stopScan()
    }
    override fun setObserver(observer: ScanObserver?) {
        if (observer == null) {
            controller.setScanListener(null)
            return
        }
        controller.setScanListener(object : WiFiScanController.ScanListener {
            override fun onDeviceFound(device: SignalDevice) {
                observer.onDevice(device)
            }
            override fun onScanError(error: String) {
                observer.onError(error)
            }
        })
    }
}

class CellularScanSource(private val controller: CellularScanController) : ScanSource {
    override fun appliesTo(scanType: ScanType): Boolean = scanType.isCellular
    override fun label(): String = "蜂窝巡检"
    override fun start() {
        controller.startScan()
    }
    override fun stop() {
        controller.stopScan()
    }
    override fun setObserver(observer: ScanObserver?) {
        if (observer == null) {
            controller.setScanListener(null)
            return
        }
        controller.setScanListener(object : CellularScanController.ScanListener {
            override fun onSignalUpdate(device: SignalDevice) {
                observer.onDevice(device)
            }
            override fun onScanError(error: String) {
                observer.onError(error)
            }
        })
    }
}
