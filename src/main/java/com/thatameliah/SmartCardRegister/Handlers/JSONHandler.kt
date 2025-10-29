package com.thatameliah.SmartCardRegister.Handlers

import org.json.JSONArray
import org.json.JSONObject

object JSONHandler {
    /** Create a new JSON Object representing one student
     *
     * @param name The student's name
     * @param id The ID of the student
     * @return JSONObject with String "name" and String "id" fields
     */
    @JvmStatic
    fun createStudentJSON(name: String?, id: String?): JSONObject {
        val studentMap: MutableMap<String?, String?> = HashMap()
        studentMap["name"] = name
        studentMap["id"] = id
        return JSONObject(studentMap)
    }

    /**
     * Converts an array of JSONObjects into a JSONArray.
     *
     * @param objects An array of JSONObjects
     * @return JSONArray containing the provided objects
     */
    @JvmStatic
    fun toJSONArray(objects: Array<JSONObject?>): JSONArray {
        val array = JSONArray()
        for (obj in objects) {
            array.put(obj)
        }
        return array
    }

    /**
     * Converts a JSONArray into a formatted JSON String.
     *
     * @param array The JSONArray to convert
     * @param indentFactor The number of spaces to indent for pretty-printing
     * @return Formatted JSON string
     */
    @JvmStatic
    fun toJSONString(array: JSONArray?, indentFactor: Int): String? {
        return if (array == null)
            "[]"
        else
            array.toString(indentFactor)
    }

    /**
     * Parses a JSON String into a JSONArray.
     *
     * @param jsonString The JSON string to parse
     * @return JSONArray parsed from the string
     */
    @JvmStatic
    fun parseJSONArray(jsonString: String?): JSONArray {
        return if (jsonString == null || jsonString.isEmpty())
            JSONArray()
        else
            JSONArray(jsonString)
    }
}