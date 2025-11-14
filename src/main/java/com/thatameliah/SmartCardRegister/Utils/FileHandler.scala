package com.thatameliah.SmartCardRegister.Utils

import java.io.{File, IOException}
import java.nio.file.{Files, Path}

object FileHandler {
  /**
   * Writes a string to a file
   *
   * @param content The String to be written
   * @param file The file to write to
   */
  def WriteToFile(file: File, content: String): Unit = {
    val path = file.toPath

    try {
      Files.createDirectories(path.getParent)
      Files.writeString(path, content)
    }
    catch { case err: IOException => System.err.println("Error writing to file " + path + ": " + err.getCause) }
  }

  /**
   * Reads the contents of a file
   *
   * @param file The file to read from
   * @return The string contents of the file
   */
  def ReadFile(file: File): String = {
    val path: Path = file.toPath

    if (!Files.exists(path)) { return new String }
    try { Files.readString(path) }
    catch { case _: IOException => new String }
  }
}