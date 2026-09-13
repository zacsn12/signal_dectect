package org.zacsn.signal_dectect.util;

import org.zacsn.signal_dectect.data.scan.ManufacturerResolver;

/**
 * Bluetooth 厂商识别工具。纯逻辑已迁移到 {@link ManufacturerResolver}
 * （无 Android 依赖、可单测）；本类保留既有静态 API 并委托过去，调用方无需改动。
 */
public final class BluetoothManufacturerUtils {

    private BluetoothManufacturerUtils() {
    }

    public static String getManufacturer(int companyId) {
        return ManufacturerResolver.companyId(companyId);
    }

    public static String getPnpManufacturer(int vendorIdSource, int vendorId) {
        return ManufacturerResolver.pnp(vendorIdSource, vendorId);
    }

    public static String getPnpVendorSourceLabel(int vendorIdSource) {
        return ManufacturerResolver.pnpSourceLabel(vendorIdSource);
    }

    public static String getCompanyIdHex(int companyId) {
        return ManufacturerResolver.companyIdHex(companyId);
    }

    public static String getReliableManufacturer(int companyId, byte[] manufacturerData) {
        return ManufacturerResolver.reliableCompanyId(companyId, manufacturerData);
    }

    public static String inferManufacturerFromName(String deviceName) {
        return ManufacturerResolver.fromName(deviceName);
    }

    public static boolean isSameManufacturer(String first, String second) {
        return ManufacturerResolver.sameManufacturer(first, second);
    }

    public static String normalizeBrand(String manufacturer) {
        return ManufacturerResolver.normalizeBrand(manufacturer);
    }

    public static String parseManufacturerData(byte[] manufacturerData) {
        if (manufacturerData == null || manufacturerData.length < 2) {
            return null;
        }
        int companyId = (manufacturerData[1] & 0xFF) << 8 | (manufacturerData[0] & 0xFF);
        return getManufacturer(companyId);
    }

    public static int getManufacturerCount() {
        return ManufacturerResolver.companyIdCount();
    }
}
