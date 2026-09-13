package org.zacsn.signal_dectect.data.scan

/**
 * 厂商识别的纯逻辑集中处（无 Android 依赖，可在 JVM 单测中直接调用）。
 *
 * 汇总 Bluetooth SIG Company ID / USB-IF VID → 厂商名、设备名品牌推断、品牌归一化，
 * 以及对“历史重载/含糊”Company ID 的保守处理（按未知返回，避免弱线索误判厂商）。
 *
 * 原先这部分逻辑散落在 BluetoothManufacturerUtils（含 android.util.Log，无法单测）里，
 * 现迁移到此；BluetoothManufacturerUtils 保留对外静态 API 并委托到本类。
 */
object ManufacturerResolver {

    private val companyIds: Map<Int, String> = mapOf(
        0x004C to "Apple, Inc.",
        0x0075 to "Samsung Electronics Co. Ltd.",
        0x0157 to "Xiaomi Inc.",
        0x0272 to "Xiaomi Inc.",
        0x038F to "Xiaomi Inc.",
        0x0589 to "Huawei Technologies Co., Ltd.",
        0x027D to "Huawei Technologies Co., Ltd.",
        0x0471 to "OPPO Mobile Telecommunications Corp., Ltd.",
        0x04E1 to "vivo Mobile Communication Co., Ltd.",
        0x0459 to "OnePlus Electronics (Shenzhen) Co., Ltd.",
        0x0006 to "Microsoft",
        0x000F to "Intel Corp.",
        0x000A to "Qualcomm",
        0x00E0 to "Google",
        0x0087 to "Garmin International, Inc.",
        0x0131 to "Lenovo (Singapore) Pte Ltd.",
        0x0259 to "HP Inc.",
        0x0419 to "ASUS Global Pte Ltd",
        0x009E to "Sony Corporation",
        0x0109 to "Bose Corporation",
        0x00A7 to "Harman International Industries, Inc.",
        0x0141 to "Sennheiser electronic GmbH & Co. KG",
        0x0357 to "JBL",
        0x0090 to "Fitbit, Inc.",
        0x0275 to "Huami Information Technology Co., Ltd.",
        0x0099 to "Texas Instruments Inc.",
        0x0059 to "Nordic Semiconductor ASA",
        0x02E5 to "Espressif Inc.",
        0x0349 to "Shenzhen Jingxun Software Telecommunication Technology Co., Ltd",
        0x0143 to "Tesla Motors",
        0x0189 to "Volkswagen AG",
        0x0164 to "BMW AG",
        0x0118 to "Mercedes-Benz",
        0x0183 to "Toyota Motor Corporation",
        0x0009 to "Nintendo Co., Ltd.",
        0x0054 to "Sony Interactive Entertainment Inc.",
        0x0159 to "Cisco Systems, Inc",
        0x0286 to "TP-Link Corporation Limited",
        0x0489 to "Xiaomi Communications Co Ltd",
        0x0057 to "LG Electronics",
        0x0117 to "Motorola Solutions",
        0x0168 to "HTC Corporation",
        0x0224 to "Amazon.com Services LLC",
        0x0050 to "Plantronics, Inc.",
        0x0077 to "Logitech International SA",
        0x0195 to "GoPro, Inc.",
        0x0247 to "DJI",
        0x0343 to "Anker Innovations Limited",
        0x05AC to "Realme Chongqing Mobile"
    )

    /** 历史重载 / 含糊的 Company ID：按“未知”处理，避免把弱 BLE 线索变成错误厂商。 */
    private val ambiguousCompanyIds: Set<Int> = setOf(0x0171, 0x0499)

    private val usbVendors: Map<Int, String> = mapOf(
        0x05AC to "Apple, Inc.",
        0x04E8 to "Samsung Electronics Co. Ltd.",
        0x2717 to "Xiaomi Inc.",
        0x12D1 to "Huawei Technologies Co., Ltd.",
        0x18D1 to "Google",
        0x045E to "Microsoft",
        0x8086 to "Intel Corp.",
        0x8087 to "Intel Corp.",
        0x05C6 to "Qualcomm",
        0x054C to "Sony Corporation",
        0x17EF to "Lenovo (Singapore) Pte Ltd.",
        0x03F0 to "HP Inc.",
        0x0B05 to "ASUS Global Pte Ltd",
        0x22D9 to "OPPO Mobile Telecommunications Corp., Ltd.",
        0x2D95 to "vivo Mobile Communication Co., Ltd.",
        0x2A70 to "OnePlus Electronics (Shenzhen) Co., Ltd."
    )

