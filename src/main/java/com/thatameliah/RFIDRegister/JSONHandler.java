package com.thatameliah.nfcregister;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class JSONHandler {
    public JSONObject NewUser(String name, String id) {
        Map<String, String> hash = new HashMap<>();
        hash.put("name", name);
        hash.put("id", id);
        return new JSONObject(hash);
    }

    public void WriteJsonObjectsToFile(JSONObject[] objects, String filename, String extension) {
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("saves/"+filename+extension))) {
            for (JSONObject object : objects) {
                writer.write(String.valueOf(object));
            }
        } catch (IOException err) {
            System.out.println("Error writing to file "+filename+extension+": "+err.getMessage());
        }
    }

    public void DeleteRegister(String filename, String extension) {
        Path path = Paths.get("saves/"+filename+extension);
        try { Files.delete(path); }
        catch(IOException err) {
            System.out.println("Failed to delete file "+filename+extension+": "+err.getMessage());
        }
    }
}
