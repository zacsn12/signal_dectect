package org.zacsn.signal_dectect.data.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** 纯 JVM 单测：验证厂商识别的核心逻辑。 */
public class ManufacturerResolverTest {

    @Test
    public void resolvesKnownCompanyId() {
        assertEquals("Apple, Inc.", ManufacturerResolver.companyId(0x004C));
        assertEquals("Samsung Electronics Co. Ltd.", ManufacturerResolver.companyId(0x0075));
    }

    @Test
    public void unknownCompanyIdIsNull() {
        assertNull(ManufacturerResolver.companyId(0xFFFF));
    }

    @Test
    public void ambiguousCompanyIdIsNull() {
        assertNull(ManufacturerResolver.companyId(0x0171));
        assertNull(ManufacturerResolver.companyId(0x0499));
    }

    @Test
    public void normalizeBrandCollapsesVariants() {
        assertEquals("huawei", ManufacturerResolver.normalizeBrand("Huawei Technologies Co., Ltd."));
        assertEquals("huawei", ManufacturerResolver.normalizeBrand("HONOR"));
        assertEquals("xiaomi", ManufacturerResolver.normalizeBrand("Amazfit"));
    }

    @Test
    public void sameManufacturerAcrossNames() {
        assertTrue(ManufacturerResolver.sameManufacturer("Apple, Inc.", "beats by dre"));
        assertFalse(ManufacturerResolver.sameManufacturer("Apple, Inc.", "Samsung"));
    }

    @Test
    public void nameInference() {
        assertEquals("Apple, Inc.", ManufacturerResolver.fromName("My AirPods Pro"));
        assertNull(ManufacturerResolver.fromName("random gadget"));
    }

    @Test
    public void pnpUsbVendorAndLabels() {
        assertEquals("Apple, Inc.", ManufacturerResolver.pnp(2, 0x05AC));
        assertEquals("Bluetooth SIG", ManufacturerResolver.pnpSourceLabel(1));
        assertEquals("USB-IF", ManufacturerResolver.pnpSourceLabel(2));
    }

    @Test
    public void iBeaconApplePayloadIgnored() {
        byte[] ibeacon = new byte[23];
        ibeacon[0] = 0x02;
        ibeacon[1] = 0x15;
        assertNull(ManufacturerResolver.reliableCompanyId(0x004C, ibeacon));
        assertEquals("Apple, Inc.", ManufacturerResolver.reliableCompanyId(0x004C, new byte[]{0, 0}));
    }

    @Test
    public void companyIdHexFormat() {
        assertEquals("0x004C", ManufacturerResolver.companyIdHex(0x004C));
    }
}
