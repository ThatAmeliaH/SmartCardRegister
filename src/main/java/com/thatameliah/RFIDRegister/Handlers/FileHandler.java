package com.thatameliah.RFIDRegister.Handlers;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileHandler {
    private static final Path SAVES_DIRECTORY = Paths.get("saves");

    // Gets the Path to the specified file
    private Path GetPathToFile(String filename, String extension) {
        return SAVES_DIRECTORY.resolve(filename + extension);
    }

    // Ensure the "saves" directory exists
    private void EnsureDirectory() throws IOException {
        if (!Files.exists(SAVES_DIRECTORY)) {
            Files.createDirectories(SAVES_DIRECTORY);
        }
    }

    // Write a string to a file
    public void WriteToFile(String content, String filename, String extension) {
        Path path = GetPathToFile(filename, extension);
        try {
            EnsureDirectory();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
                writer.write(content);
            }
            System.out.println("Successfully wrote to " + path);
        } catch (IOException e) {
            System.err.println("Error writing to file " + path + ": " + e.getMessage());
        }
    }

    // Read file content as string
    public String ReadFile(String filename, String extension) {
        Path path = GetPathToFile(filename, extension);
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
    public void DeleteFile(String filename, String extension) {
        Path path = GetPathToFile(filename, extension);
        try {
            if (Files.deleteIfExists(path)) {
                System.out.println("Deleted file " + path);
            }
        } catch (IOException e) {
            System.err.println("Failed to delete file " + path + ": " + e.getMessage());
        }
    }
}