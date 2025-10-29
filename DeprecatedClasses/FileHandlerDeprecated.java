package com.thatameliah.SmartCardRegister.Handlers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Deprecated
public class FileHandlerDeprecated {
    private static final Path SAVES_DIRECTORY = Paths.get("saves");

    // Private constructor, prevents accidental initialisation
    private FileHandlerDeprecated() {
        throw new UnsupportedOperationException("Handler classes are static and cannot be initialised.");
    }

    // Gets the Path to the specified file
    private static Path GetPathToFile(String filename) {
        return SAVES_DIRECTORY.resolve(filename);
    }

    // Ensure the "saves" directory exists
    private static void EnsureDirectory() throws IOException {
        if (!Files.exists(SAVES_DIRECTORY)) {
            Files.createDirectories(SAVES_DIRECTORY);
        }
    }

    // Write a string to a file
    public static void WriteToFile(String content, File file) {
        Path path = file.toPath();
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
            System.out.println("Successfully wrote to " + path);
        } catch (IOException e) {
            System.err.println("Error writing to file " + path + ": " + e.getMessage());
        }
    }

    // Read the contents of a file
    public static String ReadFile(File file) {
        Path path = file.toPath();
        if (!Files.exists(path)) {
            System.err.println("File not found: " + path);
            return null;
        }
        try {
            return Files.readString(path);
        } catch (IOException e) {
            System.err.println("Error reading file " + path + ": " + e.getMessage());
            return null;
        }
    }

    // Delete file if exists
    public static void DeleteFile(String filename) {
        Path path = GetPathToFile(filename);
        try {
            if (Files.deleteIfExists(path)) {
                System.out.println("Deleted file " + path);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete file " + path + ": " + e.getMessage());
        }
    }
}