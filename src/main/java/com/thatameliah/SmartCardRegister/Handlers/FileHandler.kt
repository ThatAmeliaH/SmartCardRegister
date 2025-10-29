package com.thatameliah.SmartCardRegister.Handlers

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileHandler {
    val savesDirectory: Path = Paths.get("saves")

    // Gets the Path to the specified file
    @JvmStatic
    private fun getPathToFIle(filename: String): Path {
        return savesDirectory.resolve(filename)
    }

    /**
     * Ensures the saves directory exists
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun ensureDirectory() {
        if (!Files.exists(savesDirectory)) {
            Files.createDirectories(savesDirectory)
        }
    }

    /**
     * Writes a string to a file
     *
     * @param content The String to be written
     * @param file The file to write to
     */
    @JvmStatic
    fun writeToFile(content: String, file: File) {
        val path = file.toPath()
        try {
            Files.createDirectories(path.getParent())
            Files.writeString(path, content)
            println("Successfully wrote to $path")
        } catch (e: IOException) {
            System.err.println("Error writing to file " + path + ": " + e.message)
        }
    }

    /**
     * Reads the contents of a file
     *
     * @param file The file to read from
     * @return The string contents of the file, or null if it is empty
     */
    @JvmStatic
    fun readFile(file: File): String? {
        val path = file.toPath()
        if (!Files.exists(path)) {
            System.err.println("File not found: $path")
            return null
        }
        try {
            return Files.readString(path)
        } catch (e: IOException) {
            System.err.println("Error reading file " + path + ": " + e.message)
            return null
        }
    }
}