package org.zacsn.signal_dectect.domain.alert;

import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.Locale;
import java.util.Set;

public final class AlertRuleMatcher {
    private AlertRuleMatcher() {
    }

    public static AlertMatch match(SignalDevice device, AlertConfig config) {
        if (device == null || config == null) {
            return null;
        }

        String normalizedMac = normalizeMac(device.getMacAddress());
        if (normalizedMac.isEmpty() || config.getWhitelistMacs().contains(normalizedMac)) {
            return null;
        }

        if (config.getBlacklistMacs().contains(normalizedMac)) {
            return new AlertMatch("黑名单告警", "该设备 MAC 地址命中黑名单", normalizedMac);
        }

        String watchlistMatch = findWatchlistMatch(device, config);
        if (watchlistMatch == null) {
            return null;
        }

        return new AlertMatch(
                "巡检机型告警",
                "设备信息命中巡检机型: " + watchlistMatch,
                normalizedMac
        );
    }

    public static void addKeyword(Set<String> keywords, String value) {
        if (keywords == null) {
            return;
        }
        String normalized = safeLower(value).trim();
        if (isStrongKeyword(normalized)) {
            keywords.add(normalized);
        }
    }

    public static void addBrand(Set<String> brands, String value) {
        if (brands == null) {
            return;
        }
        String brand = toBrandKey(value);
        if (!brand.isEmpty()) {
            brands.add(brand);
        }
    }

    public static void addMac(Set<String> macs, String value) {
        if (macs == null) {
            return;
        }
        String normalized = normalizeMac(value);
        if (!normalized.isEmpty()) {
            macs.add(normalized);
        }
    }

    public static String normalizeMac(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replace("-", ":").toUpperCase(Locale.US);
    }

    public static String toBrandKey(String value) {
        String normalized = normalizeTextToken(value);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.contains("apple") || normalized.contains("iphone") || normalized.contains("ipad")) {
            return "apple";
        }
        if (normalized.contains("huawei") || normalized.contains("honor")) {
            return "huawei";
        }
        if (normalized.contains("xiaomi") || normalized.contains("redmi")) {
            return "xiaomi";
        }
        if (normalized.contains("samsung") || normalized.contains("galaxy")) {
            return "samsung";
        }
        if (normalized.contains("oppo")) {
            return "oppo";
        }
        if (normalized.contains("vivo") || normalized.contains("iqoo")) {
            return "vivo";
        }
        if (normalized.contains("oneplus")) {
            return "oneplus";
        }
        if (normalized.contains("google") || normalized.contains("pixel")) {
            return "google";
        }
        if (normalized.contains("realme")) {
            return "realme";
        }
        if (normalized.contains("microsoft")) {
            return "microsoft";
        }
        return normalized.length() >= 4 ? normalized : "";
    }

    private static String findWatchlistMatch(SignalDevice device, AlertConfig config) {
        String normalizedMac = normalizeMac(device.getMacAddress());
        if (!normalizedMac.isEmpty() && config.getWatchlistMacs().contains(normalizedMac)) {
            return normalizedMac + " (MAC精确命中)";
        }

        if (!isManufacturerAlertable(device)) {
            return null;
        }

        String manufacturer = getAlertableManufacturer(device);
        String brandKey = toBrandKey(manufacturer);
        if (!brandKey.isEmpty() && config.getWatchlistBrands().contains(brandKey)) {
            return getCanonicalBrandLabel(brandKey) + " (" + getManufacturerAlertLevelLabel(device) + ")";
        }

        String normalizedManufacturer = normalizeTextToken(manufacturer);
        if (normalizedManufacturer.length() < 4) {
            return null;
        }

        for (String keyword : config.getWatchlistKeywords()) {
            if (isStrongKeyword(keyword) && normalizedManufacturer.contains(normalizeTextToken(keyword))) {
                return keyword + " (" + getManufacturerAlertLevelLabel(device) + ")";
            }
        }
        return null;
    }

    private static boolean isManufacturerAlertable(SignalDevice device) {
        ManufacturerVerdict verdict = device.getManufacturerVerdict();
        if (verdict == ManufacturerVerdict.CONFIRMED) {
            return true;
        }
        if (verdict != ManufacturerVerdict.LIKELY) {
            return false;
        }

        String source = device.getManufacturerSource();
        return "classic_mac_oui".equals(source)
                || "classic_mac_name_match".equals(source)
                || "gatt_device_info_conflict".equals(source)
                || "wifi_bssid_oui".equals(source)
                || "ble_apple_findmy".equals(source)
                || "ble_apple_nearby".equals(source);
    }

    private static String getManufacturerAlertLevelLabel(SignalDevice device) {
        return device.getManufacturerVerdict() == ManufacturerVerdict.CONFIRMED
                ? "厂商已确认"
                : "厂商高可信";
    }

    private static String getAlertableManufacturer(SignalDevice device) {
        if (device.getManufacturerVerdict() == ManufacturerVerdict.CONFIRMED) {
            return safeText(device.getManufacturer());
        }
        return safeText(device.getCandidateManufacturer());
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.US);
    }

    private static String safeText(String value) {
        return value == null || value.trim().isEmpty() ? "未知" : value;
    }

    private static boolean isStrongKeyword(String keyword) {
        String normalized = normalizeTextToken(keyword);
        return normalized.length() >= 4;
    }

    private static String normalizeTextToken(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.US).replaceAll("[^a-z0-9\\u4e00-\\u9fa5]+", "");
    }

    private static String getCanonicalBrandLabel(String brandKey) {
        switch (brandKey) {
            case "apple":
                return "Apple";
            case "huawei":
                return "Huawei/Honor";
            case "xiaomi":
                return "Xiaomi/Redmi";
            case "samsung":
                return "Samsung";
            case "oppo":
                return "OPPO";
            case "vivo":
                return "vivo/iQOO";
            case "oneplus":
                return "OnePlus";
            case "google":
                return "Google/Pixel";
            case "realme":
                return "realme";
            case "microsoft":
                return "Microsoft";
            default:
                return brandKey;
        }
    }
}
