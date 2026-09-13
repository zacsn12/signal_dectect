package org.zacsn.signal_dectect.data.scanner;

import org.zacsn.signal_dectect.data.scan.SignalDeviceMerge;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

/**
 * 各扫描控制器共用的入口，实际规则见 {@link SignalDeviceMerge}。
 *
 * 合并语义现在同时被控制器与 ScanEngine 使用，因此实现下沉到 data/scan；
 * 这里只保留静态委托，避免改动控制器内的大量调用点。
 */
final class SignalDeviceStabilizer {

    private SignalDeviceStabilizer() {
    }

    static int smoothSignalStrength(SignalDevice previous, int latestSignalStrength) {
        return SignalDeviceMerge.smoothSignalStrength(previous, latestSignalStrength);
    }

    static SignalDevice merge(SignalDevice previous, SignalDevice latest) {
        return SignalDeviceMerge.merge(previous, latest);
    }
}
