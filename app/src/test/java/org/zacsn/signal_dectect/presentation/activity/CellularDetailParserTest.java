package org.zacsn.signal_dectect.presentation.activity;

import org.junit.Test;
import static org.junit.Assert.*;

public class CellularDetailParserTest {

    @Test
    public void testParseFull5GNrServingCell() {
        String evidence = "运营商=中国移动, PLMN=46000, 小区=区域=41235, 小区ID=12345678, PCI/PSC=235, 频点=627264, 制式=5G NR, 当前服务小区=是, 原始信号=-78 dBm";
        String deviceName = "当前服务小区 5G NR";
        String mac = "CELLULAR_CELL_46000_5G_NR_41235_12345678_235";
        String candidate = "中国移动";
        int dbm = -78;

        DeviceDetailActivity.CellularInfo info = DeviceDetailActivity.CellularInfo.parse(
                evidence, deviceName, mac, candidate, dbm
        );

        assertEquals("中国移动", info.operator);
        assertEquals("46000", info.plmn);
        assertEquals("5G NR", info.radioType);
        assertTrue(info.isServing);
        assertEquals("12345678", info.cellId);
        assertEquals("235", info.pci);
        assertEquals("41235", info.tac);
        assertEquals("627264", info.channel);
        assertEquals(-78, info.dbm);
    }

    @Test
    public void testParse4GLteNeighborCell() {
        String evidence = "运营商=中国电信, PLMN=46003, 小区=区域=28901, 小区ID=87654321, PCI/PSC=128, 频点=1825, 制式=4G LTE, 当前服务小区=否, 原始信号=-92 dBm";
        String deviceName = "邻区 4G LTE";
        String mac = "CELLULAR_CELL_46003_4G_LTE_28901_87654321_128";
        String candidate = "中国电信";
        int dbm = -92;

        DeviceDetailActivity.CellularInfo info = DeviceDetailActivity.CellularInfo.parse(
                evidence, deviceName, mac, candidate, dbm
        );

        assertEquals("中国电信", info.operator);
        assertEquals("46003", info.plmn);
        assertEquals("4G LTE", info.radioType);
        assertFalse(info.isServing);
        assertEquals("87654321", info.cellId);
        assertEquals("128", info.pci);
        assertEquals("28901", info.tac);
        assertEquals("1825", info.channel);
        assertEquals(-92, info.dbm);
    }

    @Test
    public void testParseFallbackOperatorCell() {
        String evidence = "运营商=中国联通, PLMN=46001, 网络=4G LTE, 原始信号=-88 dBm";
        String deviceName = "蜂窝网络 4G LTE";
        String mac = "CELLULAR_OPERATOR_46001_4G_LTE";
        String candidate = "中国联通";
        int dbm = -88;

        DeviceDetailActivity.CellularInfo info = DeviceDetailActivity.CellularInfo.parse(
                evidence, deviceName, mac, candidate, dbm
        );

        assertEquals("中国联通", info.operator);
        assertEquals("46001", info.plmn);
        assertEquals("4G LTE", info.radioType);
        assertEquals("未知", info.cellId);
        assertEquals("未知", info.pci);
        assertEquals("未知", info.tac);
        assertEquals(-88, info.dbm);
    }

    @Test
    public void testParseFallbackFromMacAndDeviceNameWhenEvidenceEmpty() {
        String evidence = null;
        String deviceName = "当前服务小区 5G NR";
        String mac = "CELLULAR_CELL_46000_5G_NR_41235_12345678_235";
        String candidate = "中国移动";
        int dbm = -75;

        DeviceDetailActivity.CellularInfo info = DeviceDetailActivity.CellularInfo.parse(
                evidence, deviceName, mac, candidate, dbm
        );

        assertEquals("中国移动", info.operator);
        assertEquals("46000", info.plmn);
        assertEquals("5G NR", info.radioType);
        assertTrue(info.isServing);
        assertEquals("12345678", info.cellId);
        assertEquals("235", info.pci);
        assertEquals("41235", info.tac);
    }
}
