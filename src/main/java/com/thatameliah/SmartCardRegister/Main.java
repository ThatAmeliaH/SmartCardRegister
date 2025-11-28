/*
    Smart Card Registration system.
    Initial prototype by Sidney "ThatAmeliaH" Hills.
    Candidate number 3829.
*/

package com.thatameliah.SmartCardRegister;

// Internal Java classes
import com.thatameliah.SmartCardRegister.Forms.*;

// External libraries
import javax.swing.*;
import java.time.Duration;
import java.time.Instant;

public class Main {
    public static void main(String[] args) {
        LoadingScreen loadingScreen = new LoadingScreen();

        Instant start = Instant.now();
        Register registerUI = new Register();
        Instant end = Instant.now();
       
        Duration duration = Duration.between(start, end);
        System.out.println("Register loaded in " + duration.toMillis() + "ms (" + duration.toNanos() + " ns)");

        // Runs the specified Runnable (or in this case lambda) asynchronously after all other java.awt events have finished
        // Used here to allow the Register.java class and linked form to fully initialise before displaying it to the user
        SwingUtilities.invokeLater(() -> {
            loadingScreen.dispose();
            registerUI.setVisible(true);
        });
    }
}