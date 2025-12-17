package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class SettingsMenu extends JFrame {
  private JPanel ContentPane;

  public enum Setting {
    THEME,
  }

  public SettingsMenu(Register parentRegister) {
    try { Thread.sleep(1); }
    catch (InterruptedException ignored) {}

    final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    final double HEIGHT = SCREEN_SIZE.getHeight();
    final int V_HEIGHT = (int) HEIGHT / 4;
    final double WIDTH = SCREEN_SIZE.getWidth();
    final int V_WIDTH = (int) WIDTH / 4;

    this.setSize(V_WIDTH, V_HEIGHT);
    this.setLocationRelativeTo(parentRegister);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setContentPane(ContentPane);
    this.setTitle("Settings");
    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        parentRegister.TerminalTesterOpen = false;
        dispose();
      }
    });
  }
}