package com.thatameliah.SmartCardRegister.Utils

import org.jetbrains.annotations.{NotNull, Nullable}

import java.io.{File, IOException}
import java.nio.file.{Files, Path}

object FileHandler {
  /**
   * Writes a string to a file
   * @param content The String to be written
   * @param file The file to write to
   * @throws NullPointerException If file or content are null
   */
  def WriteToFile(@NotNull file: File, @NotNull content: String): Unit = {
    if (file == null || content == null) throw new NullPointerException
    val path = file.toPath
    
    try {
      Files.createDirectories(path.getParent)
      Files.writeString(path, content)
    }
    catch {
      case err: IOException =>
        System.err.println("Error writing to file " + path.toString + ": " + err.getMessage)
    }
  }

  /**
   * Reads the contents of a file
   * @param file The file to read from
   * @return The string contents of the file
   */
  @NotNull def ReadFile(@NotNull file: File): String = {
    val path: Path = file.toPath

    if (!Files.exists(path)) { return new String }
    try { Files.readString(path) }
    catch { case _: IOException => new String }
  }
}