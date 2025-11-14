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

public class Main {
    public static void main(String[] args) {
        LoadingScreen loadingScreen = new LoadingScreen();
        Register registerUI = new Register();

        // Runs the specified Runnable (or in this case lambda) asynchronously after all other java.awt events have finished
        // Used here to allow the Register.java class and linked form to fully initialise before displaying it to the user
        SwingUtilities.invokeLater(() -> {
            loadingScreen.setVisible(false);
            registerUI.setVisible(true);
        });
    }
}