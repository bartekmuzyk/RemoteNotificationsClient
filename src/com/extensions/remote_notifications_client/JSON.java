package com.extensions.remote_notifications_client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class JSON {
    private JSON() {}

    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public static String stringify(Map<String, Object> source) {
        return gson.toJson(source);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseToMap(String source) {
        try {
            return gson.fromJson(source, Map.class);
        } catch (JsonSyntaxException exception) {
            return new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Object> parseToList(String source) {
        try {
            return gson.fromJson(source, List.class);
        } catch (JsonSyntaxException exception) {
            return new ArrayList<>();
        }
    }
}

