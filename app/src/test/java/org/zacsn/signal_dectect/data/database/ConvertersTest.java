package org.zacsn.signal_dectect.data.database;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
}
