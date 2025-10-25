package com.thatameliah.SmartCardRegister.Handlers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

public class JSONHandler {
    // Private constructor, prevents accidental initialisation
    private JSONHandler() {
        throw new UnsupportedOperationException("Handler classes are static and cannot be initialised.");
    }

    /** Create a new JSON Object representing one person
     *
     * @param name The person's name
     * @param id The ID of the person
     * @return JSONObject with String "name" and String "id" fields
     */
    public static JSONObject CreatePersonJSON(String name, String id) {
        Map<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("name", name);
        hashMap.put("id", id);
        return new JSONObject(hashMap);
    }

    /**
     * Converts an array of JSONObjects into a JSONArray.
     *
     * @param objects An array of JSONObjects
     * @return JSONArray containing the provided objects
     */
    public static JSONArray ToJSONArray(JSONObject[] objects) {
        JSONArray array = new JSONArray();
        for (JSONObject obj : objects) {
            array.put(obj);
        }
        return array;
    }

    /**
     * Converts a JSONArray into a formatted JSON String.
     *
     * @param array The JSONArray to convert
     * @param indentFactor The number of spaces to indent for pretty-printing
     * @return Formatted JSON string
     */
    public static String ToJSONString(JSONArray array, int indentFactor) {
        return (array == null) 
                ? "[]"
                : array.toString(indentFactor);
    }

    /**
     * Parses a JSON String into a JSONArray.
     *
     * @param jsonString The JSON string to parse
     * @return JSONArray parsed from the string
     */
    public static JSONArray parseJSONArray(String jsonString) {
        return (jsonString == null || jsonString.isEmpty())
                ? new JSONArray()
                : new JSONArray(jsonString);
    }
}