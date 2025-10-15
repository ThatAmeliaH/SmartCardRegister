/*
    RFID Registration system
    Initial prototype by Sidney "Amelia" Hills, candidate number 3829
*/

package com.thatameliah.SmartCardRegister;

// Internal Java classes
import com.thatameliah.SmartCardRegister.Forms.MainRegisterUI;
import com.thatameliah.SmartCardRegister.Handlers.*;

// Internal Kotlin classes
import com.thatameliah.SmartCardRegister.DataClasses.*;

// External libraries
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Initialise UI
        MainRegisterUI registerUI = new MainRegisterUI();

        // Enable register UI - runs on the event dispatch thread after all pending AWT events have finished
        SwingUtilities.invokeLater(() -> {
            System.out.println("Loading MainRegister on " + Thread.currentThread());
            registerUI.setVisible(true);
        });
    }
}