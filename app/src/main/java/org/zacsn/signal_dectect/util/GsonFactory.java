package org.zacsn.signal_dectect.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Factory for creating a safely configured Gson instance.
 * Avoids Gson 2.8.5 EnumTypeAdapter reflection bugs (AssertionError: NoSuchFieldException)
 * on Android ART runtime when serializing/deserializing Enums.
 */
public class GsonFactory {

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new SafeEnumTypeAdapterFactory())
            .create();

    public static Gson getGson() {
        return GSON;
    }

    private static class SafeEnumTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            Class<? super T> rawType = type.getRawType();
            if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
                return null;
            }
            if (!rawType.isEnum()) {
                rawType = rawType.getSuperclass(); // Handles anonymous enum subclasses if any
            }
            return (TypeAdapter<T>) new SafeEnumTypeAdapter((Class<Enum>) rawType);
        }
    }

    private static class SafeEnumTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
        private final Map<String, T> nameToConstant = new HashMap<>();
        private final Map<T, String> constantToName = new HashMap<>();

        public SafeEnumTypeAdapter(Class<T> classOfT) {
            for (T constant : classOfT.getEnumConstants()) {
                String name = constant.name();
                try {
                    Field field = classOfT.getField(name);
                    SerializedName annotation = field.getAnnotation(SerializedName.class);
                    if (annotation != null) {
                        name = annotation.value();
                    }
                } catch (Throwable ignored) {
                    // Safely ignore Reflection NoSuchFieldException on ART / R8 obfuscated enums
                }
                nameToConstant.put(name, constant);
                constantToName.put(constant, name);
                // Also map constant.name() as fallback for deserializing legacy JSON
                if (!nameToConstant.containsKey(constant.name())) {
                    nameToConstant.put(constant.name(), constant);
                }
            }
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                String name = constantToName.get(value);
                out.value(name != null ? name : value.name());
            }
        }

        @Override
        public T read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String str = in.nextString();
            T constant = nameToConstant.get(str);
            if (constant == null && str != null) {
                // Case-insensitive fallback
                for (Map.Entry<String, T> entry : nameToConstant.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(str)) {
                        return entry.getValue();
                    }
                }
            }
            return constant;
        }
    }
}
