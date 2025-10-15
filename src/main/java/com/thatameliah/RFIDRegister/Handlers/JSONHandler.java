package com.thatameliah.RFIDRegister.Handlers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class JSONHandler {
    // Create a new JSON Object representing one person
    // TODO: Rewrite to accept a "User" Kotlin dataclass
    public JSONObject NewUser(String name, String id) {
        Map<String, String> hash = new HashMap<>();
        hash.put("name", name);
        hash.put("id", id);
        return new JSONObject(hash);
    }

    // Convert an array of JSONObjects to one JSONArray
    public JSONArray ToJSONArray(JSONObject[] objects) {
        JSONArray array = new JSONArray();
        for (JSONObject obj : objects) {
            array.put(obj);
        }
        return array;
    }

    // Convert a JSONArray into a formatted JSON String
    public String ToJSONString(JSONArray array, int indentFactor) {
        return array.toString(indentFactor);
    }

    // Parse a JSON String into a JSONArray
    public JSONArray ParseJSONArray(String jsonString) {
        return new JSONArray(jsonString);
    }
}