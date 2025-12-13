package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Utils.*;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public class SettingsMenu extends JFrame {
  private JPanel ContentPane;
  private JRadioButton LightModeButton;
  private JRadioButton DarkModeButton;
  private JLabel ThemeLabel;
  private JButton ResetDefaultsButton;

  public record Shortcut(String name, int keyCode, int modifiers, Runnable handler) {}

  public enum Setting {
    THEME,
  }

  private final Map<String, JRadioButton> THEME_STATES = new HashMap<>() {{
    put("Light", LightModeButton);
    put("Dark", DarkModeButton);
  }};

  public SettingsMenu(Register ParentFrame) {
    try { Thread.sleep(1); }
    catch (InterruptedException ignored) {}

    final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
    final double HEIGHT = SCREEN_SIZE.getHeight();
    final int V_HEIGHT = (int) HEIGHT / 4;
    final double WIDTH = SCREEN_SIZE.getWidth();
    final int V_WIDTH = (int) WIDTH / 4;

    this.setSize(V_WIDTH, V_HEIGHT);
    this.setLocationRelativeTo(null);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setContentPane(ContentPane);
    this.setTitle("Settings");
    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) {
        ParentFrame.settingsMenu = null;
        dispose();
      }
    });

    ContentPane.setFocusable(true);
    ContentPane.requestFocus();

    Shortcut[] shortcuts = {
      new Shortcut("Close", KeyEvent.VK_ESCAPE, 0, this::dispose),
    };
    for (var shortcut : shortcuts) { BindKey(shortcut); }

    SetupMenuButtons();
    SetActiveSettings();
  }

  private void createUIComponents() {
    if (ContentPane == null) {
      ContentPane = new JPanel();
    }
  }

  private void SetupMenuButtons() {
    LightModeButton.addActionListener(event -> SetSetting(Setting.THEME, "Light"));
    DarkModeButton.addActionListener(event -> SetSetting(Setting.THEME, "Dark"));

    ResetDefaultsButton.addActionListener(event -> ResetAll());
  }

  private void SetActiveSettings() {
    String theme = SettingsHandler.Get(Setting.THEME);
    THEME_STATES.get(theme).setSelected(true);
  }

  private void SetSetting(Setting setting, String state) {
    SettingsHandler.Update(setting, state);
  }

  private void ResetAll() {
    int confirm = JOptionPane.showConfirmDialog(
      this,
      "You are about to reset all settings to their default state, are you sure you would like to continue?\nThis action is irreversible!",
      "Reset",
      JOptionPane.YES_NO_OPTION
    );
    if (confirm == JOptionPane.YES_OPTION) return;

    SettingsHandler.ResetAll();
    SetActiveSettings();
  }

  private void BindKey(@NotNull Shortcut shortcut) {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcut.keyCode, shortcut.modifiers);
    if (keyStroke == null) {
      System.err.println("Invalid KeyStroke: " + shortcut.keyCode);
      return;
    }

    // Gets the input map and action map for the main content pane
    InputMap inputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap actionMap = ContentPane.getActionMap();

    // Check for existing bindings and overwrite if present
    Object oldBinding = inputMap.get(keyStroke);
    if (oldBinding != null) {
      inputMap.remove(keyStroke);
      actionMap.remove(oldBinding);
    }

    // Add the new bindings to the input and action maps
    inputMap.put(keyStroke, shortcut.name);
    actionMap.put(shortcut.name, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) {
        try { shortcut.handler.run(); }
        catch (Exception ex) { System.err.println(ex.getMessage()); }
      }
    });
  }
}
