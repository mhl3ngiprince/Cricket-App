package com.finedine.spucricketclub.data.db.converter;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Type converter for JSON objects in Room database
 */
public class JsonConverter {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String listToJson(List<String> list) {
        return gson.toJson(list);
    }

    @TypeConverter
    public static List<String> jsonToList(String json) {
        if (json == null) {
            return new ArrayList<>();
        }
        Type listType = new TypeToken<List<String>>() {
        }.getType();
        return gson.fromJson(json, listType);
    }

    @TypeConverter
    public static String mapToJson(Map<String, Object> map) {
        return map == null ? null : gson.toJson(map);
    }

    @TypeConverter
    public static Map<String, Object> jsonToMap(String json) {
        if (json == null) {
            return null;
        }
        Type mapType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(json, mapType);
    }
}