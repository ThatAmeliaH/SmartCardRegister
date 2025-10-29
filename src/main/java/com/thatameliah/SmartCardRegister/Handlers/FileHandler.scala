package com.thatameliah.SmartCardRegister.Handlers

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}

object FileHandler {
  private val SAVES_DIRECTORY: Path = Paths.get("saves")

  private def GetPathToFile(filename: String) = SAVES_DIRECTORY.resolve(filename)

  @throws[IOException]
  private def EnsureDirectory(): Unit = {
    if (!Files.exists(SAVES_DIRECTORY)) Files.createDirectories(SAVES_DIRECTORY)
  }

  def WriteToFile(content: String, file: File): Unit = {
    val path: Path = file.toPath
    try {
      Files.createDirectories(path.getParent)
      Files.writeString(path, content)
      print("Successfully wrote to " + path)
    } catch {
      case err: IOException => print("Error writing to file " + path + ": " + err.getMessage)
    }
  }

  def ReadFile(file: File): String = {
    val path = file.toPath
    if (!Files.exists(path)) {
      System.err.println("File not found: " + path)
      return null
    }
    try Files.readString(path)
    catch {
      case e: IOException =>
        System.err.println("Error reading file " + path + ": " + e.getMessage)
        null
    }
  }

  // Delete file if exists
  def DeleteFile(filename: String): Unit = {
    val path = GetPathToFile(filename)
    try if (Files.deleteIfExists(path)) System.out.println("Deleted file " + path)
    catch {
      case e: IOException =>
        System.err.println("Failed to delete file " + path + ": " + e.getMessage)
    }
  }
}
