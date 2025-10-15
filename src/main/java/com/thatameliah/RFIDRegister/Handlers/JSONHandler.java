package com.thatameliah.RFIDRegister.Handlers;

import org.json.JSONArray;
import org.json.JSONObject;
import com.thatameliah.RFIDRegister.DataClasses.*;

import java.util.*;

public class JSONHandler {
   // Create a new JSON Object representing one person
    public JSONObject CreatePersonJSON(Person person) {
        String name = person.getForename() + person.getSurname();
        String Id = person.getId();
        
        Map<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("name", name);
        hashMap.put("id", Id);
        return new JSONObject(hashMap);
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