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
  /** Standard entry point. Runs the full register program with all UIs. */
  public static void main(String[] args) {
    LoadingScreen loadingScreen = new LoadingScreen();

    Instant start = Instant.now();
    Register registerUI = new Register();
    Instant end = Instant.now();

    Duration duration = Duration.between(start, end);
    System.out.println("Register loaded in " + duration.toMillis() + "ms (" + duration.toNanos() + " ns)");

    // Invoke later to allow all UI components to create before showing to the user
    SwingUtilities.invokeLater(() -> {
      loadingScreen.dispose();
      registerUI.setVisible(true);
    });
  }
}