package com.thatameliah.SmartCardRegister.Utils

import com.thatameliah.SmartCardRegister.Forms.SettingsMenu.Setting

import java.util
import scala.jdk.CollectionConverters._

object SettingsHandler {
  // TODO: Link to a ".settings" file.
  private var SettingsMap = new util.HashMap[Setting, String]() {{
    put(Setting.THEME, "Light")
  }}

  private val DefaultSettings = new util.HashMap[Setting, String]() {{
    put(Setting.THEME, "Light")
  }}

  /**
   * Update a setting to a new value
   * @param setting The setting to update
   * @param state The new state of the setting
   * @throws IllegalArgumentException If the provided setting does not exist
   */
  def Update(setting: Setting, state: String): Unit = {
    require(setting != null && state != null, "Setting or state cannot be null.")

    val oldState: String = Get(setting)
    if (oldState.isEmpty) throw new IllegalArgumentException
    if (state == oldState) return

    SettingsMap.put(setting, state)
  }

  /**
   * Get the value of a setting
   * @param setting The setting to get
   * @return The value of the setting
   */
  def Get(setting: Setting): String = {
    Option(SettingsMap.get(setting))
      .getOrElse(throw new IllegalArgumentException("No such setting found"))
  }

  /**
   * Resets a specific setting to the default state
   * @param setting The setting to reset
   * @throws IllegalArgumentException If the provided setting name is invalid
   */
  def Reset(setting: Setting): String = {
    val newState: String = DefaultSettings.get(setting)
    if (newState == null) throw new IllegalArgumentException

    SettingsMap.put(setting, newState)
    newState
  }

  /** Reset all settings to their default state */
  def ResetAll(): Unit = SettingsMap = new util.HashMap(DefaultSettings)
}
