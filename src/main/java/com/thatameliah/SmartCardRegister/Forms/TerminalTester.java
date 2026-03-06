package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Exceptions.*;
import com.thatameliah.SmartCardRegister.Utils.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class TerminalTester extends JFrame {
  private final Register parentRegister;
  private JPanel ContentPane;
  private JLabel TerminalLabel;
  private JLabel UIDLabel;

  private volatile boolean running = false;
  private Thread cardListenerThread;

  public record Shortcut(String name, int keyCode, int modifiers, Runnable handler) {}

  public TerminalTester(Register parentRegister) {
    try { Thread.sleep(1); }
    catch (InterruptedException ignored) {}
    this.parentRegister = parentRegister;

    final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    final double HEIGHT = SCREEN_SIZE.getHeight();
    final int V_HEIGHT = (int) HEIGHT / 4;
    final double WIDTH = SCREEN_SIZE.getWidth();
    final int V_WIDTH = (int) WIDTH / 4;

    this.setSize(V_WIDTH, V_HEIGHT);
    this.setLocationRelativeTo(null);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setContentPane(ContentPane);
    this.setTitle("Terminal Tester Utility");
    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        parentRegister.TerminalTesterOpen = false;
        dispose();
      }
    });

    ContentPane.setFocusable(true);
    ContentPane.requestFocus();

    TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());

    Shortcut[] shortcuts = {
      new Shortcut("Close", KeyEvent.VK_ESCAPE, 0, this::dispose),
      new Shortcut("Refresh", KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK, this::Restart)
    };
    for (var shortcut : shortcuts) { BindKey(shortcut); }

    SetupStopOnClose();
    StartCardListener();
  }

  private void StartCardListener() {
    running = true;

    cardListenerThread = new Thread(this::ListenForCards);
    cardListenerThread.setDaemon(true);
    cardListenerThread.start();
  }

  public void StopCardListener() {
    running = false;

    if (cardListenerThread != null) {
      cardListenerThread.interrupt();
    }
  }

  private void ListenForCards() {
    while (running && ContentPane.isVisible()) {
      String UID = NFCHandler.TestTerminal(0, 0);
      SwingUtilities.invokeLater(() -> { UIDLabel.setText("Last UID: " + (UID.isEmpty() ? "N/A" : UID)); });
    }
  }
  
  private void BindKey(@NotNull Shortcut shortcut) throws InvalidShortcutException {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcut.keyCode, shortcut.modifiers);
    if (keyStroke == null) { throw new InvalidShortcutException("Invalid KeyStroke: Cannot parse KeyStroke for shortcut \"" + shortcut.keyCode + "\""); }

    // Gets the input map and action map for the main content pane
    InputMap inputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = ContentPane.getActionMap();

    // Check for existing bindings and throw an exception if present
    Object oldBinding = inputMap.get(keyStroke);
    if (oldBinding != null) { throw new InvalidShortcutException("Duplicate Shortcut: KeyStroke \"" + keyStroke + "\" already bound to \""+ oldBinding + "\""); }

    // Add the new bindings to the input and action maps
    inputMap.put(keyStroke, shortcut.name);
    actionMap.put(shortcut.name, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { shortcut.handler.run(); }
    });
  }

  private void SetupStopOnClose() {
    this.addComponentListener(new ComponentAdapter() {
      @Override public void componentHidden(ComponentEvent e) { StopCardListener(); }
    });

    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) { StopCardListener(); }
      @Override public void windowClosed(WindowEvent e) { StopCardListener(); }
    });
  }

  private void Restart() {
    parentRegister.OpenTerminalTester();
    this.dispose();
  }

  private void createUIComponents() {
    if (ContentPane == null) {
      ContentPane = new JPanel();
    }

    TerminalLabel = new JLabel("Terminal: " + NFCHandler.GetActiveTerminalName());
    TerminalLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
  }
}
