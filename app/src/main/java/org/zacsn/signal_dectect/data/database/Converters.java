package org.zacsn.signal_dectect.data.database;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Room TypeConverters for complex data types
 * 
 * This class provides conversion methods for storing complex types in Room database:
 * - List<String> to JSON string and back (for device lists)
 * - Custom objects to JSON and back
 */
public class Converters {
    
    private static final Gson gson = new Gson();
    
    /**
     * Convert JSON string to List of Strings
     * Used for deserializing device lists from database
     * 
     * @param value JSON string representation of list
     * @return List of strings, or null if input is null
     */
    @TypeConverter
    public static List<String> fromString(String value) {
        if (value == null) {
            return null;
        }
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }
    
    /**
     * Convert List of Strings to JSON string
     * Used for serializing device lists to database
     * 
     * @param list List of strings to convert
     * @return JSON string representation, or null if input is null
     */
    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null) {
            return null;
        }
        return gson.toJson(list);
    }
    
    /**
     * Convert JSON string to any object type
     * Generic converter for complex objects stored as JSON
     * 
     * @param value JSON string representation
     * @param type The type to deserialize to
     * @param <T> Generic type parameter
     * @return Deserialized object, or null if input is null
     */
    public static <T> T fromJson(String value, Type type) {
        if (value == null) {
            return null;
        }
        return gson.fromJson(value, type);
    }
    
    /**
     * Convert any object to JSON string
     * Generic converter for complex objects to be stored as JSON
     * 
     * @param object Object to serialize
     * @return JSON string representation, or null if input is null
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        return gson.toJson(object);
    }
}
