package com.oppo.AIHealth.utils;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.oppo.AIHealth.data.DiagnosisStructured;
import java.lang.reflect.Type;
import java.util.Date;

public class Converters {
    private static Gson gson = new Gson();

    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    // DiagnosisStructured 类型转换
    @TypeConverter
    public static String fromDiagnosisStructured(DiagnosisStructured structured) {
        return structured == null ? null : gson.toJson(structured);
    }

    @TypeConverter
    public static DiagnosisStructured toDiagnosisStructured(String json) {
        if (json == null) return null;
        Type type = new TypeToken<DiagnosisStructured>() {}.getType();
        return gson.fromJson(json, type);
    }
}