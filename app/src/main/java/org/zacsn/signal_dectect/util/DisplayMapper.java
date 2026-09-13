package org.zacsn.signal_dectect.util;

import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.PresenceState;

/**
 * 展示层文案映射：把内部枚举 / 来源常量统一翻译成面向用户的中文，
 * 避免 BLUETOOTH_LE、gatt_device_info 之类的内部标识直接出现在界面上。
 *
 * 全 App 的“枚举 / 来源 → 用户可读文案”映射，以此类为唯一来源。
 * 新增设备类型、判定等级或识别来源时，只在这里补一处即可。
 */
public final class DisplayMapper {

    private DisplayMapper() {
    }

    /** 设备类型 → 中文标签。 */
    public static String deviceType(DeviceType type) {
        if (type == null) {
            return "未知类型";
        }
        switch (type) {
            case BLUETOOTH_CLASSIC:
                return "经典蓝牙";
            case BLUETOOTH_LE:
                return "低功耗蓝牙";
            case WIFI:
                return "WiFi";
            case CELLULAR:
                return "蜂窝网络";
            default:
                return "未知类型";
        }
    }

    /** 厂商判定等级 → 中文标签（不含百分比，需要百分比请在调用处拼接）。 */
    public static String verdictLabel(ManufacturerVerdict verdict) {
        if (verdict == null) {
            return "未知";
        }
        switch (verdict) {
            case CONFIRMED:
                return "已确认";
            case LIKELY:
                return "高可信";
            case POSSIBLE:
                return "候选线索";
            case UNKNOWN:
            default:
                return "未知";
        }
    }

    /** 设备在线状态 → 中文标签。 */
    public static String presenceState(PresenceState state) {
        if (state == null) {
            return "";
        }
        switch (state) {
            case ACTIVE:
                return "在线";
            case MISSING:
                return "暂失";
            case STALE:
                return "离线";
            default:
                return "";
        }
    }

    /** 厂商识别的线索来源常量 → 中文说明。 */
    public static String manufacturerSource(String source) {
        if (source == null) {
            return "来源未知";
        }
        switch (source) {
            case "gatt_device_info+pnp_id":
                return "GATT设备信息+PnP ID";
            case "gatt_device_info":
                return "GATT设备信息";
            case "gatt_pnp_id":
                return "GATT PnP ID";
            case "gatt_device_info_conflict":
                return "GATT线索冲突";
            case "device_name":
                return "设备名称线索";
            case "ble_company_id":
                return "BLE广播Company ID";
            case "ble_apple_audio":
                return "Apple音频设备画像";
            case "ble_apple_findmy":
                return "Apple定位设备画像";
            case "ble_apple_handoff":
                return "Apple Handoff画像";
            case "ble_apple_nearby":
                return "Apple Nearby画像";
            case "ble_apple_protocol":
                return "Apple生态协议";
            case "mac_oui":
                return "MAC OUI线索";
            case "ble_mac_oui":
                return "BLE MAC OUI线索";
            case "classic_mac_oui":
                return "经典蓝牙MAC OUI";
            case "classic_mac_name_match":
                return "经典蓝牙MAC+名称一致";
            case "wifi_bssid_oui":
                return "WiFi BSSID厂商库";
            case "wifi_ssid_name":
                return "WiFi SSID品牌线索";
            case "wifi_bssid_ssid_match":
                return "WiFi BSSID+SSID一致";
            case "wifi_randomized_bssid":
                return "WiFi随机BSSID";
            case "cellular_operator":
                return "运营商";
            default:
                return "来源未知";
        }
    }
}
