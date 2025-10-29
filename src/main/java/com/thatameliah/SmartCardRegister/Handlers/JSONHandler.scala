package com.thatameliah.SmartCardRegister.Handlers

import org.json.{JSONArray, JSONObject}

import java.util

object JSONHandler {
  /** Create a new JSON Object representing one student
   *
   * @param name The student's name
   * @param id   The ID of the student
   * @return JSONObject with String "name" and String "id" fields
   */
  def CreateStudentJSON(name: String, id: String): JSONObject = {
    val hashMap = new util.HashMap[String, String]
    hashMap.put("name", name)
    hashMap.put("id", id)
    new JSONObject(hashMap)
  }

  /**
   * Converts an array of JSONObjects into a JSONArray.
   *
   * @param objects An array of JSONObjects
   * @return JSONArray containing the provided objects
   */
  def ToJSONArray(objects: Array[JSONObject]): JSONArray = {
    val array = new JSONArray
    for (obj <- objects) {
      array.put(obj)
    }
    array
  }

  /**
   * Converts a JSONArray into a formatted JSON String.
   *
   * @param array        The JSONArray to convert
   * @param indentFactor The number of spaces to indent for pretty-printing
   * @return Formatted JSON string
   */
  def ToJSONString(array: JSONArray, indentFactor: Int): String = if (array == null) "[]"
  else array.toString(indentFactor)

  /**
   * Parses a JSON String into a JSONArray.
   *
   * @param jsonString The JSON string to parse
   * @return JSONArray parsed from the string
   */
  def parseJSONArray(jsonString: String): JSONArray = if (jsonString == null || jsonString.isEmpty) new JSONArray
  else new JSONArray(jsonString)
}