    @JvmStatic
    fun companyId(id: Int): String? {
        if (id in ambiguousCompanyIds) return null
        return companyIds[id]
    }

    @JvmStatic
    fun pnp(vendorIdSource: Int, vendorId: Int): String? = when (vendorIdSource) {
        1 -> companyId(vendorId)
        2 -> usbVendors[vendorId]
        else -> null
    }

    @JvmStatic
    fun pnpSourceLabel(vendorIdSource: Int): String = when (vendorIdSource) {
        1 -> "Bluetooth SIG"
        2 -> "USB-IF"
        else -> "未知VID来源"
    }

    @JvmStatic
    fun companyIdHex(id: Int): String = "0x%04X".format(id and 0xFFFF)

    /**
     * 仅当广播数据足够可靠时才返回厂商。iBeacon 常借用 Apple 的 Company ID，
     * 即便硬件由第三方制造 —— 这种情况按未知返回，避免误报。
     */
    @JvmStatic
    fun reliableCompanyId(id: Int, manufacturerData: ByteArray?): String? {
        if (id == 0x004C && isAppleIBeacon(manufacturerData)) return null
        return companyId(id)
    }

    @JvmStatic
    fun fromName(deviceName: String?): String? {
        if (deviceName == null) return null
        val n = deviceName.lowercase()
        return when {
            n.contains("airpods") || n.contains("beats") || n.contains("apple") -> "Apple, Inc."
            n.contains("galaxy") || n.contains("samsung") -> "Samsung Electronics Co. Ltd."
            n.contains("xiaomi") || n.contains("redmi") || n.contains("mi band") -> "Xiaomi Inc."
            n.contains("huawei") || n.contains("honor") -> "Huawei Technologies Co., Ltd."
            n.contains("oppo") -> "OPPO Mobile Telecommunications Corp., Ltd."
            n.contains("vivo") -> "vivo Mobile Communication Co., Ltd."
            n.contains("oneplus") -> "OnePlus Electronics (Shenzhen) Co., Ltd."
            n.contains("pixel") || n.contains("google") -> "Google"
            else -> null
        }
    }

    @JvmStatic
    fun sameManufacturer(first: String?, second: String?): Boolean {
        val a = normalizeBrand(first)
        val b = normalizeBrand(second)
        return a != null && a == b
    }

    @JvmStatic
    fun normalizeBrand(manufacturer: String?): String? {
        if (manufacturer == null) return null
        val n = manufacturer.lowercase()
            .replace(",", " ")
            .replace(".", " ")
            .replace("-", " ")
            .replace("(", " ")
            .replace(")", " ")
            .replace("&", " ")
            .trim()
        if (n.isEmpty()) return null
        return when {
            n.contains("apple") || n.contains("beats") -> "apple"
            n.contains("samsung") || n.contains("galaxy") -> "samsung"
            n.contains("xiaomi") || n.contains("redmi") || n.contains("huami") || n.contains("amazfit") -> "xiaomi"
            n.contains("huawei") || n.contains("honor") -> "huawei"
            n.contains("oppo") -> "oppo"
            n.contains("vivo") -> "vivo"
            n.contains("oneplus") -> "oneplus"
            n.contains("google") || n.contains("pixel") -> "google"
            n.contains("microsoft") -> "microsoft"
            n.contains("intel") -> "intel"
            n.contains("qualcomm") -> "qualcomm"
            n.contains("sony") -> "sony"
            n.contains("lenovo") -> "lenovo"
            n.contains("hp ") -> "hp"
            n.contains("asus") -> "asus"
            else -> n
        }
    }

    @JvmStatic
    fun companyIdCount(): Int = companyIds.size

    private fun isAppleIBeacon(data: ByteArray?): Boolean =
        data != null && data.size >= 23 &&
            (data[0].toInt() and 0xFF) == 0x02 &&
            (data[1].toInt() and 0xFF) == 0x15
}
