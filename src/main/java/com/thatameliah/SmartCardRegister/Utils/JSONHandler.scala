package com.thatameliah.SmartCardRegister.Utils

import scala.jdk.CollectionConverters.MapHasAsJava
import org.json.{JSONArray, JSONObject}

object JSONHandler {
  /** Create a new JSON Object representing one student
   * @param name The student's name
   * @param studentId The ID of the student
   * @return JSONObject with String "name" and String "id" fields
   */
  def CreateStudentJSON(name: String, studentId: String, UID: String): JSONObject = {
    if (name == null || studentId == null) { throw new NullPointerException }

    val studentMap: java.util.Map[String, String] = Map(
      "name" -> name,
      "id" -> studentId,
      "UID" -> UID
    ).asJava
    new JSONObject(studentMap)
  }

  /**
   * Converts an array of JSONObjects into a JSONArray.
   * @param objects An array of JSONObjects
   * @return JSONArray containing the provided objects
   */
  def ToJSONArray(objects: Array[JSONObject]): JSONArray = {
    val array: JSONArray = new JSONArray
    
    objects.foreach(obj => array.put(obj))
    array
  }

  /**
   * Converts a JSONArray into a formatted JSON String.
   * @param array The JSONArray to convert
   * @param indentFactor The number of spaces to indent for pretty-printing
   * @return The formatted JSON string
   */
  def ToJSONString(array: JSONArray, indentFactor: Int): String = {
    if (array == null) "[]"
    else array.toString(indentFactor)
  }

  /**
   * Parses a JSON String into a JSONArray.
   * @param jsonString The JSON string to parse
   * @return JSONArray parsed from the string
   */
  def ParseJSONArray(jsonString: String): JSONArray = {
    if (jsonString == null || jsonString.isEmpty) new JSONArray
    else new JSONArray(jsonString)
  }
}