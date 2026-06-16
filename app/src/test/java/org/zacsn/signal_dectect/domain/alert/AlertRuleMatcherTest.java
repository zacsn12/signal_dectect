package org.zacsn.signal_dectect.domain.alert;

import org.junit.Test;
import org.zacsn.signal_dectect.domain.model.DeviceType;
import org.zacsn.signal_dectect.domain.model.ManufacturerVerdict;
import org.zacsn.signal_dectect.domain.model.SignalDevice;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class AlertRuleMatcherTest {
    @Test
    public void whitelistSuppressesBlacklistMatch() {
        AlertConfig config = config(
                set(),
                set(),
                set(),
                set("AA:BB:CC:00:00:01"),
                set("AA:BB:CC:00:00:01")
        );

        AlertMatch match = AlertRuleMatcher.match(device("AA:BB:CC:00:00:01"), config);

        assertNull(match);
    }

    @Test
    public void blacklistMacCreatesBlacklistAlert() {
        AlertConfig config = config(
                set(),
                set(),
                set(),
                set(),
                set("AA:BB:CC:00:00:01")
        );

        AlertMatch match = AlertRuleMatcher.match(device("aa-bb-cc-00-00-01"), config);

        assertNotNull(match);
        assertEquals("黑名单告警", match.getAlertType());
        assertEquals("AA:BB:CC:00:00:01", match.getNormalizedMac());
    }

    @Test
    public void trustedManufacturerSourceMatchesWatchlistBrand() {
        Set<String> brands = new HashSet<>();
        AlertRuleMatcher.addBrand(brands, "Apple");
        AlertConfig config = config(set(), set(), brands, set(), set());

        AlertMatch match = AlertRuleMatcher.match(
                device(
                        "11:22:33:44:55:66",
                        "未确认",
                        "Apple, Inc.",
                        "ble_apple_nearby",
                        82,
                        ManufacturerVerdict.LIKELY
                ),
                config
        );

        assertNotNull(match);
        assertEquals("巡检机型告警", match.getAlertType());
    }

    @Test
    public void weakManufacturerSourceDoesNotMatchWatchlistBrand() {
        Set<String> brands = new HashSet<>();
        AlertRuleMatcher.addBrand(brands, "Apple");
        AlertConfig config = config(set(), set(), brands, set(), set());

        AlertMatch match = AlertRuleMatcher.match(
                device(
                        "11:22:33:44:55:66",
                        "未确认",
                        "Apple, Inc.",
                        "ble_company_id",
                        60,
                        ManufacturerVerdict.POSSIBLE
                ),
                config
        );

        assertNull(match);
    }

    private SignalDevice device(String macAddress) {
        return device(macAddress, "未知", "未知", "unknown", 0, ManufacturerVerdict.UNKNOWN);
    }

    private SignalDevice device(
            String macAddress,
            String manufacturer,
            String candidateManufacturer,
            String manufacturerSource,
            int manufacturerConfidence,
            ManufacturerVerdict verdict
    ) {
        long now = System.currentTimeMillis();
        return new SignalDevice(
                macAddress,
                "Test Device",
                DeviceType.BLUETOOTH_LE,
                manufacturer,
                candidateManufacturer,
                manufacturerSource,
                manufacturerConfidence,
                verdict,
                "",
                -55,
                null,
                1.0,
                now,
                now,
                false,
                false,
                false
        );
    }

    private AlertConfig config(
            Set<String> watchlistKeywords,
            Set<String> watchlistMacs,
            Set<String> watchlistBrands,
            Set<String> whitelistMacs,
            Set<String> blacklistMacs
    ) {
        return new AlertConfig(
                watchlistKeywords,
                watchlistMacs,
                watchlistBrands,
                whitelistMacs,
                blacklistMacs
        );
    }

    private Set<String> set(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}
