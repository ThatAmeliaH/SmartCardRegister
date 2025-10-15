/*
    RFID Registration system
    Initial prototype by Sidney "Amelia" Hills, candidate number 3829
    All Rights Reserved
*/

package com.thatameliah.SmartCardRegister;

// Internal form classes
import com.thatameliah.SmartCardRegister.Forms.MainRegister;

// Internal handler classes
import com.thatameliah.SmartCardRegister.Handlers.Base64Handler;
import com.thatameliah.SmartCardRegister.Handlers.FileHandler;
import com.thatameliah.SmartCardRegister.Handlers.JSONHandler;

// Kotlin data classes
import com.thatameliah.SmartCardRegister.DataClasses.*;

// External libraries
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Initialise helper classes
        JSONHandler jsonHandler = new JSONHandler();
        FileHandler fileHandler = new FileHandler();
        Base64Handler base64Handler = new Base64Handler();
        // Initialise UI
        MainRegister RegisterUI = new MainRegister();

        // Enable register UI - runs on a new thread after all other java.awt events have finished
        SwingUtilities.invokeLater(() -> {
            System.out.println("Loading MainRegister on " + Thread.currentThread());
            RegisterUI.setVisible(true);
        });
    }
}