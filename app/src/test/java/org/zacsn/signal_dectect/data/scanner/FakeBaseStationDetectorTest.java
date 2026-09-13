package org.zacsn.signal_dectect.data.scanner;

import org.junit.Before;
import org.junit.Test;
import org.zacsn.signal_dectect.domain.model.FakeBaseStationCell;
import org.zacsn.signal_dectect.domain.model.FakeCellRiskLevel;

import static org.junit.Assert.*;

public class FakeBaseStationDetectorTest {

    private FakeBaseStationDetector detector;

    @Before
    public void setUp() {
        // Constructor with null context for direct evaluateCell testing
        detector = new FakeBaseStationDetector(null);
    }

    @Test
    public void testNormalSafe4GLteCell() {
        FakeBaseStationDetector.RawCellRawData raw = new FakeBaseStationDetector.RawCellRawData();
        raw.operator = "中国移动";
        raw.plmn = "46000";
        raw.radioType = "4G LTE";
        raw.cellId = "12345678";
        raw.lacTac = "41235";
        raw.pciPsc = "235";
        raw.channel = "1825";
        raw.signalDbm = -85;
        raw.isServing = true;

        long now = System.currentTimeMillis();
        FakeBaseStationCell result = detector.evaluateCell(raw, 5, now);

        assertEquals(FakeCellRiskLevel.SAFE, result.getRiskLevel());
        assertEquals(0, result.getRiskScore());
        assertTrue(result.getAnomalyReasons().isEmpty());
        assertEquals("中国移动", result.getOperator());
        assertEquals("46000", result.getPlmn());
    }

    @Test
    public void testAbnormalLac0TriggersThreat() {
        FakeBaseStationDetector.RawCellRawData raw = new FakeBaseStationDetector.RawCellRawData();
        raw.operator = "中国移动";
        raw.plmn = "46000";
        raw.radioType = "2G GSM";
        raw.cellId = "1001";
        raw.lacTac = "0"; // Suspicious fake LAC
        raw.pciPsc = "12";
        raw.channel = "95";
        raw.signalDbm = -68; // Strong isolated
        raw.isServing = true;

        long now = System.currentTimeMillis();
        FakeBaseStationCell result = detector.evaluateCell(raw, 0, now);

        assertEquals(FakeCellRiskLevel.DANGEROUS, result.getRiskLevel());
        assertTrue(result.getRiskScore() >= 55);
        assertTrue(result.getAnomalyReasons().stream().anyMatch(r -> r.contains("位置区码异常")));
        assertTrue(result.getAnomalyReasons().stream().anyMatch(r -> r.contains("孤立极强2G信号")));
    }

    @Test
    public void testForced2GDowngradeTrigger() {
        FakeBaseStationDetector.RawCellRawData raw = new FakeBaseStationDetector.RawCellRawData();
        raw.operator = "中国联通";
        raw.plmn = "46001";
        raw.radioType = "2G GSM";
        raw.cellId = "5543";
        raw.lacTac = "65535"; // Reserved LAC
        raw.pciPsc = "50";
        raw.channel = "80";
        raw.signalDbm = -70;
        raw.isServing = true;

        long now = System.currentTimeMillis();
        FakeBaseStationCell result = detector.evaluateCell(raw, 0, now);

        assertEquals(FakeCellRiskLevel.DANGEROUS, result.getRiskLevel());
        assertTrue(result.isThreat());
    }

    @Test
    public void testInvalidPlmn() {
        FakeBaseStationDetector.RawCellRawData raw = new FakeBaseStationDetector.RawCellRawData();
        raw.operator = "未知";
        raw.plmn = "00000"; // Invalid PLMN
        raw.radioType = "4G LTE";
        raw.cellId = "12345";
        raw.lacTac = "20001";
        raw.pciPsc = "10";
        raw.channel = "100";
        raw.signalDbm = -90;
        raw.isServing = false;

        long now = System.currentTimeMillis();
        FakeBaseStationCell result = detector.evaluateCell(raw, 2, now);

        assertEquals(FakeCellRiskLevel.SUSPICIOUS, result.getRiskLevel());
        assertTrue(result.getAnomalyReasons().stream().anyMatch(r -> r.contains("运营商代码异常")));
    }

    @Test
    public void testInvalidCellId() {
        FakeBaseStationDetector.RawCellRawData raw = new FakeBaseStationDetector.RawCellRawData();
        raw.operator = "中国电信";
        raw.plmn = "46003";
        raw.radioType = "4G LTE";
        raw.cellId = "0"; // Invalid cell id
        raw.lacTac = "28901";
        raw.pciPsc = "120";
        raw.channel = "1825";
        raw.signalDbm = -88;
        raw.isServing = true;

        long now = System.currentTimeMillis();
        FakeBaseStationCell result = detector.evaluateCell(raw, 3, now);

        assertEquals(FakeCellRiskLevel.SUSPICIOUS, result.getRiskLevel());
        assertTrue(result.getAnomalyReasons().stream().anyMatch(r -> r.contains("小区身份标识缺失或非法")));
    }
}
