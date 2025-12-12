package com.thatameliah.SmartCardRegister.Utils

import scala.jdk.CollectionConverters._

object SettingsHandler {
  // TODO: Link to a ".settings" file.
  private var SettingsMap = Map(
    "Theme" -> "Light"
  ).asJava

  private val DefaultSettings = Map(
    "Theme" -> "Light"
  ).asJava

  /**
   * Update a setting to a new value
   * @param setting The name of the setting to update
   * @param state The new state of the setting
   * @throws IllegalArgumentException If the provided setting does not exist
   */
  def Update(setting: String, state: String): Unit = {
    if (setting == null || state == null) return

    val oldState: String = Get(setting)
    if (oldState.isEmpty) throw new IllegalArgumentException
    if (setting == oldState) return

    SettingsMap.put(setting, state)
  }

  /**
   * Get the value of a setting
   * @param setting The name of the setting to get
   * @return The value of the setting
   */
  def Get(setting: String): String = {
    val state: String = SettingsMap.get(setting)
    if (state != null) state
    else new String
  }

  /**
   * Resets a specific setting to the default state
   * @param setting The setting to reset
   * @throws IllegalArgumentException If the provided setting name is invalid
   */
  def Reset(setting: String): String = {
    val newState: String = DefaultSettings.get(setting)
    if (newState == null) throw new IllegalArgumentException

    SettingsMap.put(setting, newState)
    return newState
  }

  /** Reset all settings to their default state */
  def ResetAll(): Unit = SettingsMap = DefaultSettings
}
