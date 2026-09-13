package org.zacsn.signal_dectect.data.database;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.google.gson.Gson;

import static org.junit.Assert.*;

/**
 * Unit tests for TypeConverters
 */
public class ConvertersTest {
    
    @Test
    public void testListToStringConversion() {
        // Arrange
        List<String> testList = Arrays.asList("device1", "device2", "device3");
        
        // Act
        String json = Converters.fromList(testList);
        List<String> result = Converters.fromString(json);
        
        // Assert
        assertNotNull(json);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("device1", result.get(0));
        assertEquals("device2", result.get(1));
        assertEquals("device3", result.get(2));
    }
    
    @Test
    public void testNullListConversion() {
        // Act
        String json = Converters.fromList(null);
        List<String> result = Converters.fromString(null);
        
        // Assert
        assertNull(json);
        assertNull(result);
    }
    
    @Test
    public void testEmptyListConversion() {
        // Arrange
        List<String> emptyList = Arrays.asList();
        
        // Act
        String json = Converters.fromList(emptyList);
        List<String> result = Converters.fromString(json);
        
        // Assert
        assertNotNull(json);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
    
    @Test
    public void testGenericObjectConversion() {
        // Arrange
        TestObject testObj = new TestObject("test", 123);
        
        // Act
        String json = Converters.toJson(testObj);
        
        // Assert
        assertNotNull(json);
        assertTrue(json.contains("test"));
        assertTrue(json.contains("123"));
    }
    
    @Test
    public void testNullObjectConversion() {
        // Act
        String json = Converters.toJson(null);
        
        // Assert
        assertNull(json);
    }
    
    // Helper class for testing
    private static class TestObject {
        String name;
        int value;
        
        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    public void testScanRecordEntityToDomain() {
        java.util.List<org.zacsn.signal_dectect.domain.model.SignalDevice> devices = new java.util.ArrayList<>();
        devices.add(new org.zacsn.signal_dectect.domain.model.SignalDevice(
                "AA:BB:CC:DD:EE:FF",
                "Test BLE",
                org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_LE,
                "Apple",
                "Apple Inc.",
                "gatt_device_info",
                95,
                org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.CONFIRMED,
                "Device Info Service",
                -65,
                null,
                1.5,
                1000L,
                2000L,
                false,
                false,
                false,
                org.zacsn.signal_dectect.domain.model.PresenceState.ACTIVE,
                0L
        ));

        String json = org.zacsn.signal_dectect.util.GsonFactory.getGson().toJson(devices);
        ScanRecordEntity entity = new ScanRecordEntity(
                System.currentTimeMillis(),
                org.zacsn.signal_dectect.domain.model.ScanType.ALL.toInt(),
                120L,
                39.9,
                116.4,
                1,
                json
        );
        entity.setId(1L);
        entity.setName("Test Record");

        org.zacsn.signal_dectect.domain.model.ScanRecord domain = org.zacsn.signal_dectect.domain.model.EntityMappers.toDomain(entity);
        assertNotNull(domain);
        assertEquals(1L, domain.getId());
        assertEquals(1, domain.getDeviceCount());
        assertNotNull(domain.getDevices());
        assertEquals(1, domain.getDevices().size());
        assertEquals("AA:BB:CC:DD:EE:FF", domain.getDevices().get(0).getMacAddress());
        assertEquals(org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_LE, domain.getDevices().get(0).getDeviceType());
        assertEquals(org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.CONFIRMED, domain.getDevices().get(0).getManufacturerVerdict());
        assertEquals(org.zacsn.signal_dectect.domain.model.PresenceState.ACTIVE, domain.getDevices().get(0).getPresenceState());
    }

    @Test
    public void testMalformedJsonHandlingInEntityMappers() {
        ScanRecordEntity entity = new ScanRecordEntity(
                System.currentTimeMillis(),
                1,
                60L,
                null,
                null,
                0,
                "{invalid_json_content"
        );
        entity.setId(2L);
        entity.setName("Corrupted Record");

        org.zacsn.signal_dectect.domain.model.ScanRecord domain = org.zacsn.signal_dectect.domain.model.EntityMappers.toDomain(entity);
        assertNotNull(domain);
        assertNotNull(domain.getDevices());
        assertEquals(0, domain.getDevices().size());
    }

    @Test
    public void testMalformedJsonInConverters() {
        List<String> list = Converters.fromString("not a json array");
        assertNull(list);
    }

    @Test
    public void testSignalDeviceNullSafeGetters() {
        // Construct SignalDevice with null fields (simulating deserialization anomaly)
        org.zacsn.signal_dectect.domain.model.SignalDevice device = new org.zacsn.signal_dectect.domain.model.SignalDevice(
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                -80,
                null,
                10.0,
                0L,
                0L,
                false,
                false,
                false,
                null,
                0L
        );

        assertNotNull(device.getMacAddress());
        assertEquals("", device.getMacAddress());
        assertNotNull(device.getDeviceName());
        assertEquals("", device.getDeviceName());
        assertNotNull(device.getDeviceType());
        assertEquals(org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_CLASSIC, device.getDeviceType());
        assertNotNull(device.getManufacturer());
        assertNotNull(device.getPresenceState());
        assertNotNull(device.getManufacturerVerdict());
    }

    @Test
    public void testScanRecordMultipleDevicesJsonRoundTrip() {
        List<org.zacsn.signal_dectect.domain.model.SignalDevice> originalDevices = new ArrayList<>();
        originalDevices.add(new org.zacsn.signal_dectect.domain.model.SignalDevice(
                "AA:BB:CC:11:22:33",
                "Apple iPhone",
                org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_LE,
                "Apple",
                "Apple Inc.",
                "gatt_device_info",
                95,
                org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.CONFIRMED,
                "Device Info Service",
                -55,
                null,
                1.2,
                1000L,
                2000L,
                false,
                false,
                false,
                org.zacsn.signal_dectect.domain.model.PresenceState.ACTIVE,
                0L
        ));
        originalDevices.add(new org.zacsn.signal_dectect.domain.model.SignalDevice(
                "11:22:33:44:55:66",
                "Huawei Router",
                org.zacsn.signal_dectect.domain.model.DeviceType.WIFI,
                "Huawei",
                "Huawei Technologies",
                "wifi_oui",
                80,
                org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.LIKELY,
                "BSSID OUI match",
                -68,
                2412,
                4.5,
                1500L,
                2500L,
                false,
                false,
                false,
                org.zacsn.signal_dectect.domain.model.PresenceState.MISSING,
                0L
        ));
        originalDevices.add(new org.zacsn.signal_dectect.domain.model.SignalDevice(
                "CELL:460:00:1234:5678",
                "China Mobile LTE",
                org.zacsn.signal_dectect.domain.model.DeviceType.CELLULAR,
                "China Mobile",
                "China Mobile",
                "cell_plmn",
                100,
                org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.CONFIRMED,
                "LTE Cell Tower",
                -85,
                null,
                50.0,
                500L,
                3000L,
                false,
                false,
                false,
                org.zacsn.signal_dectect.domain.model.PresenceState.ACTIVE,
                0L
        ));

        Gson gson = org.zacsn.signal_dectect.util.GsonFactory.getGson();
        String json = gson.toJson(originalDevices);
        assertNotNull(json);
        assertTrue(json.contains("AA:BB:CC:11:22:33"));
        assertTrue(json.contains("Huawei Router"));
        assertTrue(json.contains("China Mobile LTE"));

        Type listType = new com.google.gson.reflect.TypeToken<List<org.zacsn.signal_dectect.domain.model.SignalDevice>>() {}.getType();
        List<org.zacsn.signal_dectect.domain.model.SignalDevice> parsedDevices = gson.fromJson(json, listType);

        assertNotNull(parsedDevices);
        assertEquals(3, parsedDevices.size());

        // Verify Device 1 (BLE)
        org.zacsn.signal_dectect.domain.model.SignalDevice d1 = parsedDevices.get(0);
        assertEquals("AA:BB:CC:11:22:33", d1.getMacAddress());
        assertEquals("Apple iPhone", d1.getDeviceName());
        assertEquals(org.zacsn.signal_dectect.domain.model.DeviceType.BLUETOOTH_LE, d1.getDeviceType());
        assertEquals("Apple", d1.getManufacturer());
        assertEquals(org.zacsn.signal_dectect.domain.model.ManufacturerVerdict.CONFIRMED, d1.getManufacturerVerdict());
        assertEquals(org.zacsn.signal_dectect.domain.model.PresenceState.ACTIVE, d1.getPresenceState());
        assertEquals(-55, d1.getSignalStrength());

        // Verify Device 2 (WiFi)
        org.zacsn.signal_dectect.domain.model.SignalDevice d2 = parsedDevices.get(1);
        assertEquals("11:22:33:44:55:66", d2.getMacAddress());
        assertEquals("Huawei Router", d2.getDeviceName());
        assertEquals(org.zacsn.signal_dectect.domain.model.DeviceType.WIFI, d2.getDeviceType());
        assertEquals(Integer.valueOf(2412), d2.getFrequency());
        assertTrue(d2.isMissing());

        // Verify Device 3 (Cellular)
        org.zacsn.signal_dectect.domain.model.SignalDevice d3 = parsedDevices.get(2);
        assertEquals("CELL:460:00:1234:5678", d3.getMacAddress());
        assertEquals(org.zacsn.signal_dectect.domain.model.DeviceType.CELLULAR, d3.getDeviceType());
        assertEquals(-85, d3.getSignalStrength());
    }

    @Test
    public void testParseDevicesJsonFromStringArray() {
        String stringArrayJson = "[\"AA:BB:CC:DD:EE:FF\", \"11:22:33:44:55:66\"]";
        List<org.zacsn.signal_dectect.domain.model.SignalDevice> devices = 
                org.zacsn.signal_dectect.domain.model.EntityMappers.parseDevicesJson(stringArrayJson, 1000L);
        assertNotNull(devices);
        assertEquals(2, devices.size());
        assertEquals("AA:BB:CC:DD:EE:FF", devices.get(0).getMacAddress());
        assertEquals("11:22:33:44:55:66", devices.get(1).getMacAddress());
    }
}
